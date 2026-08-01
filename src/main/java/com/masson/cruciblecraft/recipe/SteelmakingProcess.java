package com.masson.cruciblecraft.recipe;

import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;

/**
 * Integer-unit steelmaking rules. A high-carbon 3:1 iron/carbon charge is
 * blown down into a 1-2 carbon-unit-per-ingot steel range.
 */
public final class SteelmakingProcess {
    public static final String IRON = "iron";
    public static final String CARBON = "carbon";
    public static final String STEEL = "steel";
    public static final int REACTION_INTERVAL_TICKS = 20;
    public static final int CARBON_CONSUMED_PER_CYCLE = 10;

    private SteelmakingProcess() {}

    public static Optional<Batch> begin(Map<String, Integer> composition) {
        if (composition.size() != 2) {
            return Optional.empty();
        }
        int ironUnits = composition.getOrDefault(IRON, 0);
        int carbonUnits = composition.getOrDefault(CARBON, 0);
        int threeIngots = MaterialPrefixes.INGOT.units() * 3;
        if (ironUnits <= 0
                || ironUnits % threeIngots != 0
                || carbonUnits <= 0
                || carbonUnits * 3 != ironUnits) {
            return Optional.empty();
        }
        return resume(ironUnits);
    }

    public static Optional<Batch> resume(int ironUnits) {
        int threeIngots = MaterialPrefixes.INGOT.units() * 3;
        if (ironUnits <= 0 || ironUnits % threeIngots != 0) {
            return Optional.empty();
        }
        int ingots = ironUnits / MaterialPrefixes.INGOT.units();
        return Optional.of(new Batch(ironUnits, ingots, ingots * 2));
    }

    public static CarbonStep consumeCarbon(Batch batch, int carbonUnits) {
        if (carbonUnits <= batch.maximumSteelCarbonUnits()) {
            return new CarbonStep(carbonUnits, true);
        }
        int remaining = Math.max(
                batch.maximumSteelCarbonUnits(),
                carbonUnits - CARBON_CONSUMED_PER_CYCLE);
        return new CarbonStep(remaining, remaining <= batch.maximumSteelCarbonUnits());
    }

    public record Batch(
            int ironUnits,
            int minimumSteelCarbonUnits,
            int maximumSteelCarbonUnits) {}

    public record CarbonStep(int remainingCarbonUnits, boolean steelRangeReached) {}
}
