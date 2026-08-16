package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Small Java plugin boundary for cover callbacks. Numeric policy remains in
 * {@link CoverDefinition}; tick, transfer and configuration semantics live
 * here.
 */
public interface CoverBehavior {
    default boolean allowsIncoming(
            PipeCover cover,
            CoverDefinition definition,
            Access access) {
        return true;
    }

    default boolean allowsOutgoing(
            PipeCover cover,
            CoverDefinition definition,
            Access access) {
        return true;
    }

    default int limitIncoming(
            PipeCover cover,
            CoverDefinition definition,
            Access access,
            int requested) {
        return Math.max(0, requested);
    }

    default boolean matchesItem(
            PipeCover cover,
            CoverDefinition definition,
            ItemStack stack) {
        return true;
    }

    default boolean matchesFluid(
            PipeCover cover,
            CoverDefinition definition,
            FluidStack stack) {
        return true;
    }

    default void tick(
            PipeCover cover,
            CoverDefinition definition,
            TransferContext context) {}

    default PipeCover configure(
            PipeCover cover,
            CoverDefinition definition,
            ConfigRequest request) {
        if (!definition.configurable().contains(request.field())) {
            throw new IllegalArgumentException(
                    definition.id() + ": field is not configurable");
        }
        PipeCover changed = cover.withConfig(
                cover.config().with(request.field(), request.value()));
        definition.resolve(changed.config());
        return changed;
    }

    record Access(
            CoverDefinition.Medium medium,
            int storedAmount,
            int capacity) {
        public Access {
            if (storedAmount < 0 || capacity < 0 || storedAmount > capacity) {
                throw new IllegalArgumentException(
                        "Invalid cover access pressure values");
            }
        }
    }

    record ConfigRequest(
            CoverDefinition.ConfigField field,
            int value) {}

    interface TransferContext {
        CoverDefinition.Medium medium();

        Direction side();

        int storedAmount();

        int capacity();

        int transferItems(
                int amount,
                Optional<String> matchId,
                CoverDefinition.TransferMode mode);

        int transferFluids(
                int amount,
                Optional<String> matchId,
                CoverDefinition.TransferMode mode);
    }
}
