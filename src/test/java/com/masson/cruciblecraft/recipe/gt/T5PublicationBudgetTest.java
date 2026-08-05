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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

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
