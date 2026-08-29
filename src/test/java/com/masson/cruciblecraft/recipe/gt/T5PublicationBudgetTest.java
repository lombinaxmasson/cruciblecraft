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

class T5PublicationBudgetTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void explicitRecipePrefixClassifiesT5AcrossReusedMaps() {
        assertTrue(GTRecipeMapLoader.isT5ChemicalRecipe(id(
                "t5/bath/wash_precipitate")));
        assertTrue(GTRecipeMapLoader.isT5ChemicalRecipe(id(
                "t5/centrifuge/separate_solution")));
        assertFalse(GTRecipeMapLoader.isT5ChemicalRecipe(id(
                "ore_chain/centrifuge/copper")));
        assertFalse(GTRecipeMapLoader.isT5ChemicalRecipe(
                ResourceLocation.fromNamespaceAndPath(
                        "other", "t5/centrifuge/separate_solution")));

        assertDoesNotThrow(() -> GTRecipeMapLoader.validateT5RecipeProvenance(
                ModRecipeMaps.BATH,
                List.of(entry("t5/bath/wash_precipitate"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateT5RecipeProvenance(
                ModRecipeMaps.ELECTROLYZER,
                List.of(entry("t5/electrolyzer/split_solution"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateT5RecipeProvenance(
                ModRecipeMaps.ROASTER,
                List.of(
                        entry("t36/roaster/coal_dust_bootstrap"),
                        entry("t38/097d1d1ad1adb809"))));
        assertTrue(GTRecipeMapLoader.isT38CompactRecipe(id("t38/097d1d1ad1adb809")));
        assertFalse(GTRecipeMapLoader.isT38CompactRecipe(id("t36/roaster/coal_dust_bootstrap")));
        assertTrue(GTRecipeMapLoader.isT39CompactRecipe(id("t39/7b613367b7d4fc7a")));
        assertTrue(GTRecipeMapLoader.isT39CompactRecipe(
                id("t39_player_path_support/mixer_aluminium_fluoride")));
        assertFalse(GTRecipeMapLoader.isT39CompactRecipe(
                id("t39_player_path_recovery/mixer_aluminium_fluoride")));
        assertFalse(GTRecipeMapLoader.isT39CompactRecipe(id("t5/centrifuge/fluid_closure_glue_and_latex")));
        assertTrue(GTRecipeMapLoader.isT40CompactRecipe(id("t40/ae9f63194c927036")));
        assertTrue(GTRecipeMapLoader.isT40CompactRecipe(
                id("t40_player_path_support/unused")));
        assertFalse(GTRecipeMapLoader.isT40CompactRecipe(id("t40_catalog/electrolyzer/gt_recipe_electrolyzer_0000")));
        assertFalse(GTRecipeMapLoader.isT40CompactRecipe(
                id("t5/electrolyzer/split_solution")));
        assertTrue(GTRecipeMapLoader.isT41CompactRecipe(id("t41/a5d684f67018b8a3")));
        assertTrue(GTRecipeMapLoader.isT41CompactRecipe(
                id("t41_player_path_support/unused")));
        assertFalse(GTRecipeMapLoader.isT41CompactRecipe(
                id("t41_catalog/assembler/gt_recipe_assembler_0000")));
        assertFalse(GTRecipeMapLoader.isT41CompactRecipe(id("t37/a5d684f67018b8a3")));
        assertTrue(GTRecipeMapLoader.isT43CompactRecipe(id("t43/2d256883018190e2")));
        assertFalse(GTRecipeMapLoader.isT43CompactRecipe(id("t45/04a5fce0ce479f65")));
        assertTrue(GTRecipeMapLoader.isT45CompactRecipe(id("t45/04a5fce0ce479f65")));
        assertFalse(GTRecipeMapLoader.isT45CompactRecipe(id("t43/2d256883018190e2")));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateT5RecipeProvenance(
                ModRecipeMaps.ELECTROLYZER,
                List.of(entry("t40/ae9f63194c927036"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateT5RecipeProvenance(
                ModRecipeMaps.ASSEMBLER,
                List.of(entry("t41/a5d684f67018b8a3"))));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateT5RecipeProvenance(
                ModRecipeMaps.DRYING,
                List.of(entry("t45/04a5fce0ce479f65"))));
    }

    @Test
    void t39CompactSupersedesEquivalentT5StandaloneBeforeShadowCheck() {
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
                        id("t5/centrifuge/fluid_closure_glue_and_latex"),
                        shared),
                new RecipeMap.Entry(id("t39/7b613367b7d4fc7a"), shared)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.CENTRIFUGE.id(),
                complete,
                List.of(DedupRuleFixtures.t39T5PostEnumeration()));
        assertEquals(1, complete.size());
        assertEquals(id("t39/7b613367b7d4fc7a"), complete.get(0).id());
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateNoShadows(
                ModRecipeMaps.CENTRIFUGE, complete));
    }

    @Test
    void differentOutputsDoNotSupersedeT5AndStillFailShadowCheck() {
        GTRecipe t5Recipe = new GTRecipe(
                List.of(Ingredient.of(Items.SLIME_BALL)),
                List.of(1),
                List.of(new ItemStack(Items.SLIME_BALL)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        GTRecipe t39Recipe = new GTRecipe(
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
                        id("t5/centrifuge/fluid_closure_glue_and_latex"),
                        t5Recipe),
                new RecipeMap.Entry(id("t39/7b613367b7d4fc7a"), t39Recipe)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.CENTRIFUGE.id(),
                complete,
                List.of(DedupRuleFixtures.t39T5PostEnumeration()));
        assertEquals(2, complete.size());
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateNoShadows(ModRecipeMaps.CENTRIFUGE, complete));
    }

    @Test
    void t40CompactSupersedesEquivalentT5StandaloneBeforeShadowCheck() {
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
                        id("t5/electrolyzer/split_solution"),
                        shared),
                new RecipeMap.Entry(id("t40/ae9f63194c927036"), shared)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.ELECTROLYZER.id(),
                complete,
                List.of(DedupRuleFixtures.t40T5PostEnumeration()));
        assertEquals(1, complete.size());
        assertEquals(id("t40/ae9f63194c927036"), complete.get(0).id());
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateNoShadows(
                ModRecipeMaps.ELECTROLYZER, complete));
    }

