package com.masson.cruciblecraft.logistics.hopper;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** External sided view of a Hopper or Queue Hopper inventory. */
public final class HopperSidedHandler implements IItemHandler {
    private final ItemStackHandler inventory;
    private final HopperKind kind;
    private final boolean facingSide;
    private final boolean allowFacingExtract;

    public HopperSidedHandler(
            ItemStackHandler inventory,
            HopperKind kind,
            boolean facingSide,
            boolean allowFacingExtract) {
        this.inventory = inventory;
        this.kind = kind;
        this.facingSide = facingSide;
        this.allowFacingExtract = allowFacingExtract;
    }

    @Override
    public int getSlots() {
        return kind.isQueue() ? Math.min(2, inventory.getSlots()) : inventory.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(actual(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!canInsert(slot)) {
            return stack;
        }
        return inventory.insertItem(actual(slot), stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!canExtract(slot)) {
            return ItemStack.EMPTY;
        }
        return inventory.extractItem(actual(slot), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(actual(slot));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return canInsert(slot) && inventory.isItemValid(actual(slot), stack);
    }

    private boolean canInsert(int slot) {
        if (kind.isQueue()) {
            return slot == 0;
        }
        return !facingSide;
    }

    private boolean canExtract(int slot) {
        if (kind.isQueue()) {
            return slot == getSlots() - 1;
        }
        return allowFacingExtract || !facingSide;
    }

    private int actual(int slot) {
        if (!kind.isQueue()) {
            return slot;
        }
        if (slot == 0) {
            return 0;
        }
        return inventory.getSlots() - 1;
    }
}
