package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecipeExpansionRulesTest {
    @Test
    void materialSpecificRulesAlwaysSortBeforeGenericRules() {
        assertTrue(RecipeExpansionRules.comparePriority(
                true, "z:specific", false, "a:generic") < 0);
        assertTrue(RecipeExpansionRules.comparePriority(
                false, "a:generic", true, "z:specific") > 0);
        assertTrue(RecipeExpansionRules.comparePriority(
                false, "a:first", false, "b:second") < 0);
    }

    @Test
    void chanceConversionRoundsToNearestTenThousandth() {
        assertEquals(0, RecipeExpansionRules.chanceToTenThousandths(0.0));
        assertEquals(2_500, RecipeExpansionRules.chanceToTenThousandths(0.25));
        assertEquals(10_000, RecipeExpansionRules.chanceToTenThousandths(1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> RecipeExpansionRules.chanceToTenThousandths(Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> RecipeExpansionRules.chanceToTenThousandths(1.01));
    }

    @Test
    void missingRequiredFormsSkipExpansion() {
        assertTrue(RecipeExpansionRules.formsAvailable(
                true, true, true, true, true, true));
        assertFalse(RecipeExpansionRules.formsAvailable(
                false, true, false, false, false, false));
        assertFalse(RecipeExpansionRules.formsAvailable(
                true, true, true, false, false, false));
        assertFalse(RecipeExpansionRules.formsAvailable(
                true, true, false, false, true, false));
    }

    @Test
    void expandedIdsRetainRuleAndMaterialIdentity() {
        assertEquals(
                "anvil/ingot_to_plate/copper",
                RecipeExpansionRules.expandedPath("anvil/ingot_to_plate", "copper"));
        assertThrows(
                IllegalArgumentException.class,
                () -> RecipeExpansionRules.expandedPath("", "copper"));
    }
}
