package com.masson.cruciblecraft.content.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Single-type bulk store. Capacity is a long count; vanilla stacks are the
 * transfer unit. Overflow is rejected rather than spilled.
 */
public final class MassStorageHandler implements IItemHandler {
    private final int capacity;
    private final Runnable onChange;
    private ItemStack filter = ItemStack.EMPTY;
    private int stored;

    public MassStorageHandler(int capacity) {
        this(capacity, () -> {});
    }

    public MassStorageHandler(int capacity, Runnable onChange) {
        this.capacity = capacity;
        this.onChange = onChange == null ? () -> {} : onChange;
    }

    public ItemStack filter() {
        return filter.copy();
    }

    public int stored() {
        return stored;
    }

    public int capacity() {
        return capacity;
    }

    public int remaining() {
        return Math.max(0, capacity - stored);
    }

    public void resetFilterIfEmpty() {
        if (stored <= 0) {
            stored = 0;
            filter = ItemStack.EMPTY;
        }
    }

    public void load(ItemStack filter, int stored) {
        this.filter = filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1);
        this.stored = Math.max(0, Math.min(capacity, stored));
        resetFilterIfEmpty();
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot != 0 || filter.isEmpty() || stored <= 0) {
            return ItemStack.EMPTY;
        }
        return filter.copyWithCount(Math.min(stored, filter.getMaxStackSize()));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty()) {
            return stack;
        }
        if (!filter.isEmpty() && !ItemStack.isSameItemSameComponents(filter, stack)) {
            return stack;
        }
        int accepted = Math.min(stack.getCount(), remaining());
        if (accepted <= 0) {
            return stack;
        }
        if (!simulate) {
            if (filter.isEmpty()) {
                filter = stack.copyWithCount(1);
            }
            stored += accepted;
            onChange.run();
        }
        return accepted == stack.getCount()
                ? ItemStack.EMPTY
                : stack.copyWithCount(stack.getCount() - accepted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0 || amount <= 0 || stored <= 0 || filter.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, Math.min(stored, filter.getMaxStackSize()));
        ItemStack result = filter.copyWithCount(taken);
        if (!simulate) {
            stored -= taken;
            resetFilterIfEmpty();
            onChange.run();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return capacity;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0
                && !stack.isEmpty()
                && (filter.isEmpty()
                        || ItemStack.isSameItemSameComponents(filter, stack));
    }

    public ItemStack insertAll(ItemStack stack, boolean simulate) {
        return insertItem(0, stack, simulate);
    }

    public boolean sameType(ItemStack stack) {
        return !stack.isEmpty()
                && (filter.isEmpty()
                        || ItemStack.isSameItemSameComponents(filter, stack));
    }
}
