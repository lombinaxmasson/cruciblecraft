package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.recipe.gt.CompactWaveRecipeIds;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class ChemicalPublicationBudgetTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void explicitRecipePrefixClassifiesChemicalAcrossReusedMaps() {
        assertTrue(GTRecipeMapLoader.isAuthoredChemicalRecipe(id(
                "chemical/bath/wash_precipitate")));
        assertTrue(GTRecipeMapLoader.isAuthoredChemicalRecipe(id(
                "chemical/centrifuge/separate_solution")));
        assertFalse(GTRecipeMapLoader.isAuthoredChemicalRecipe(id(
                "ore_chain/centrifuge/copper")));
        assertFalse(GTRecipeMapLoader.isAuthoredChemicalRecipe(
                ResourceLocation.fromNamespaceAndPath(
                        "other", "chemical/centrifuge/separate_solution")));

        assertDoesNotThrow(() -> GTRecipeMapLoader.validateChemicalRecipeProvenance(
                ModRecipeMaps.BATH,
                List.of(entry("chemical/bath/wash_precipitate"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateChemicalRecipeProvenance(
                ModRecipeMaps.ELECTROLYZER,
                List.of(entry("chemical/electrolyzer/split_solution"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateChemicalRecipeProvenance(
                ModRecipeMaps.ROASTER,
                List.of(
                        entry("machine/bootstrap/roaster/coal_dust_bootstrap"),
                        entry("roaster/compact/097d1d1ad1adb809"))));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("assembler/compact/a5d684f67018b8a3")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("roaster/compact/097d1d1ad1adb809")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("centrifuge/compact/7b613367b7d4fc7a")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(
                id("player_path_support/centrifuge/mixer_aluminium_fluoride")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("electrolyzer/compact/ae9f63194c927036")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(
                id("player_path_support/electrolyzer/unused")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("assembler/wood/a5d684f67018b8a3")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(
                id("player_path_support/assembler_wood/unused")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("smelter/stone/2d256883018190e2")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("smelter/block/04a5fce0ce479f65")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("drying/block/04a5fce0ce479f65")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("bath/mte/023952e1e28f28ca")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("bath/remainder/0123456789abcdef")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(
                id("player_path_support/bath_remainder/cruciblecraft_rainbow_sap")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("bath/identity/229324c3dd9ec9af")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(
                id("player_path_support/bath_identity/cruciblecraft_frying_oil_hot")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(id("bath/tiny_purified/1a8b858f5b602a9c")));
        assertTrue(CompactWaveRecipeIds.isCompactHostRecipe(
                id("player_path_support/bath_tiny_purified/unused")));
        assertFalse(CompactWaveRecipeIds.isCompactHostRecipe(id("machine/bootstrap/roaster/coal_dust_bootstrap")));
        assertFalse(CompactWaveRecipeIds.isCompactHostRecipe(
                id("player_path_recovery/roaster/mixer_aluminium_fluoride")));
        assertFalse(CompactWaveRecipeIds.isCompactHostRecipe(id("chemical/centrifuge/fluid_closure_glue_and_latex")));
        assertFalse(CompactWaveRecipeIds.isCompactHostRecipe(
                id("chemical/electrolyzer/split_solution")));
        assertFalse(CompactWaveRecipeIds.isCompactHostRecipe(
                id("t40_catalog/electrolyzer/gt_recipe_electrolyzer_0000")));
        assertFalse(CompactWaveRecipeIds.isCompactHostRecipe(
                id("t41_catalog/assembler/gt_recipe_assembler_0000")));
        assertTrue(CompactWaveRecipeIds.isRoasterRecoveryRecipe(
                id("player_path_recovery/roaster/mixer_aluminium_fluoride")));
        assertTrue(CompactWaveRecipeIds.isSemanticWaveRecipe(
                id("smelter/ordinary_closure/singleton/example")));
        assertFalse(CompactWaveRecipeIds.isSemanticWaveRecipe(id("unassigned/mixer/example")));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateChemicalRecipeProvenance(
                ModRecipeMaps.ELECTROLYZER,
                List.of(entry("electrolyzer/compact/ae9f63194c927036"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateChemicalRecipeProvenance(
                ModRecipeMaps.ASSEMBLER,
                List.of(entry("assembler/wood/a5d684f67018b8a3"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateChemicalRecipeProvenance(
                ModRecipeMaps.DRYING,
                List.of(entry("smelter/block/04a5fce0ce479f65"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateChemicalRecipeProvenance(
                ModRecipeMaps.BATH,
                List.of(entry("bath/mte/023952e1e28f28ca"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateChemicalRecipeProvenance(
                ModRecipeMaps.BATH,
                List.of(entry("bath/identity/229324c3dd9ec9af"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateChemicalRecipeProvenance(
                ModRecipeMaps.BATH,
                List.of(entry("bath/tiny_purified/1a8b858f5b602a9c"))));
        GTRecipe twoFluidOut = new GTRecipe(
                List.of(Ingredient.of(Items.IRON_INGOT)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(new net.neoforged.neoforge.fluids.FluidStack(
                        net.minecraft.world.level.material.Fluids.WATER, 7000)),
                List.of(
                        new net.neoforged.neoforge.fluids.FluidStack(
                                net.minecraft.world.level.material.Fluids.WATER, 3000),
                        new net.neoforged.neoforge.fluids.FluidStack(
                                net.minecraft.world.level.material.Fluids.LAVA, 3000)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                512,
                0L,
                0L);
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateTarget(
                id("bath/remainder/2bb2aaf581884b1a"),
                ModRecipeMaps.BATH,
                twoFluidOut));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateTarget(
                id("bath/identity/229324c3dd9ec9af"),
                ModRecipeMaps.BATH,
                twoFluidOut));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateTarget(
                id("bath/tiny_purified/1a8b858f5b602a9c"),
                ModRecipeMaps.BATH,
                twoFluidOut));
        GTRecipe oversizedMolten = new GTRecipe(
                List.of(Ingredient.of(Items.IRON_INGOT)),
                List.of(1),
                List.of(),
                List.of(),
                List.of(new net.neoforged.neoforge.fluids.FluidStack(
                        net.minecraft.world.level.material.Fluids.LAVA, 13032)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        assertTrue(CompactWaveRecipeIds.isSmelterDeferredRecyclingRecipe(
                id("smelter/deferred_recycling/steel/gt_recipe_smelter_0001")));
        assertTrue(CompactWaveRecipeIds.isSemanticWaveRecipe(
                id("smelter/deferred_recycling/steel/gt_recipe_smelter_0001")));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateTarget(
                id("smelter/deferred_recycling/steel/gt_recipe_smelter_0001"),
                ModRecipeMaps.SMELTER,
                oversizedMolten));
        IllegalArgumentException smelterAmount = assertThrows(
                IllegalArgumentException.class,
                () -> GTRecipeMapLoader.validateTarget(
                        id("smelter/ordinary_closure/steel/example"),
                        ModRecipeMaps.SMELTER,
                        oversizedMolten));
        assertTrue(
                smelterAmount.getMessage().contains("chemical_recipe_amount"),
                smelterAmount.getMessage());
        IllegalArgumentException chemicalShape = assertThrows(
                IllegalArgumentException.class,
                () -> GTRecipeMapLoader.validateTarget(
                        id("chemical/bath/over_shape"),
                        ModRecipeMaps.BATH,
                        twoFluidOut));
        assertTrue(chemicalShape.getMessage().contains("chemical_recipe_shape"), chemicalShape.getMessage());
    }

    @Test
    void centrifugeCompactSupersedesEquivalentChemicalStandaloneBeforeShadowCheck() {
        GTRecipe shared = new GTRecipe(
                List.of(Ingredient.of(Items.SLIME_BALL)),
                List.of(1),
                List.of(new ItemStack(Items.SLIME_BALL)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        java.util.ArrayList<RecipeMap.Entry> complete = new java.util.ArrayList<>(List.of(
                new RecipeMap.Entry(
                        id("chemical/centrifuge/fluid_closure_glue_and_latex"),
                        shared),
                new RecipeMap.Entry(id("centrifuge/compact/7b613367b7d4fc7a"), shared)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.CENTRIFUGE.id(),
                complete,
                List.of(DedupRuleFixtures.centrifugeChemicalPostEnumeration()));
        assertEquals(1, complete.size());
        assertEquals(id("centrifuge/compact/7b613367b7d4fc7a"), complete.get(0).id());
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateNoShadows(
                ModRecipeMaps.CENTRIFUGE, complete));
    }

    @Test
    void differentOutputsDoNotSupersedeChemicalAndStillFailShadowCheck() {
        GTRecipe chemicalStandaloneRecipe = new GTRecipe(
                List.of(Ingredient.of(Items.SLIME_BALL)),
                List.of(1),
                List.of(new ItemStack(Items.SLIME_BALL)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        GTRecipe centrifugeCompactRecipe = new GTRecipe(
                List.of(Ingredient.of(Items.SLIME_BALL)),
                List.of(1),
                List.of(new ItemStack(Items.BONE_MEAL)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        java.util.ArrayList<RecipeMap.Entry> complete = new java.util.ArrayList<>(List.of(
                new RecipeMap.Entry(
                        id("chemical/centrifuge/fluid_closure_glue_and_latex"),
                        chemicalStandaloneRecipe),
                new RecipeMap.Entry(id("centrifuge/compact/7b613367b7d4fc7a"), centrifugeCompactRecipe)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.CENTRIFUGE.id(),
                complete,
                List.of(DedupRuleFixtures.centrifugeChemicalPostEnumeration()));
        assertEquals(2, complete.size());
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateNoShadows(ModRecipeMaps.CENTRIFUGE, complete));
    }

    @Test
    void electrolyzerCompactSupersedesEquivalentChemicalStandaloneBeforeShadowCheck() {
        GTRecipe shared = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.GLOWSTONE_DUST)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        java.util.ArrayList<RecipeMap.Entry> complete = new java.util.ArrayList<>(List.of(
                new RecipeMap.Entry(
                        id("chemical/electrolyzer/split_solution"),
                        shared),
                new RecipeMap.Entry(id("electrolyzer/compact/ae9f63194c927036"), shared)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.ELECTROLYZER.id(),
                complete,
                List.of(DedupRuleFixtures.electrolyzerChemicalPostEnumeration()));
        assertEquals(1, complete.size());
        assertEquals(id("electrolyzer/compact/ae9f63194c927036"), complete.get(0).id());
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateNoShadows(
                ModRecipeMaps.ELECTROLYZER, complete));
    }

    @Test
    void assemblerWoodDropsEquivalentAssemblerLeftoverBeforeShadowCheck() {
        GTRecipe shared = new GTRecipe(
                List.of(Ingredient.of(Items.ACACIA_PLANKS), Ingredient.of(Items.BOOK)),
                List.of(6, 3),
                List.of(new ItemStack(Items.BOOKSHELF)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        java.util.ArrayList<RecipeMap.Entry> complete = new java.util.ArrayList<>(List.of(
                new RecipeMap.Entry(id("assembler/compact/a755393c9b55194f"), shared),
                new RecipeMap.Entry(id("assembler/wood/08b1a5b9f5140687"), shared)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.ASSEMBLER.id(),
                complete,
                List.of(DedupRuleFixtures.assemblerCompactWoodPostEnumeration()));
        assertEquals(1, complete.size());
        assertEquals(id("assembler/compact/a755393c9b55194f"), complete.get(0).id());
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateNoShadows(
                ModRecipeMaps.ASSEMBLER, complete));
    }

    @Test
    void assemblerWoodSourcesDropEquivalentLeftoverButKeepUniqueRows() {
        CompactGTRecipeFamilyDefinition.Relation historical = assemblerRelation(
                "assembler/compact/a755393c9b55194f",
                Ingredient.of(Items.ACACIA_PLANKS),
                new ItemStack(Items.BOOKSHELF));
        CompactGTRecipeFamilyDefinition.Relation leftover = assemblerRelation(
                "assembler/wood/08b1a5b9f5140687",
                Ingredient.of(Items.ACACIA_PLANKS),
                new ItemStack(Items.BOOKSHELF));
        CompactGTRecipeFamilyDefinition.Relation unique = assemblerRelation(
                "assembler/wood/unique_blue_spruce",
                Ingredient.of(Items.SPRUCE_PLANKS),
                new ItemStack(Items.CHEST));
        CompactRecipeFamilySource assemblerCompact = new CompactRecipeFamilySource(
                id("assembler/compact/gt_recipe_assembler_0043"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0043",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(historical)));
        CompactRecipeFamilySource woodDup = new CompactRecipeFamilySource(
                id("assembler/wood/gt_recipe_assembler_0331"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0331",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(leftover),
                        CompactPublicationGroups.ASSEMBLER_PLANKS2));
        CompactRecipeFamilySource woodUnique = new CompactRecipeFamilySource(
                id("assembler/wood/gt_recipe_assembler_0290"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0290",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(unique),
                        CompactPublicationGroups.ASSEMBLER_PLANKS2));
        List<CompactRecipeFamilySource> kept =
                CompactRecipeDeduplicator.applyPreSnapshot(
                        List.of(assemblerCompact, woodDup, woodUnique),
                        List.of(DedupRuleFixtures.assemblerCompactWoodPreSnapshot()));
        assertEquals(2, kept.size());
        assertEquals(
                List.of(assemblerCompact.id(), woodUnique.id()),
                kept.stream().map(CompactRecipeFamilySource::id).toList());
    }

    @Test
    void assemblerWoodSourcesDropEquivalentComponentIngredients() {
        ItemStack named = new ItemStack(Items.PAPER);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("circuit-1"));
        CompactGTRecipeFamilyDefinition.Relation historical = assemblerRelation(
                "assembler/compact/circuit_oak_button",
                DataComponentIngredient.of(false, named),
                new ItemStack(Items.OAK_BUTTON));
        CompactGTRecipeFamilyDefinition.Relation leftover = assemblerRelation(
                "assembler/wood/circuit_oak_button",
                DataComponentIngredient.of(false, named.copy()),
                new ItemStack(Items.OAK_BUTTON));
        CompactRecipeFamilySource assemblerCompact = new CompactRecipeFamilySource(
                id("assembler/compact/gt_recipe_assembler_0002"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0002",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(historical)));
        CompactRecipeFamilySource woodDup = new CompactRecipeFamilySource(
                id("assembler/wood/gt_recipe_assembler_0290"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0290",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(leftover),
                        CompactPublicationGroups.ASSEMBLER_PLANKS2));
        List<CompactRecipeFamilySource> kept =
                CompactRecipeDeduplicator.applyPreSnapshot(
                        List.of(assemblerCompact, woodDup),
                        List.of(DedupRuleFixtures.assemblerCompactWoodPreSnapshot()));
        assertEquals(List.of(assemblerCompact.id()), kept.stream().map(CompactRecipeFamilySource::id).toList());
    }

    @Test
    void differentElectrolyzerOutputsDoNotSupersedeChemicalAndStillFailShadowCheck() {
        GTRecipe chemicalStandaloneRecipe = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.GLOWSTONE_DUST)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        GTRecipe electrolyzerCompactRecipe = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.GUNPOWDER)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        java.util.ArrayList<RecipeMap.Entry> complete = new java.util.ArrayList<>(List.of(
                new RecipeMap.Entry(
                        id("chemical/electrolyzer/split_solution"),
                        chemicalStandaloneRecipe),
                new RecipeMap.Entry(id("electrolyzer/compact/ae9f63194c927036"), electrolyzerCompactRecipe)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.ELECTROLYZER.id(),
                complete,
                List.of(DedupRuleFixtures.electrolyzerChemicalPostEnumeration()));
        assertEquals(2, complete.size());
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateNoShadows(ModRecipeMaps.ELECTROLYZER, complete));
    }

    @Test
    void provenanceCannotBypassDedicatedOrAllowedMapGates() {
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateChemicalRecipeProvenance(
                        ModRecipeMaps.CRUSHER,
                        List.of(entry("chemical/crusher/not_allowed"))));
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateChemicalRecipeProvenance(
                        ModRecipeMaps.ELECTROLYZER,
                        List.of(entry("legacy/unclassified"))));
    }

    @Test
    void loaderValidationMatchesMappedSpecsIncludingTheAssemblerPolicy() {
        GTRecipe assemblerProjection = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE), Ingredient.of(Items.COAL)),
                List.of(4, 1),
                List.of(new ItemStack(Items.IRON_NUGGET, 4)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        assertLoaderMatchesSpec(
                "chemical/assembler/phosphor",
                ModRecipeMaps.ASSEMBLER,
                assemblerProjection);

        GTRecipe invalidAssemblerCatalyst = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE), Ingredient.of(Items.COAL)),
                List.of(1, 0),
                List.of(ItemInputAction.CONSUME, ItemInputAction.PRESERVE),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                24L,
                0L,
                true,
                Optional.empty());
        assertLoaderMatchesSpec(
                "chemical/assembler/invalid_catalyst",
                ModRecipeMaps.ASSEMBLER,
                invalidAssemblerCatalyst);

        GTRecipe invalidChemicalAmount = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(),
                List.of(),
                List.of(0),
                20,
                24L,
                0L);
        assertLoaderMatchesSpec(
                "chemical/compressor/invalid_chance",
                ModRecipeMaps.COMPRESSOR,
                invalidChemicalAmount);
    }

    @Test
    void frozenBudgetsRemainAndChemicalAndGlobalBudgetsFailIndependently() {
        assertEquals(10_000, ModProcessingMachines.COMPONENT_EXPANSION_BUDGET);
        assertEquals(4_000, ModProcessingMachines.TOOL_EXPANSION_BUDGET);
        assertEquals(13_000, ModProcessingMachines.LIVE_COMPONENT_MAP_RECIPE_BUDGET);

        assertDoesNotThrow(() -> GTRecipeMapLoader.validatePublicationBudgets(
                10_000,
                3_000,
                13_000,
                ModProcessingMachines.CHEMICAL_RECIPE_BUDGET,
                ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validatePublicationBudgets(
                8_136,
                3_452,
                11_589,
                145,
                1,
                16_957));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validatePublicationBudgets(
                        0,
                        0,
                        0,
                        ModProcessingMachines.CHEMICAL_RECIPE_BUDGET + 1,
                        ModProcessingMachines.CHEMICAL_RECIPE_BUDGET + 1));
        assertDoesNotThrow(() ->
                GTRecipeMapLoader.validatePublicationBudgets(
                        0,
                        0,
                        0,
                        0,
                        ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET + 1));
    }

    private static CompactGTRecipeFamilyDefinition.Relation assemblerRelation(
            String path,
            Ingredient input,
            ItemStack output) {
        return new CompactGTRecipeFamilyDefinition.Relation(
                id(path),
                List.of(input),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(output),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L,
                true,
                0,
                new GTRecipeProvenance(
                        "SOURCE_BACKED",
                        Optional.of("gt.recipe.assembler#0002"),
                        List.of("abc")));
    }

    private static RecipeMap.Entry entry(String path) {
        return new RecipeMap.Entry(id(path), new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                24L,
                0L));
    }

    private static void assertLoaderMatchesSpec(
            String recipePath,
            RecipeMap map,
            GTRecipe recipe) {
        Optional<String> invalid = ModProcessingMachines
                .forRecipeMap(map.id())
                .orElseThrow()
                .validator()
                .validate(recipe);
        if (invalid.isEmpty()) {
            assertDoesNotThrow(() ->
                    GTRecipeMapLoader.validateTarget(id(recipePath), map, recipe));
            return;
        }
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> GTRecipeMapLoader.validateTarget(id(recipePath), map, recipe));
        assertTrue(
                failure.getMessage().contains("(" + invalid.get() + ")"),
                failure.getMessage());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
