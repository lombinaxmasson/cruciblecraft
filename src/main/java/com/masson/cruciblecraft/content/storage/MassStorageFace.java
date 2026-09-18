package com.masson.cruciblecraft.content.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Shared GT6 mass-storage face click, dump, and auto-output. Used by T44 hosts
 * and remaining in-place metal mass storage.
 */
public final class MassStorageFace {
    private MassStorageFace() {}

    public static boolean onActivated(
            MassStorageHandler inventory,
            Player player,
            ItemStack held,
            BlockHitResult hit,
            Direction facing,
            BlockPos pos,
            java.util.function.Consumer<ItemStack> eject) {
        if (hit.getDirection() != facing) {
            return false;
        }
        float[] uv = MassStorageClicks.facingUv(facing, pos, hit.getLocation());
        if (!MassStorageClicks.onFace(uv)) {
            return false;
        }
        inventory.convertPartials();
        int amount = MassStorageClicks.amount(uv);
        if (!inventory.filter().isEmpty()) {
            if (amount > 0) {
                eject.accept(inventory.extractStored(amount, false));
            } else if (!held.isEmpty()) {
                ItemStack leftover = inventory.insertAll(held.copy(), false);
                held.setCount(leftover.getCount());
            } else if (amount == -1) {
                dumpPlayerInventory(inventory, player);
            }
        } else if (!held.isEmpty()) {
            ItemStack leftover = inventory.insertAll(held.copy(), false);
            held.setCount(leftover.getCount());
        }
        inventory.convertPartials();
        return true;
    }

    public static void giveToPlayer(MassStorageHandler inventory, Player player) {
        ItemStack partial = inventory.takePartials();
        if (!partial.isEmpty()) {
            int leftover = moveIntoPlayerMain(player, partial);
            if (leftover > 0) {
                inventory.insertAll(partial.copyWithCount(leftover), false);
            }
        }
        while (inventory.stored() > 0) {
            ItemStack taken = inventory.extractStored(64, false);
            if (taken.isEmpty()) {
                break;
            }
            int leftover = moveIntoPlayerMain(player, taken);
            if (leftover > 0) {
                inventory.insertAll(taken.copyWithCount(leftover), false);
                break;
            }
        }
    }

    public static void dumpInFront(
            MassStorageHandler inventory,
            java.util.function.Consumer<ItemStack> eject) {
        while (inventory.stored() > 0) {
            int chunk = Math.min(
                    inventory.stored(),
                    Math.max(1, inventory.filter().getMaxStackSize()));
            ItemStack taken = inventory.extractStored(chunk, false);
            if (taken.isEmpty()) {
                break;
            }
            eject.accept(taken);
        }
        ItemStack partial = inventory.takePartials();
        if (!partial.isEmpty()) {
            eject.accept(partial);
        }
        inventory.clearAll();
    }

    public static void dropContents(
            MassStorageHandler inventory, Level level, BlockPos pos) {
        while (inventory.stored() > 0) {
            ItemStack extracted = inventory.extractItem(0, 64, false);
            if (extracted.isEmpty()) {
                break;
            }
            Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(), extracted);
        }
        ItemStack partial = inventory.takePartials();
        if (!partial.isEmpty()) {
            Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(), partial);
        }
    }

    public static void ejectInFront(
            Level level, BlockPos pos, Direction facing, ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return;
        }
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.75;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.75;
        int remaining = stack.getCount();
        int max = Math.max(1, stack.getMaxStackSize());
        while (remaining > 0) {
            int chunk = Math.min(max, remaining);
            Containers.dropItemStack(level, x, y, z, stack.copyWithCount(chunk));
            remaining -= chunk;
        }
    }

    public static void pushBelow(
            MassStorageHandler inventory, Level level, BlockPos pos) {
        IItemHandler below = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        if (below == null) {
            return;
        }
        ItemStack taken = inventory.extractItem(0, 64, false);
        if (!taken.isEmpty()) {
            insertInto(inventory, below, taken);
        }
    }

    public static void emitOverflowBelow(
            MassStorageHandler inventory, Level level, BlockPos pos) {
        IItemHandler below = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        if (below == null) {
            return;
        }
        while (inventory.stored() > inventory.capacity()) {
            int extra = Math.min(64, inventory.stored() - inventory.capacity());
            ItemStack taken = inventory.extractStored(extra, false);
            if (taken.isEmpty()) {
                break;
            }
            int leftover = insertInto(inventory, below, taken);
            if (leftover >= extra) {
                break;
            }
        }
    }

    private static void dumpPlayerInventory(
            MassStorageHandler inventory, Player player) {
        var items = player.getInventory().items;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || !inventory.sameType(stack)) {
                continue;
            }
            ItemStack leftover = inventory.insertAll(stack.copy(), false);
            stack.setCount(leftover.getCount());
            if (!leftover.isEmpty()) {
                break;
            }
        }
        player.getInventory().setChanged();
    }

    private static int moveIntoPlayerMain(Player player, ItemStack stack) {
        int remaining = stack.getCount();
        var items = player.getInventory().items;
        for (int slot = 9; slot < 36 && remaining > 0; slot++) {
            ItemStack dest = items.get(slot);
            if (dest.isEmpty()) {
                int put = Math.min(remaining, stack.getMaxStackSize());
                items.set(slot, stack.copyWithCount(put));
                remaining -= put;
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(dest, stack)) {
                continue;
            }
            int put = Math.min(
                    remaining, dest.getMaxStackSize() - dest.getCount());
            if (put <= 0) {
                continue;
            }
            dest.grow(put);
            remaining -= put;
        }
        player.getInventory().setChanged();
        return remaining;
    }

    private static int insertInto(
            MassStorageHandler inventory, IItemHandler dest, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ItemStack leftover = stack.copy();
        for (int slot = 0; slot < dest.getSlots() && !leftover.isEmpty(); slot++) {
            leftover = dest.insertItem(slot, leftover, false);
        }
        if (!leftover.isEmpty()) {
            inventory.insertAll(leftover, false);
        }
        return leftover.getCount();
    }
}
