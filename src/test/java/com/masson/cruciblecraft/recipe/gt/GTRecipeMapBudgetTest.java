package com.masson.cruciblecraft.recipe.gt;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GTRecipeMapBudgetTest {
    @Test
    void publicationMetricsExcludeVerificationOnlyLookupAndInvalidHeapDelta() {
        assertEquals(
                List.of(
                        "t3ComponentRecipes",
                        "t4ToolRecipes",
                        "liveT3MapRecipes",
                        "t5ChemicalRecipes",
                        "t7AuthoredMaterialRules",
                        "t8PipeMaterialRules",
                        "t10KnownFormMaterialRules",
                        "allPublishedRecipes",
                        "reloadMillis",
                        "indexMillis"),
                Arrays.stream(
                                GTRecipeMapLoader.PublicationMetrics.class
                                        .getRecordComponents())
                        .map(component -> component.getName())
                        .toList());
    }

    @Test
    void t4ToolRecipesHaveAnExplicitProvenanceBoundary() {
        assertTrue(GTRecipeMapLoader.isT4ToolRecipe(id(
                "t4/assembler/pickaxe/metal/iron")));
        assertFalse(GTRecipeMapLoader.isT4ToolRecipe(id(
                "assembler/plates_to_gear/iron")));
        assertFalse(GTRecipeMapLoader.isT4ToolRecipe(
                ResourceLocation.fromNamespaceAndPath(
                        "other", "t4/assembler/pickaxe/metal/iron")));
    }

    @Test
    void authoredRulesUseStagePathsAndIndependentBudgets() {
        assertEquals(
                256,
                ModProcessingMachines.T7_AUTHORED_MATERIAL_RULE_BUDGET);
        assertEquals(
                320,
                ModProcessingMachines.T8_PIPE_MATERIAL_RULE_BUDGET);
        assertEquals(
                1_500,
                ModProcessingMachines.T10_AUTHORED_MATERIAL_RULE_BUDGET);
        assertEquals(21_000, ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET);
        assertEquals(
                7,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "t7/mortar/ingot_to_dust/iron"))
                        .orElseThrow());
        assertEquals(
                8,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "t8/extruder/plates_to_fluid_pipe/copper"))
                        .orElseThrow());
        assertEquals(
                10,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "t10/anvil/ingot_to_double_ingot/copper"))
                        .orElseThrow());
        assertEquals(
                11,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "t11/future_rule/copper"))
                        .orElseThrow());
        assertTrue(GTRecipeMapLoader.authoredMaterialRuleStage(id(
                        "mortar/crushed_to_dust/copper"))
                .isEmpty());
        assertTrue(GTRecipeMapLoader.authoredMaterialRuleStage(
                        ResourceLocation.fromNamespaceAndPath(
                                "other", "t7/mortar/ingot_to_dust/iron"))
                .isEmpty());
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(7, 220));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(8, 257));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(10, 1_288));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(
                        7,
                        ModProcessingMachines
                                .T7_AUTHORED_MATERIAL_RULE_BUDGET + 1));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(
                        8,
                        ModProcessingMachines.T8_PIPE_MATERIAL_RULE_BUDGET + 1));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(10, 1_501));
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(11, 0));
    }

    @Test
    void componentToolAndLiveBudgetsFailIndependently() {
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateExpansionBudgets(
                ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET,
                ModProcessingMachines.LIVE_T3_MAP_RECIPE_BUDGET
                        - ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET,
                ModProcessingMachines.LIVE_T3_MAP_RECIPE_BUDGET));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateExpansionBudgets(
                        ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET + 1,
                        0,
                        ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET + 1));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateExpansionBudgets(
                        0,
                        ModProcessingMachines.T4_TOOL_EXPANSION_BUDGET + 1,
                        ModProcessingMachines.T4_TOOL_EXPANSION_BUDGET + 1));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateExpansionBudgets(
                        ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET,
                        ModProcessingMachines.LIVE_T3_MAP_RECIPE_BUDGET
                                - ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET + 1,
                        ModProcessingMachines.LIVE_T3_MAP_RECIPE_BUDGET + 1));
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateExpansionBudgets(1, 1, 1));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
