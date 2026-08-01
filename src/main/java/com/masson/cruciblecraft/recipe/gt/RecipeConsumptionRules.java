package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Objects;

/** Pure cross-resource validation for whether a recipe consumes anything. */
public final class RecipeConsumptionRules {
    private RecipeConsumptionRules() {}

    public static boolean consumesAnything(
            List<Integer> itemInputCounts,
            boolean hasFluidInputs) {
        Objects.requireNonNull(itemInputCounts, "itemInputCounts");
        return hasFluidInputs || itemInputCounts.stream().anyMatch(count -> count > 0);
    }
}
