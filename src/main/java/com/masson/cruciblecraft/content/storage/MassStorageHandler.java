package com.masson.cruciblecraft.content.storage;

import com.masson.cruciblecraft.api.unit.MaterialUnits;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Single-type bulk store. Capacity is a long count; vanilla stacks are the
 * transfer unit. Overflow is rejected rather than spilled unless the overflow
 * bonus is enabled. Same-material prefix units merge like GT6
 * {@code mPartialUnits}.
 */
public final class MassStorageHandler implements IItemHandler {
    public static final int OVERFLOW_BONUS = 256;

    private final int capacity;
    private final Runnable onChange;
    private ItemStack filter = ItemStack.EMPTY;
    private int stored;
    private long partialUnits;
    private boolean keepFilterWhenEmpty = true;
    private int overflowBonus;

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

    public long partialUnits() {
        return partialUnits;
    }

    public int capacity() {
        return capacity;
    }

    public int maxContent() {
        return capacity + Math.max(0, overflowBonus);
    }

    public int remaining() {
        return Math.max(0, maxContent() - stored);
    }

    public boolean keepFilterWhenEmpty() {
        return keepFilterWhenEmpty;
    }

    public void setKeepFilterWhenEmpty(boolean keep) {
        this.keepFilterWhenEmpty = keep;
        resetFilterIfEmpty();
        onChange.run();
    }

    public void setOverflowBonus(int bonus) {
        this.overflowBonus = Math.max(0, bonus);
    }

    public void resetFilterIfEmpty() {
        if (stored > 0 || partialUnits > 0) {
            return;
        }
        stored = 0;
        partialUnits = 0;
        if (!keepFilterWhenEmpty) {
            filter = ItemStack.EMPTY;
        }
    }

    public void clearAll() {
        filter = ItemStack.EMPTY;
        stored = 0;
        partialUnits = 0;
        onChange.run();
    }

    public void load(ItemStack filter, int stored) {
        load(filter, stored, 0L);
    }

    public void load(ItemStack filter, int stored, long partialUnits) {
        this.filter = filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1);
        this.stored = Math.max(0, stored);
        this.partialUnits = Math.max(0L, partialUnits);
        convertPartials();
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
        if (filter.isEmpty()) {
            int accepted = Math.min(stack.getCount(), remaining());
            if (accepted <= 0) {
                return stack;
            }
            if (!simulate) {
                filter = stack.copyWithCount(1);
                stored += accepted;
                onChange.run();
            }
            return accepted == stack.getCount()
                    ? ItemStack.EMPTY
                    : stack.copyWithCount(stack.getCount() - accepted);
        }
        if (ItemStack.isSameItemSameComponents(filter, stack)) {
            int accepted = Math.min(stack.getCount(), remaining());
            if (accepted <= 0) {
                return stack;
            }
            if (!simulate) {
                stored += accepted;
                onChange.run();
            }
            return accepted == stack.getCount()
                    ? ItemStack.EMPTY
                    : stack.copyWithCount(stack.getCount() - accepted);
        }
        if (stored >= maxContent()) {
            return stack;
        }
        long unit = MassStoragePrefixUnits.unitAmount(filter, stack, partialUnits);
        if (unit <= 0L) {
            return stack;
        }
        if (!simulate) {
            addPartials(unit * stack.getCount());
            onChange.run();
        }
        return ItemStack.EMPTY;
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

    public ItemStack extractStored(int amount, boolean simulate) {
        if (amount <= 0 || stored <= 0 || filter.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, stored);
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
        return maxContent();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && remaining() > 0 && sameType(stack);
    }

    public ItemStack insertAll(ItemStack stack, boolean simulate) {
        return insertItem(0, stack, simulate);
    }

    public boolean sameType(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (filter.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(filter, stack)
                || MassStoragePrefixUnits.sameFamily(filter, stack);
    }

    public ItemStack takePartials() {
        ItemStack dropped = MassStoragePrefixUnits.partialStack(filter, partialUnits);
        if (dropped.isEmpty()) {
            return ItemStack.EMPTY;
        }
        long consumed = (long) dropped.getCount()
                * MaterialUnits.resolve(dropped).map(entry -> entry.form().units()).orElse(0);
        if (consumed > 0L) {
            partialUnits = Math.max(0L, partialUnits - consumed);
        } else {
            partialUnits = 0L;
        }
        resetFilterIfEmpty();
        onChange.run();
        return dropped;
    }

    public void convertPartials() {
        addPartials(0L);
    }

    private void addPartials(long added) {
        if (added > 0L) {
            partialUnits += added;
        }
        if (partialUnits <= 0L || filter.isEmpty() || stored >= maxContent()) {
            return;
        }
        int unit = MaterialUnits.resolve(filter)
                .map(entry -> entry.form().units())
                .orElse(0);
        if (unit <= 0) {
            return;
        }
        long canAdd = Math.min(partialUnits / unit, remaining());
        if (canAdd <= 0L) {
            return;
        }
        stored += (int) canAdd;
        partialUnits -= canAdd * unit;
    }
}
