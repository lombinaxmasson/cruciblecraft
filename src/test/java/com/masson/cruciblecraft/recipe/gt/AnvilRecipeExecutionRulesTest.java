package com.masson.cruciblecraft.recipe.gt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilRecipeExecutionRulesTest {
    @Test
    void rejectsOutputsTheTwoSlotTransactionCannotExecute() {
        assertTrue(AnvilRecipeExecutionRules.supportsOutputs(1, 10_000));
        assertTrue(AnvilRecipeExecutionRules.supportsOutputs(2, 10_000));
        assertFalse(AnvilRecipeExecutionRules.supportsOutputs(0, 10_000));
        assertFalse(AnvilRecipeExecutionRules.supportsOutputs(3, 10_000));
        assertFalse(AnvilRecipeExecutionRules.supportsOutputs(2, 5_000));
    }
}
