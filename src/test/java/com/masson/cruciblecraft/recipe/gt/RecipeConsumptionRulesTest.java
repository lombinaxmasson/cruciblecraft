package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RecipeConsumptionRulesTest {
    @Test
    void rejectsRecipeWhoseInputsAreAllPresenceOnly() {
        assertFalse(RecipeConsumptionRules.consumesAnything(List.of(0, 0), false));
    }

    @Test
    void acceptsPositiveItemOrFluidConsumption() {
        assertTrue(RecipeConsumptionRules.consumesAnything(List.of(0, 1), false));
        assertTrue(RecipeConsumptionRules.consumesAnything(List.of(0), true));
    }
}
