package com.masson.cruciblecraft.recipe;

import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialForm;

public final class AnvilRecipeRules {
    private AnvilRecipeRules() {}

    public static Optional<String> normalizeMaterial(Optional<String> material) {
        return material.filter(value -> !value.isBlank());
    }

    public static void validate(
            MaterialForm input,
            MaterialForm output,
            int outputCount,
            int hits) {
        if (outputCount <= 0) {
            throw new IllegalArgumentException("Anvil recipe output_count must be positive");
        }
        if (hits <= 0) {
            throw new IllegalArgumentException("Anvil recipe hits must be positive");
        }
        int inputUnits = input.units();
        int outputUnits = output.units() * outputCount;
        if (inputUnits != outputUnits) {
            throw new IllegalArgumentException(
                    "Anvil recipe must conserve material units: "
                            + input.serializedName() + "=" + inputUnits
                            + ", " + output.serializedName() + "x" + outputCount
                            + "=" + outputUnits);
        }
    }

    public static void validate(
            MaterialForm input,
            int inputCount,
            Optional<MaterialForm> secondInput,
            int secondInputCount,
            MaterialForm output,
            int outputCount,
            int hits,
            Optional<MaterialForm> secondaryOutput,
            int secondaryOutputCount,
            double secondaryChance,
            long recipePower) {
        if (inputCount <= 0 || (secondInput.isPresent() && secondInputCount <= 0)) {
            throw new IllegalArgumentException("Anvil recipe input counts must be positive");
        }
        if (outputCount <= 0 || (secondaryOutput.isPresent() && secondaryOutputCount <= 0)) {
            throw new IllegalArgumentException("Anvil recipe output counts must be positive");
        }
        if (hits <= 0 || recipePower <= 0) {
            throw new IllegalArgumentException("Anvil recipe hits and recipe_power must be positive");
        }
        if (!Double.isFinite(secondaryChance)
                || secondaryChance < 0.0
                || secondaryChance > 1.0) {
            throw new IllegalArgumentException("secondary_chance must be between 0 and 1");
        }

        int inputUnits = input.units() * inputCount
                + secondInput.map(form -> form.units() * secondInputCount).orElse(0);
        int maximumOutputUnits = output.units() * outputCount
                + secondaryOutput.map(form -> form.units() * secondaryOutputCount).orElse(0);
        if (secondInput.isEmpty() && secondaryOutput.isEmpty()) {
            validate(input, output, outputCount, hits);
        } else if (maximumOutputUnits > inputUnits) {
            throw new IllegalArgumentException("Anvil recipe outputs exceed consumed material units");
        }
    }

    public static boolean secondarySucceeds(double chance, double roll) {
        if (!Double.isFinite(roll) || roll < 0.0 || roll >= 1.0) {
            throw new IllegalArgumentException("Secondary output roll must be in [0, 1)");
        }
        return chance > 0.0 && roll < chance;
    }

    public static RemainingCounts consume(
            int firstCount,
            int secondCount,
            int primarySlot,
            int primaryCount,
            int secondarySlot,
            int secondaryCount) {
        if (firstCount < 0 || secondCount < 0
                || primarySlot < 0 || primarySlot > 1
                || primaryCount <= 0
                || (secondarySlot >= 0 && (secondarySlot > 1
                        || secondarySlot == primarySlot
                        || secondaryCount <= 0))) {
            throw new IllegalArgumentException("Invalid anvil consumption plan");
        }
        int firstUsed = primarySlot == 0 ? primaryCount : 0;
        int secondUsed = primarySlot == 1 ? primaryCount : 0;
        if (secondarySlot == 0) {
            firstUsed += secondaryCount;
        } else if (secondarySlot == 1) {
            secondUsed += secondaryCount;
        }
        if (firstUsed > firstCount || secondUsed > secondCount) {
            throw new IllegalArgumentException("Anvil consumption exceeds available inputs");
        }
        return new RemainingCounts(firstCount - firstUsed, secondCount - secondUsed);
    }

    public record RemainingCounts(int first, int second) {}
}
