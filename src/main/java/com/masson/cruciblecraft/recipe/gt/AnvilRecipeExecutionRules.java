package com.masson.cruciblecraft.recipe.gt;

/** Physical constraints of the two-slot anvil execution transaction. */
public final class AnvilRecipeExecutionRules {
    private AnvilRecipeExecutionRules() {}

    public static boolean supportsOutputs(int outputCount, int primaryChance) {
        return outputCount >= 1
                && outputCount <= 2
                && primaryChance == 10_000;
    }
}
