package com.masson.cruciblecraft.recipe.gt;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GTRecipeMapBudgetTest {
    @BeforeAll
    static void ensureCatalog(@TempDir Path configDirectory) {
        MinecraftTestBootstrap.bootstrap();
        if (!MaterialCatalog.isBootstrapped()) {
            MaterialCatalog.bootstrap(configDirectory);
        }
    }

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
                        "eagerPublishedRecipes",
                        "lazyLogicalRecipes",
                        "t14ExtruderLogicalRecipes",
                        "t14ExtruderEagerRecipes",
                        "t14ExtruderLazyRecipes",
                        "t14ExtruderCacheCeiling",
                        "t14ExtruderSyncBytes",
                        "t14ExtruderAuthoredEntries",
                        "t14ExtruderStableFingerprint",
                        "compactFamilyAuthoredEntries",
                        "compactFamilyLogicalRecipes",
                        "compactFamilyEagerRecipes",
                        "compactFamilyLazyRecipes",
                        "compactFamilyCacheCeiling",
                        "compactFamilyUnindexedRelations",
                        "compactFamilySyncBytes",
                        "compactFamilyStableFingerprint",
                        "runtimeSide",
                        "reloadMillis",
                        "indexMillis"),
                Arrays.stream(
                                GTRecipeMapLoader.PublicationMetrics.class
                                        .getRecordComponents())
                        .map(component -> component.getName())
                        .toList());

        RecipeMap staleOwner = new RecipeMap(id("metrics_failure"));
        staleOwner.replaceRecipes(List.of());
        RecipeMap.Prepared stale = staleOwner.prepareRecipes(List.of());
        staleOwner.replaceRecipes(List.of());
        boolean[] candidateCommitted = {false};
        var preview = MaterialCatalog.previewRuntime(List.of(), java.util.Map.of());
        assertThrows(
                IllegalStateException.class,
                () -> GTRecipeRuntimeEpoch.publish(
                        preview,
                        List.of(stale),
                        () -> candidateCommitted[0] = true));
        assertFalse(
                candidateCommitted[0],
                "failed publication must not replace publication-adjacent metrics");
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
        assertEquals(
                0,
                ModProcessingMachines.T11_AUTHORED_MATERIAL_RULE_BUDGET);
        assertEquals(
                0,
                ModProcessingMachines.T12_AUTHORED_MATERIAL_RULE_BUDGET);
        assertEquals(21_000, ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET);
        assertEquals(
                18_000,
                ModProcessingMachines.ALL_EAGER_PUBLICATION_SOFT_BUDGET);
        assertEquals(
                56_000,
                ModProcessingMachines
                        .ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING);
        assertEquals(
                4_096,
                ModProcessingMachines
                        .ALL_LAZY_RECIPE_CACHE_HARD_CEILING);
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateT14MaterializationBudgets(
                        16_650, 2_225, 512));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateT14MaterializationBudgets(
                        ModProcessingMachines
                                .ALL_EAGER_PUBLICATION_SOFT_BUDGET + 1,
                        2_225,
                        512));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateT14MaterializationBudgets(
                        ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET + 1,
                        2_225,
                        512));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateT14MaterializationBudgets(
                        16_650, 56_001, 512));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateT14MaterializationBudgets(
                        16_650, 2_225, 4_097));
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
        assertEquals(
                12,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "t12/future_rule/copper"))
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
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(11, 0));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(12, 0));
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
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(11, 1));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(12, 1));
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

    @Test
    void dedicatedClientAndLookupMetricsUseOnlineBudgetGate() {
        var metrics = new GTRecipeMapLoader.PublicationMetrics(
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 512, 331_124L, 20,
                "a".repeat(64),
                0, 0, 0, 0, 0, 0, 0L, "",
                ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                ModProcessingMachines.CLIENT_RECIPE_RELOAD_BUDGET_MS,
                ModProcessingMachines.CLIENT_RECIPE_INDEX_BUILD_BUDGET_MS);
        var lookup = new GTRecipeMapLoader.T14LookupMetrics(
                61,
                1_952,
                ModProcessingMachines.RECIPE_LOOKUP_P95_BUDGET_NS,
                ModProcessingMachines.RECIPE_LOOKUP_P95_CANDIDATE_BUDGET,
                ModProcessingMachines
                        .RECIPE_LOOKUP_MAX_CANDIDATE_HARD_CEILING);
        assertTrue(GTRecipeMapLoader.evaluateT14OnlineBudgetGate(
                metrics, lookup).allPass());

        var overClientReload = new GTRecipeMapLoader.PublicationMetrics(
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 512, 331_124L, 20,
                "a".repeat(64),
                0, 0, 0, 0, 0, 0, 0L, "",
                ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                ModProcessingMachines.CLIENT_RECIPE_RELOAD_BUDGET_MS + 1,
                ModProcessingMachines.CLIENT_RECIPE_INDEX_BUILD_BUDGET_MS);
        assertFalse(GTRecipeMapLoader.evaluateT14OnlineBudgetGate(
                overClientReload, lookup).sideReload());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
