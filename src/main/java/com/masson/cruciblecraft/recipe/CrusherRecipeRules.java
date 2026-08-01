package com.masson.cruciblecraft.recipe;

import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefix;

/** Loader-independent validation for crusher datapack recipes. */
public final class CrusherRecipeRules {
    private CrusherRecipeRules() {}

    public static void validate(
            MaterialPrefix input,
            MaterialPrefix output,
            int outputCount,
            int power,
            int duration) {
        validate(input, output, outputCount, power, duration, Map.of());
    }

    public static void validate(
            MaterialPrefix input,
            MaterialPrefix output,
            int outputCount,
            int power,
            int duration,
            Map<String, Integer> materialDurations) {
        if (input == null || output == null || input == output
                || outputCount <= 0 || power <= 0 || duration <= 0
                || materialDurations == null) {
            throw new IllegalArgumentException("Invalid crusher recipe");
        }
        if (materialDurations.entrySet().stream().anyMatch(entry ->
                entry.getKey() == null
                        || !entry.getKey().matches("[a-z0-9_]+")
                        || entry.getValue() == null
                        || entry.getValue() <= 0)) {
            throw new IllegalArgumentException(
                    "Crusher material durations require valid material ids and positive ticks");
        }
    }

    public static int durationFor(
            String materialId,
            int defaultDuration,
            Map<String, Integer> materialDurations) {
        if (materialId == null
                || defaultDuration <= 0
                || materialDurations == null) {
            throw new IllegalArgumentException("Invalid crusher duration lookup");
        }
        return materialDurations.getOrDefault(materialId, defaultDuration);
    }
}
