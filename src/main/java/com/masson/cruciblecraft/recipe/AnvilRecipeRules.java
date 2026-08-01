package com.masson.cruciblecraft.recipe;

import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;

public final class AnvilRecipeRules {
    private AnvilRecipeRules() {}

    public static Optional<String> normalizeMaterial(Optional<String> material) {
        return material.filter(value -> !value.isBlank());
    }

    public static void validate(
            MaterialPrefix input,
            MaterialPrefix output,
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
            MaterialPrefix input,
            int inputCount,
            Optional<MaterialPrefix> secondInput,
            int secondInputCount,
            MaterialPrefix output,
            int outputCount,
            int hits,
            Optional<MaterialPrefix> secondaryOutput,
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

}
