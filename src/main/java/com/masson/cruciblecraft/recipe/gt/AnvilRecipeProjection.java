package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.masson.cruciblecraft.recipe.AnvilMode;

final class AnvilRecipeProjection {
    private static final int ANVIL_DURATION = 1;
    private static final int GUARANTEED_CHANCE = 10_000;

    private AnvilRecipeProjection() {}

    static <T> Plan<T> project(
            T primaryInput,
            int primaryInputCount,
            Optional<T> secondInput,
            int secondInputCount,
            T primaryOutput,
            int primaryOutputCount,
            Optional<T> secondaryOutput,
            int secondaryOutputCount,
            double secondaryChance,
            AnvilMode mode,
            long recipePower,
            int hits) {
        Objects.requireNonNull(primaryInput, "primaryInput");
        Objects.requireNonNull(secondInput, "secondInput");
        Objects.requireNonNull(primaryOutput, "primaryOutput");
        Objects.requireNonNull(secondaryOutput, "secondaryOutput");
        Objects.requireNonNull(mode, "mode");

        List<ResourceStack<T>> inputs = new ArrayList<>();
        inputs.add(new ResourceStack<>(primaryInput, primaryInputCount));
        secondInput.ifPresent(resource ->
                inputs.add(new ResourceStack<>(resource, secondInputCount)));

        List<ResourceStack<T>> outputs = new ArrayList<>();
        List<Integer> chances = new ArrayList<>();
        outputs.add(new ResourceStack<>(primaryOutput, primaryOutputCount));
        chances.add(GUARANTEED_CHANCE);
        secondaryOutput.ifPresent(resource -> {
            outputs.add(new ResourceStack<>(resource, secondaryOutputCount));
            chances.add(RecipeExpansionRules.chanceToTenThousandths(secondaryChance));
        });

        return new Plan<>(
                inputs,
                outputs,
                chances,
                mode,
                ANVIL_DURATION,
                recipePower,
                hits);
    }

    record ResourceStack<T>(T resource, int count) {
        ResourceStack {
            Objects.requireNonNull(resource, "resource");
            if (count <= 0) {
                throw new IllegalArgumentException("Resource count must be positive");
            }
        }
    }

    record Plan<T>(
            List<ResourceStack<T>> itemInputs,
            List<ResourceStack<T>> itemOutputs,
            List<Integer> outputChances,
            AnvilMode mode,
            int duration,
            long eut,
            long specialValue) {
        Plan {
            itemInputs = List.copyOf(itemInputs);
            itemOutputs = List.copyOf(itemOutputs);
            outputChances = List.copyOf(outputChances);
            Objects.requireNonNull(mode, "mode");
        }
    }
}
