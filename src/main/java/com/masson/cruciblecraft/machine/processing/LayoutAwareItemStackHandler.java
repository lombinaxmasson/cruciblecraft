package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.IntConsumer;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Keeps the current spec size when loading older, smaller inventories.
 *
 * <p>A larger saved layout is retained intact and reported as quarantined so a
 * future layout shrink cannot silently discard inaccessible stacks.
 */
public final class LayoutAwareItemStackHandler extends ItemStackHandler {
    private final int expectedSlots;
    private final BiPredicate<Integer, ItemStack> validity;
    private final IntConsumer mutation;
    private int loadedSlots;
    private boolean layoutQuarantined;

    public LayoutAwareItemStackHandler(
            int expectedSlots,
            BiPredicate<Integer, ItemStack> validity,
            IntConsumer mutation) {
        super(expectedSlots);
        if (expectedSlots < 0) {
            throw new IllegalArgumentException("expectedSlots must be non-negative");
        }
        this.expectedSlots = expectedSlots;
        this.validity = Objects.requireNonNull(validity, "validity");
        this.mutation = Objects.requireNonNull(mutation, "mutation");
        this.loadedSlots = expectedSlots;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < expectedSlots && validity.test(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        mutation.accept(slot);
    }

    /**
     * Loads without allowing ItemStackHandler to shrink below the live spec.
     */
    public void deserializeForLayout(
            HolderLookup.Provider registries,
            CompoundTag serialized) {
        int savedSlots = serialized.contains("Size", Tag.TAG_INT)
                ? serialized.getInt("Size")
                : expectedSlots;
        loadedSlots = savedSlots;
        layoutQuarantined = savedSlots < 0 || savedSlots > expectedSlots;
        CompoundTag normalized = serialized.copy();
        normalized.putInt(
                "Size",
                layoutQuarantined
                        ? Math.max(expectedSlots, Math.max(0, savedSlots))
                        : expectedSlots);
        super.deserializeNBT(registries, normalized);
    }

    public int expectedSlots() {
        return expectedSlots;
    }

    public int loadedSlots() {
        return loadedSlots;
    }

    public boolean layoutQuarantined() {
        return layoutQuarantined;
    }
}
