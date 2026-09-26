package com.masson.cruciblecraft.content.menu;

import java.util.function.Predicate;

import com.masson.cruciblecraft.content.blockentity.BookshelfBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BottleCrateBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DrawerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class StorageMenu extends AbstractContainerMenu {
    public static final int SLOT = 18;
    public static final int IMAGE_WIDTH = 176;
    private final int machineSlots;
    private final Predicate<Player> validity;
    private final BlockPos storagePos;
    private final int imageHeight;
    private final int playerInventoryLabelY;
    private MteInPlaceBlockEntity chest;

    public StorageMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            BookshelfBlockEntity shelf) {
        this(
                type,
                id,
                playerInventory,
                shelf.inventory(),
                shelf.inventory().getSlots(),
                0,
                shelf.getBlockPos(),
                shelf::stillValid);
    }

    public StorageMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            BottleCrateBlockEntity crate) {
        this(
                type,
                id,
                playerInventory,
                crate.inventory(),
                crate.inventory().getSlots(),
                0,
                crate.getBlockPos(),
                crate::stillValid);
    }

    public StorageMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            IItemHandler items,
            int visibleSlots,
            int offset,
            BlockPos pos,
            Predicate<Player> validity) {
        super(type, id);
        this.machineSlots = visibleSlots;
        this.storagePos = pos.immutable();
        this.validity = validity;
        int rows = Math.max(1, (visibleSlots + 8) / 9);
        for (int slot = 0; slot < visibleSlots; slot++) {
            int column = slot % 9;
            int row = slot / 9;
            addSlot(new SlotItemHandler(
                    items,
                    offset + slot,
                    8 + column * SLOT,
                    18 + row * SLOT));
        }
        int playerY = 18 + rows * SLOT + 14;
        this.playerInventoryLabelY = playerY - 12;
        this.imageHeight = playerY + 82;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * SLOT,
                        playerY + row * SLOT));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(
                    playerInventory, column, 8 + column * SLOT, playerY + 58));
        }
    }

    public static StorageMenu clientBookshelf(
            int id, Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos)
                instanceof BookshelfBlockEntity shelf) {
            return new StorageMenu(ModMenus.BOOKSHELF.get(), id, inventory, shelf);
        }
        throw new IllegalStateException("Missing bookshelf at " + pos);
    }

    public static StorageMenu clientCrate(
            int id, Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos)
                instanceof BottleCrateBlockEntity crate) {
            return new StorageMenu(ModMenus.BOTTLE_CRATE.get(), id, inventory, crate);
        }
        throw new IllegalStateException("Missing bottle crate at " + pos);
    }

    public static StorageMenu clientDrawer(
            int id, Inventory inventory, BlockPos pos, int compartment) {
        if (inventory.player.level().getBlockEntity(pos)
                instanceof DrawerBlockEntity drawer) {
            drawer.setOpenCompartment(compartment);
            return new StorageMenu(
                    ModMenus.DRAWER.get(),
                    id,
                    inventory,
                    drawer.inventory(),
                    DrawerBlockEntity.COMPARTMENT_SLOTS,
                    compartment * DrawerBlockEntity.COMPARTMENT_SLOTS,
                    pos,
                    drawer::stillValid);
        }
        throw new IllegalStateException("Missing drawer at " + pos);
    }

    public static StorageMenu clientInPlace(
            int id, Inventory inventory, BlockPos pos, int visible, int offset) {
        if (inventory.player.level().getBlockEntity(pos)
                instanceof MteInPlaceBlockEntity host) {
            return new StorageMenu(
                    ModMenus.MTE_STORAGE.get(),
                    id,
                    inventory,
                    host,
                    visible,
                    offset);
        }
        throw new IllegalStateException("Missing in-place storage at " + pos);
    }

    public static StorageMenu clientRemainder(
            int id, Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos)
                instanceof com.masson.cruciblecraft.energy.remainder
                        .EnergyBatBoxBlockEntity box) {
            return new StorageMenu(
                    ModMenus.REMAINDER_BAT_BOX.get(),
                    id,
                    inventory,
                    box.items(),
                    box.items().getSlots(),
                    0,
                    pos,
                    box::stillValid);
        }
        throw new IllegalStateException("Missing remainder bat box at " + pos);
    }

    public StorageMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            MteInPlaceBlockEntity host,
            int visibleSlots,
            int offset) {
        this(
                type,
                id,
                playerInventory,
                host.items(),
                visibleSlots,
                offset,
                host.getBlockPos(),
                host::stillValid);
        if (host.spec().kind() == MteInPlaceKind.CHEST) {
            this.chest = host;
            host.startOpen(playerInventory.player);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (chest != null) {
            chest.stopOpen(player);
        }
    }

    public int machineSlots() {
        return machineSlots;
    }

    public int imageHeight() {
        return imageHeight;
    }

    public int playerInventoryLabelY() {
        return playerInventoryLabelY;
    }

    public BlockPos storagePos() {
        return storagePos;
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
        ItemStack copy = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
