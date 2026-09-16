package com.masson.cruciblecraft.machine.processing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Directional item view: input views only insert, output views only extract. */
public final class SidedItemHandler implements IItemHandler {
    private final IItemHandler delegate;
    private final List<Integer> slots;
    private final Set<Integer> insertSlots;
    private final Set<Integer> extractSlots;
    private final ProcessingMachineSpec.CapabilityAccess access;
    private final Runnable mutation;
    private final IntPredicate insertAllowed;

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
        this(
                delegate,
                access == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                        ? List.of() : slots,
                access == ProcessingMachineSpec.CapabilityAccess.INPUT
                        ? List.of() : slots,
                access,
                mutation,
                slot -> true);
    }

    public SidedItemHandler(
            IItemHandler delegate,
            List<Integer> inputs,
            List<Integer> outputs,
            ProcessingMachineSpec.CapabilityAccess access,
            Runnable mutation,
            IntPredicate insertAllowed) {
        this.delegate = delegate;
        this.access = access;
        this.mutation = mutation;
        this.insertAllowed = insertAllowed;
        if (access == ProcessingMachineSpec.CapabilityAccess.NONE) {
            throw new IllegalArgumentException("Do not expose a NONE capability adapter");
        }
        List<Integer> exposed = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        this.insertSlots = new HashSet<>(inputs);
        this.extractSlots = new HashSet<>(outputs);
        for (int slot : inputs) {
            if (seen.add(slot)) {
                exposed.add(slot);
            }
        }
        for (int slot : outputs) {
            if (seen.add(slot)) {
                exposed.add(slot);
            }
        }
        this.slots = List.copyOf(exposed);
    }

    @Override public int getSlots() { return slots.size(); }
    @Override public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(actual(slot));
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int actual = actual(slot);
        if (!insertSlots.contains(actual) || !insertAllowed.test(actual)) {
            return stack;
        }
        ItemStack remainder = delegate.insertItem(actual, stack, simulate);
        if (!simulate && remainder.getCount() < stack.getCount()) {
            mutation.run();
        }
        return remainder;
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int actual = actual(slot);
        if (!extractSlots.contains(actual)) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = delegate.extractItem(actual, amount, simulate);
        if (!simulate && !extracted.isEmpty()) {
            mutation.run();
        }
        return extracted;
    }
    @Override public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(actual(slot));
    }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        int actual = actual(slot);
        return insertSlots.contains(actual)
                && insertAllowed.test(actual)
                && delegate.isItemValid(actual, stack);
    }

    private int actual(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            throw new IndexOutOfBoundsException("Sided slot " + slot);
        }
        return slots.get(slot);
    }
}
