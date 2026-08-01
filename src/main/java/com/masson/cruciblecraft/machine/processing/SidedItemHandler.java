package com.masson.cruciblecraft.machine.processing;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Directional item view: input views only insert, output views only extract. */
public final class SidedItemHandler implements IItemHandler {
    private final IItemHandler delegate;
    private final List<Integer> slots;
    private final ProcessingMachineSpec.CapabilityAccess access;
    private final Runnable mutation;

    public SidedItemHandler(
            IItemHandler delegate,
            List<Integer> slots,
            ProcessingMachineSpec.CapabilityAccess access) {
        this(delegate, slots, access, () -> {});
    }

    public SidedItemHandler(
            IItemHandler delegate,
            List<Integer> slots,
            ProcessingMachineSpec.CapabilityAccess access,
            Runnable mutation) {
        this.delegate = delegate;
        this.slots = List.copyOf(slots);
        this.access = access;
        this.mutation = mutation;
        if (access == ProcessingMachineSpec.CapabilityAccess.NONE) {
            throw new IllegalArgumentException("Do not expose a NONE capability adapter");
        }
    }

    @Override public int getSlots() { return slots.size(); }
    @Override public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(actual(slot));
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStack remainder = access == ProcessingMachineSpec.CapabilityAccess.INPUT
                ? delegate.insertItem(actual(slot), stack, simulate)
                : stack;
        if (!simulate && remainder.getCount() < stack.getCount()) {
            mutation.run();
        }
        return remainder;
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack extracted = access == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                ? delegate.extractItem(actual(slot), amount, simulate)
                : ItemStack.EMPTY;
        if (!simulate && !extracted.isEmpty()) {
            mutation.run();
        }
        return extracted;
    }
    @Override public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(actual(slot));
    }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return access == ProcessingMachineSpec.CapabilityAccess.INPUT
                && delegate.isItemValid(actual(slot), stack);
    }

    private int actual(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            throw new IndexOutOfBoundsException("Sided slot " + slot);
        }
        return slots.get(slot);
    }
}