    @Test
    void t41CompactDropsEquivalentT37LeftoverBeforeShadowCheck() {
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
                new RecipeMap.Entry(id("t37/a755393c9b55194f"), shared),
                new RecipeMap.Entry(id("t41/08b1a5b9f5140687"), shared)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.ASSEMBLER.id(),
                complete,
                List.of(DedupRuleFixtures.t37T41PostEnumeration()));
        assertEquals(1, complete.size());
        assertEquals(id("t37/a755393c9b55194f"), complete.get(0).id());
        assertDoesNotThrow(() -> GTRecipeMapLoader.validateNoShadows(
                ModRecipeMaps.ASSEMBLER, complete));
    }

    @Test
    void t41AssemblerSourcesDropEquivalentLeftoverButKeepUniqueRows() {
        CompactGTRecipeFamilyDefinition.Relation historical = assemblerRelation(
                "t37/a755393c9b55194f",
                Ingredient.of(Items.ACACIA_PLANKS),
                new ItemStack(Items.BOOKSHELF));
        CompactGTRecipeFamilyDefinition.Relation leftover = assemblerRelation(
                "t41/08b1a5b9f5140687",
                Ingredient.of(Items.ACACIA_PLANKS),
                new ItemStack(Items.BOOKSHELF));
        CompactGTRecipeFamilyDefinition.Relation unique = assemblerRelation(
                "t41/unique_blue_spruce",
                Ingredient.of(Items.SPRUCE_PLANKS),
                new ItemStack(Items.CHEST));
        CompactRecipeFamilySource t37 = new CompactRecipeFamilySource(
                id("t37/assembler/gt_recipe_assembler_0043"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0043",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(historical)));
        CompactRecipeFamilySource t41Dup = new CompactRecipeFamilySource(
                id("t41/assembler/gt_recipe_assembler_0331"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0331",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(leftover),
                        CompactGTRecipeFamilyDefinition
                                .T41_ASSEMBLER_PLANKS2_PUBLICATION_GROUP));
        CompactRecipeFamilySource t41Unique = new CompactRecipeFamilySource(
                id("t41/assembler/gt_recipe_assembler_0290"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0290",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(unique),
                        CompactGTRecipeFamilyDefinition
                                .T41_ASSEMBLER_PLANKS2_PUBLICATION_GROUP));
        List<CompactRecipeFamilySource> kept =
                CompactRecipeDeduplicator.applyPreSnapshot(
                        List.of(t37, t41Dup, t41Unique),
                        List.of(DedupRuleFixtures.t37T41PreSnapshot()));
        assertEquals(2, kept.size());
        assertEquals(
                List.of(t37.id(), t41Unique.id()),
                kept.stream().map(CompactRecipeFamilySource::id).toList());
    }

    @Test
    void t41AssemblerSourcesDropEquivalentComponentIngredients() {
        ItemStack named = new ItemStack(Items.PAPER);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("circuit-1"));
        CompactGTRecipeFamilyDefinition.Relation historical = assemblerRelation(
                "t37/circuit_oak_button",
                DataComponentIngredient.of(false, named),
                new ItemStack(Items.OAK_BUTTON));
        CompactGTRecipeFamilyDefinition.Relation leftover = assemblerRelation(
                "t41/circuit_oak_button",
                DataComponentIngredient.of(false, named.copy()),
                new ItemStack(Items.OAK_BUTTON));
        CompactRecipeFamilySource t37 = new CompactRecipeFamilySource(
                id("t37/assembler/gt_recipe_assembler_0002"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0002",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(historical)));
        CompactRecipeFamilySource t41Dup = new CompactRecipeFamilySource(
                id("t41/assembler/gt_recipe_assembler_0290"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.assembler#0290",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(leftover),
                        CompactGTRecipeFamilyDefinition
                                .T41_ASSEMBLER_PLANKS2_PUBLICATION_GROUP));
        List<CompactRecipeFamilySource> kept =
                CompactRecipeDeduplicator.applyPreSnapshot(
                        List.of(t37, t41Dup),
                        List.of(DedupRuleFixtures.t37T41PreSnapshot()));
        assertEquals(List.of(t37.id()), kept.stream().map(CompactRecipeFamilySource::id).toList());
    }

    @Test
    void differentElectrolyzerOutputsDoNotSupersedeT5AndStillFailShadowCheck() {
        GTRecipe t5Recipe = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.GLOWSTONE_DUST)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                64,
                16L,
                0L);
        GTRecipe t40Recipe = new GTRecipe(
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
                        id("t5/electrolyzer/split_solution"),
                        t5Recipe),
                new RecipeMap.Entry(id("t40/ae9f63194c927036"), t40Recipe)));
        CompactRecipeDeduplicator.applyPostEnumeration(
                ModRecipeMaps.ELECTROLYZER.id(),
                complete,
                List.of(DedupRuleFixtures.t40T5PostEnumeration()));
        assertEquals(2, complete.size());
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateNoShadows(ModRecipeMaps.ELECTROLYZER, complete));
    }

    @Test
    void provenanceCannotBypassDedicatedOrAllowedMapGates() {
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateT5RecipeProvenance(
                        ModRecipeMaps.CRUSHER,
                        List.of(entry("t5/crusher/not_allowed"))));
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateT5RecipeProvenance(
                        ModRecipeMaps.ELECTROLYZER,
                        List.of(entry("legacy/unclassified"))));
    }

    @Test
    void loaderValidationMatchesMappedSpecsIncludingTheT3AssemblerPolicy() {
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
                "t5/assembler/phosphor",
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
                "t5/assembler/invalid_catalyst",
                ModRecipeMaps.ASSEMBLER,
                invalidAssemblerCatalyst);

        GTRecipe invalidT5Amount = new GTRecipe(
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
                "t5/compressor/invalid_chance",
                ModRecipeMaps.COMPRESSOR,
                invalidT5Amount);
    }

    @Test
    void frozenBudgetsRemainAndT5AndGlobalBudgetsFailIndependently() {
        assertEquals(10_000, ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET);
        assertEquals(4_000, ModProcessingMachines.T4_TOOL_EXPANSION_BUDGET);
        assertEquals(13_000, ModProcessingMachines.LIVE_T3_MAP_RECIPE_BUDGET);

        assertDoesNotThrow(() -> GTRecipeMapLoader.validatePublicationBudgets(
                10_000,
                3_000,
                13_000,
                ModProcessingMachines.T5_CHEMICAL_RECIPE_BUDGET,
                ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET));
        assertDoesNotThrow(() -> GTRecipeMapLoader.validatePublicationBudgets(
                8_136,
                3_452,
                11_589,
                145,
                1,
                16_957));
        assertThrows(IllegalStateException.class, () ->
                GTRecipeMapLoader.validatePublicationBudgets(
                        0,
                        0,
                        0,
                        ModProcessingMachines.T5_CHEMICAL_RECIPE_BUDGET + 1,
                        ModProcessingMachines.T5_CHEMICAL_RECIPE_BUDGET + 1));
        assertThrows(IllegalStateException.class, () ->
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
