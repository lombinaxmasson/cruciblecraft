package com.masson.cruciblecraft.content.mold;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialForm;

public final class MoldCastingRules {
    public static final float HEAT_RESISTANCE_BONUS = 1.25F;
    public static final float COOLING_PER_TICK = 5.0F;
    private static final int MAX_OUTPUT_COUNT = 64;

    private MoldCastingRules() {}

    public static Optional<Batch> smallestBatch(
            Map<String, Integer> costPerIngot,
            MaterialForm form) {
        if (costPerIngot.isEmpty()) {
            return Optional.empty();
        }
        for (int outputCount = 1; outputCount <= MAX_OUTPUT_COUNT; outputCount++) {
            Map<String, Integer> cost = new LinkedHashMap<>();
            boolean integral = true;
            for (var component : costPerIngot.entrySet()) {
                long scaled = (long) component.getValue() * form.units() * outputCount;
                if (scaled % MaterialForm.INGOT.units() != 0L) {
                    integral = false;
                    break;
                }
                int units = Math.toIntExact(scaled / MaterialForm.INGOT.units());
                if (units <= 0) {
                    integral = false;
                    break;
                }
                cost.put(component.getKey(), units);
            }
            if (integral) {
                return Optional.of(new Batch(Map.copyOf(cost), outputCount));
            }
        }
        return Optional.empty();
    }

    public static float cool(float temperature, float ambientTemperature) {
        if (temperature > ambientTemperature) {
            return Math.max(ambientTemperature, temperature - COOLING_PER_TICK);
        }
        if (temperature < ambientTemperature) {
            return Math.min(ambientTemperature, temperature + COOLING_PER_TICK);
        }
        return temperature;
    }

    public static float maximumTemperature(float ceramicMeltingPoint) {
        return (float) ((ceramicMeltingPoint + 273.15F) * HEAT_RESISTANCE_BONUS - 273.15F);
    }

    public record Batch(Map<String, Integer> cost, int outputCount) {
        public Batch {
            cost = Map.copyOf(cost);
            if (outputCount <= 0) {
                throw new IllegalArgumentException("Output count must be positive");
            }
        }
    }
}
