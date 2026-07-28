package com.masson.cruciblecraft.recipe;

import com.masson.cruciblecraft.api.material.MaterialForm;

/** Loader-independent validation for crusher datapack recipes. */
public final class CrusherRecipeRules {
    private CrusherRecipeRules() {}

    public static void validate(
            MaterialForm input,
            MaterialForm output,
            int outputCount,
            int power,
            int duration) {
        if (input == null || output == null || input == output
                || outputCount <= 0 || power <= 0 || duration <= 0) {
            throw new IllegalArgumentException("Invalid crusher recipe");
        }
    }
}
