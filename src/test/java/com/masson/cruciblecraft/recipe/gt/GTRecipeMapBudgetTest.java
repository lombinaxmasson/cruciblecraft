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
                        "componentRecipes",
                        "toolRecipes",
                        "liveComponentMapRecipes",
                        "chemicalPublishedRecipes",
                        "mortarAuthoredMaterialRules",
                        "pipeMaterialRules",
                        "ingotFormMaterialRules",
                        "allPublishedRecipes",
                        "eagerPublishedRecipes",
                        "lazyLogicalRecipes",
                        "compactLoadExtruderLogicalRecipes",
                        "compactLoadExtruderEagerRecipes",
                        "compactLoadExtruderLazyRecipes",
                        "compactLoadExtruderCacheCeiling",
                        "compactLoadExtruderSyncBytes",
                        "compactLoadExtruderAuthoredEntries",
                        "compactLoadExtruderStableFingerprint",
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
                        "indexMillis",
                        "control",
                        "phaseTimings",
                        "allocation"),
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
    void toolRecipesHaveAnExplicitProvenanceBoundary() {
        assertTrue(GTRecipeMapLoader.isToolRecipe(id(
                "tool/assembler/pickaxe/metal/iron")));
        assertFalse(GTRecipeMapLoader.isToolRecipe(id(
                "assembler/plates_to_gear/iron")));
        assertFalse(GTRecipeMapLoader.isToolRecipe(
                ResourceLocation.fromNamespaceAndPath(
                        "other", "tool/assembler/pickaxe/metal/iron")));
    }

    @Test
    void authoredRulesUseStagePathsAndIndependentBudgets() {
        assertEquals(
                256,
                ModProcessingMachines.MORTAR_MATERIAL_RULE_BUDGET);
        assertEquals(
                320,
                ModProcessingMachines.PIPE_MATERIAL_RULE_BUDGET);
        assertEquals(
                1_500,
                ModProcessingMachines.INGOT_FORM_MATERIAL_RULE_BUDGET);
        assertEquals(
                0,
                ModProcessingMachines.HYDROCARBON_MATERIAL_RULE_BUDGET);
        assertEquals(
                0,
                ModProcessingMachines.TIER_PROFILE_MATERIAL_RULE_BUDGET);
        assertEquals(16_980, ModProcessingMachines.VERIFIED_OPENING_EAGER_PUBLISHED);
        assertEquals(50_652, ModProcessingMachines.VERIFIED_OPENING_LAZY_LOGICAL);
        assertEquals(876, ModProcessingMachines.VERIFIED_OPENING_CACHE_CEILING);
        assertEquals(6_269, ModProcessingMachines.VERIFIED_OPENING_AUTHORED_ENTRIES);
        assertEquals(41_000, ModProcessingMachines.TEMPORARY_EAGER_COMPATIBILITY_CEILING);
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
                GTRecipeMapLoader.validateCompactLoadMaterializationBudgets(
                        16_650, 2_225, 512));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateCompactLoadMaterializationBudgets(
                        16_605,
                        4_933,
                        198));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateCompactLoadMaterializationBudgets(
                        ModProcessingMachines
                                .ALL_EAGER_PUBLICATION_SOFT_BUDGET + 1,
                        2_225,
                        512));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateCompactLoadMaterializationBudgets(
                        ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET + 1,
                        2_225,
                        512));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateCompactLoadMaterializationBudgets(
                        16_650, 56_001, 512));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateCompactLoadMaterializationBudgets(
                        16_650, 2_225, 4_097));
        var unverified = GTRecipeMapLoader.evaluatePublicationCapacity(
                32_425,
                15_445,
                32_425,
                50_652,
                876,
                338,
                0,
                0,
                0,
                0);
        assertTrue(unverified.unverifiedScale());
        assertTrue(unverified.warnings().stream().anyMatch(
                warning -> warning.startsWith("UNVERIFIED_SCALE:eager_published:")));
        assertEquals(
                7,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "mortar/ingot_to_dust/iron"))
                        .orElseThrow());
        assertEquals(
                8,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "pipe/extruder/plates_to_fluid_pipe/copper"))
                        .orElseThrow());
        assertEquals(
                10,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "ingot_form/anvil/ingot_to_double_ingot/copper"))
                        .orElseThrow());
        assertEquals(
                11,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "t" + "11" + "/future_rule/copper"))
                        .orElseThrow());
        assertEquals(
                12,
                GTRecipeMapLoader.authoredMaterialRuleStage(id(
                                "t" + "12" + "/future_rule/copper"))
                        .orElseThrow());
        assertTrue(GTRecipeMapLoader.authoredMaterialRuleStage(id(
                        "mortar/crushed_to_dust/copper"))
                .isEmpty());
        assertTrue(GTRecipeMapLoader.authoredMaterialRuleStage(
                        ResourceLocation.fromNamespaceAndPath(
                                "other", "mortar/ingot_to_dust/iron"))
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
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(
                        7,
                        ModProcessingMachines
                                .MORTAR_MATERIAL_RULE_BUDGET + 1));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(
                        8,
                        ModProcessingMachines.PIPE_MATERIAL_RULE_BUDGET + 1));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(10, 1_501));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(11, 1));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateAuthoredMaterialRuleBudget(12, 1));
    }

    @Test
    void componentToolAndLiveBudgetsFailIndependently() {
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateExpansionBudgets(
                ModProcessingMachines.COMPONENT_EXPANSION_BUDGET,
                ModProcessingMachines.LIVE_COMPONENT_MAP_RECIPE_BUDGET
                        - ModProcessingMachines.COMPONENT_EXPANSION_BUDGET,
                ModProcessingMachines.LIVE_COMPONENT_MAP_RECIPE_BUDGET));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateExpansionBudgets(
                        ModProcessingMachines.COMPONENT_EXPANSION_BUDGET + 1,
                        0,
                        ModProcessingMachines.COMPONENT_EXPANSION_BUDGET + 1));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateExpansionBudgets(
                        0,
                        ModProcessingMachines.TOOL_EXPANSION_BUDGET + 1,
                        ModProcessingMachines.TOOL_EXPANSION_BUDGET + 1));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validateExpansionBudgets(
                        ModProcessingMachines.COMPONENT_EXPANSION_BUDGET,
                        ModProcessingMachines.LIVE_COMPONENT_MAP_RECIPE_BUDGET
                                - ModProcessingMachines.COMPONENT_EXPANSION_BUDGET + 1,
                        ModProcessingMachines.LIVE_COMPONENT_MAP_RECIPE_BUDGET + 1));
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateExpansionBudgets(1, 1, 1));
    }

    @Test
    void releasePerformanceGatesIgnoreRecipeCount() {
        var metrics = new GTRecipeMapLoader.PublicationMetrics(
                0, 0, 0, 0, 0, 0, 0, 32_425,
                32_425, 50_862, 2_782, 557, 2_225, 512, 331_124L, 20,
                "a".repeat(64),
                338, 15_655, 15_445, 210, 128, 0, 1_024L, "",
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                ModProcessingMachines.VERIFICATION_RECIPE_RELOAD_BUDGET_MS,
                ModProcessingMachines.RECIPE_INDEX_BUILD_BUDGET_MS,
                GTRecipeMapLoader.PublicationControlMetrics.empty(),
                GTRecipeMapLoader.PublicationPhaseTimings.zero(),
                GTRecipeMapLoader.PublicationAllocationReport.pendingMeasurement());
        var lookup = new GTRecipeMapLoader.CompactLoadLookupMetrics(
                61,
                1_952,
                ModProcessingMachines.VERIFICATION_RECIPE_LOOKUP_P95_BUDGET_NS,
                ModProcessingMachines.RECIPE_LOOKUP_P95_CANDIDATE_BUDGET,
                ModProcessingMachines
                        .RECIPE_LOOKUP_MAX_CANDIDATE_HARD_CEILING);
        assertDoesNotThrow(() -> GTRecipeMapLoader.verifyReleasePerformance(
                metrics, lookup, null, null));
        var overReload = new GTRecipeMapLoader.PublicationMetrics(
                0, 0, 0, 0, 0, 0, 0, 32_425,
                32_425, 50_862, 2_782, 557, 2_225, 512, 331_124L, 20,
                "a".repeat(64),
                338, 15_655, 15_445, 210, 128, 0, 1_024L, "",
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                ModProcessingMachines.VERIFICATION_RECIPE_RELOAD_BUDGET_MS + 1,
                ModProcessingMachines.RECIPE_INDEX_BUILD_BUDGET_MS,
                GTRecipeMapLoader.PublicationControlMetrics.empty(),
                GTRecipeMapLoader.PublicationPhaseTimings.zero(),
                GTRecipeMapLoader.PublicationAllocationReport.pendingMeasurement());
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.verifyReleasePerformance(
                        overReload, lookup, null, null));
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
                ModProcessingMachines.CLIENT_RECIPE_INDEX_BUILD_BUDGET_MS,
                GTRecipeMapLoader.PublicationControlMetrics.empty(),
                GTRecipeMapLoader.PublicationPhaseTimings.zero(),
                GTRecipeMapLoader.PublicationAllocationReport.pendingMeasurement());
        var lookup = new GTRecipeMapLoader.CompactLoadLookupMetrics(
                61,
                1_952,
                ModProcessingMachines.RECIPE_LOOKUP_P95_BUDGET_NS,
                ModProcessingMachines.RECIPE_LOOKUP_P95_CANDIDATE_BUDGET,
                ModProcessingMachines
                        .RECIPE_LOOKUP_MAX_CANDIDATE_HARD_CEILING);
        assertTrue(GTRecipeMapLoader.evaluateCompactLoadOnlineBudgetGate(
                metrics, lookup).allPass());

        var overClientReload = new GTRecipeMapLoader.PublicationMetrics(
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 512, 331_124L, 20,
                "a".repeat(64),
                0, 0, 0, 0, 0, 0, 0L, "",
                ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                ModProcessingMachines.CLIENT_RECIPE_RELOAD_BUDGET_MS + 1,
                ModProcessingMachines.CLIENT_RECIPE_INDEX_BUILD_BUDGET_MS,
                GTRecipeMapLoader.PublicationControlMetrics.empty(),
                GTRecipeMapLoader.PublicationPhaseTimings.zero(),
                GTRecipeMapLoader.PublicationAllocationReport.pendingMeasurement());
        assertFalse(GTRecipeMapLoader.evaluateCompactLoadOnlineBudgetGate(
                overClientReload, lookup).sideReload());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
