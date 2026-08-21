package com.masson.cruciblecraft.content.menu;

import java.util.function.Predicate;

import com.masson.cruciblecraft.content.blockentity.HopperBlockEntity;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperMenuLayout;
import com.masson.cruciblecraft.logistics.hopper.HopperMenuLayout.Ranges;
import com.masson.cruciblecraft.logistics.hopper.HopperMenuLayout.SlotPos;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** One Hopper-family menu. Slot count is read from the block entity. */
public final class HopperMenu extends AbstractContainerMenu {
    private final int machineSlots;
    private final HopperKind kind;
    private final Predicate<Player> validity;
    private final BlockPos hopperPos;

    public HopperMenu(int id, Inventory playerInventory, HopperBlockEntity hopper) {
        this(ModMenus.HOPPER.get(), id, playerInventory, hopper);
    }

    public HopperMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            HopperBlockEntity hopper) {
        this(
                type,
                id,
                playerInventory,
                hopper.inventory(),
                hopper.variant().kind(),
                hopper.getBlockPos(),
                hopper::stillValid);
    }

    public HopperMenu(
            MenuType<?> type,
            int id,
            Container playerInventory,
            IItemHandler items,
            HopperKind kind,
            int slots,
            Predicate<Player> validity) {
        this(type, id, playerInventory, items, kind, BlockPos.ZERO, validity);
        if (items.getSlots() != slots) {
            throw new IllegalArgumentException("Hopper menu slot count drifted");
        }
    }

    private HopperMenu(
            MenuType<?> type,
            int id,
            Container playerInventory,
            IItemHandler items,
            HopperKind kind,
            BlockPos hopperPos,
            Predicate<Player> validity) {
        super(type, id);
        this.machineSlots = items.getSlots();
        this.kind = kind;
        this.hopperPos = hopperPos.immutable();
        this.validity = validity;
        for (SlotPos pos : (kind.isQueue()
                ? HopperMenuLayout.queueSlots(machineSlots)
                : HopperMenuLayout.hopperSlots(machineSlots))) {
            addSlot(new SlotItemHandler(items, pos.index(), pos.x(), pos.y()));
        }
        int playerOffset = 0;
        for (SlotPos pos : HopperMenuLayout.playerSlots(machineSlots)) {
            int playerIndex = playerOffset < 27 ? playerOffset + 9 : playerOffset - 27;
            addSlot(new Slot(playerInventory, playerIndex, pos.x(), pos.y()));
            playerOffset++;
        }
    }

    public int machineSlots() {
        return machineSlots;
    }

    public HopperKind kind() {
        return kind;
    }

    public BlockPos hopperPos() {
        return hopperPos;
    }

    public int imageHeight() {
        return HopperMenuLayout.imageHeight(machineSlots);
    }

    public int playerInventoryLabelY() {
        return HopperMenuLayout.playerInventoryY(machineSlots) - 11;
    }

    @Override
    public boolean stillValid(Player player) {
        return validity.test(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        Ranges ranges = HopperMenuLayout.ranges(machineSlots);
        boolean moved = ranges.inMachine(index)
                ? moveItemStackTo(
                        stack, ranges.playerStart(), ranges.playerEnd(), true)
                : moveItemStackTo(
                        stack, ranges.machineStart(), ranges.machineEnd(), false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    public static HopperMenu client(
            int id, Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos)
                instanceof HopperBlockEntity hopper) {
            return new HopperMenu(id, playerInventory, hopper);
        }
        return new HopperMenu(
                ModMenus.HOPPER.get(),
                id,
                playerInventory,
                new ItemStackHandler(1),
                HopperKind.HOPPER,
                1,
                player -> false);
    }
}
