package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;
import java.util.Optional;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;

/**
 * Immutable interpretation of one canonical recipe by one kind/tier pair.
 *
 * <p>Recipes remain tier-neutral. The plan applies the pinned GT6 input-window,
 * standard/cheap overclock and parallel-duration policies at runtime.
 */
public record MachineExecutionPlan(
        long minimumPower,
        long nominalPower,
        long maximumPower,
        long totalWork,
        int effectiveDuration,
        int operations,
        int overclockSteps) {

    public MachineExecutionPlan {
        if (minimumPower < 0L
                || nominalPower < minimumPower
                || maximumPower < nominalPower
                || totalWork <= 0L
                || effectiveDuration <= 0
                || operations <= 0
                || overclockSteps < 0) {
            throw new IllegalArgumentException("Invalid machine execution plan");
        }
    }

    public static Optional<MachineExecutionPlan> create(
            GTRecipe recipe,
            MachineKindSpec kind,
            TierProfile tier,
            int requestedOperations) {
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(tier, "tier");
        if (recipe.eut() < 0L
                || recipe.eut() > tier.inputMaximum()
                || recipe.duration() <= 0
                || requestedOperations <= 0
                || requestedOperations > tier.parallelLimit()) {
            return Optional.empty();
        }

        if (recipe.eut() == 0L) {
            if (requestedOperations != 1) {
                return Optional.empty();
            }
            return Optional.of(new MachineExecutionPlan(
                    0L,
                    0L,
                    tier.inputMaximum(),
                    recipe.duration(),
                    recipe.duration(),
                    1,
                    0));
        }
        try {
            long recipePower = Math.max(1L, recipe.eut());
            if (kind.overclockPolicy()
                    == MachineKindSpec.OverclockPolicy.LEGACY_TICKS) {
                if (requestedOperations != 1) {
                    return Optional.empty();
                }
                return Optional.of(new MachineExecutionPlan(
                        recipePower,
                        recipePower,
                        tier.inputMaximum(),
                        Math.multiplyExact(
                                recipePower, recipe.duration()),
                        recipe.duration(),
                        1,
                        0));
            }
            long minimumPower = kind.parallelDuration()
                    ? recipePower
                    : Math.multiplyExact(recipePower, requestedOperations);
            long totalWork = Math.multiplyExact(
                    Math.multiplyExact(recipePower, recipe.duration()),
                    requestedOperations);
            totalWork = divideCeil(
                    Math.multiplyExact(totalWork, 10_000L),
                    tier.efficiency());

            int overclocks = 0;
            if (kind.overclockPolicy()
                    == MachineKindSpec.OverclockPolicy.STANDARD) {
                while (minimumPower < tier.inputMinimum()
                        && minimumPower <= tier.inputMaximum() / 4L) {
                    minimumPower = Math.multiplyExact(minimumPower, 4L);
                    totalWork = Math.multiplyExact(totalWork, 2L);
                    overclocks++;
                }
            }
            if (minimumPower > tier.inputMaximum()) {
                return Optional.empty();
            }
            long nominalPower = Math.max(
                    minimumPower, tier.inputNominal());
            nominalPower = Math.min(nominalPower, tier.inputMaximum());
            long ticks = divideCeil(totalWork, nominalPower);
            if (ticks > Integer.MAX_VALUE) {
                return Optional.empty();
            }
            return Optional.of(new MachineExecutionPlan(
                    minimumPower,
                    nominalPower,
                    tier.inputMaximum(),
                    totalWork,
                    (int) ticks,
                    requestedOperations,
                    overclocks));
        } catch (ArithmeticException overflow) {
            return Optional.empty();
        }
    }

    private static long divideCeil(long value, long divisor) {
        return value / divisor + (value % divisor == 0L ? 0L : 1L);
    }
}
