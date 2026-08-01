package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

class RecipeMapSpecificityTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void prefersStrictInputSupersetWithoutUsingOrderForIdenticalSignatures() {
        Item plate = Items.IRON_INGOT;
        Item ring = Items.GOLD_NUGGET;
        Item gear = Items.IRON_NUGGET;
        Item rotor = Items.GOLD_INGOT;
        RecipeMap map = new RecipeMap(id("specificity"));
        map.replaceRecipes(List.of(
                new RecipeMap.Entry(id("gear"), recipe(
                        List.of(Ingredient.of(plate)), List.of(4), gear)),
                new RecipeMap.Entry(id("rotor"), recipe(
                        List.of(Ingredient.of(plate), Ingredient.of(ring)),
                        List.of(4, 1), rotor))));

        assertEquals(id("rotor"), map.findMatch(
                GTRecipeQuery.items(new ItemStack(plate, 4), new ItemStack(ring)))
                .orElseThrow().id());
        assertEquals(id("gear"), map.findMatch(
                GTRecipeQuery.items(new ItemStack(plate, 4)))
                .orElseThrow().id());
    }

    @Test
    void prefersHigherCountForSameIngredientDomain() {
        Item plate = Items.DIAMOND;
        Item low = Items.COAL;
        Item high = Items.EMERALD;
        RecipeMap map = new RecipeMap(id("counts"));
        map.replaceRecipes(List.of(
                new RecipeMap.Entry(id("two"), recipe(
                        List.of(Ingredient.of(plate)), List.of(2), low)),
                new RecipeMap.Entry(id("four"), recipe(
                        List.of(Ingredient.of(plate)), List.of(4), high))));

        assertEquals(id("four"), map.findMatch(
                GTRecipeQuery.items(new ItemStack(plate, 4)))
                .orElseThrow().id());
    }

    @Test
    void stagedValidationRejectsShadowsAndEmptyRequiredMapsWithoutPublishing() {
        GTRecipe first = recipe(
                List.of(Ingredient.of(Items.IRON_INGOT)), List.of(1), Items.IRON_NUGGET);
        GTRecipe second = recipe(
                List.of(Ingredient.of(Items.IRON_INGOT)), List.of(1), Items.GOLD_NUGGET);
        RecipeMap isolated = new RecipeMap(id("shadow_validation"));
        long revision = isolated.revision();
        var shadow = assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateNoShadows(isolated, List.of(
                        new RecipeMap.Entry(id("first"), first),
                        new RecipeMap.Entry(id("second"), second))));
        assertTrue(shadow.getMessage().contains(isolated.id().toString()));
        assertTrue(shadow.getMessage().contains("test:first"));
        assertTrue(shadow.getMessage().contains("data/test/recipe/first.json"));
        assertTrue(shadow.getMessage().contains("test:second"));
        assertTrue(shadow.getMessage().contains("data/test/recipe/second.json"));
        assertTrue(shadow.getMessage().contains("signature="));
        assertTrue(shadow.getMessage().contains("input or specificity"));
        assertEquals(revision, isolated.revision());
        assertEquals(List.of(), isolated.entries());

        LinkedHashMap<RecipeMap, List<RecipeMap.Entry>> candidates = new LinkedHashMap<>();
        for (RecipeMap map : ModRecipeMaps.ALL) {
            candidates.put(map, List.of(new RecipeMap.Entry(id(map.id().getPath()), first)));
        }
        candidates.put(ModRecipeMaps.CRUSHER, List.of());
        long crusherRevision = ModRecipeMaps.CRUSHER.revision();
        assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateRequiredMaps(candidates));
        assertEquals(crusherRevision, ModRecipeMaps.CRUSHER.revision());
    }

    private static GTRecipe recipe(
            List<Ingredient> inputs, List<Integer> counts, Item output) {
        return new GTRecipe(
                inputs,
                counts,
                List.of(new ItemStack(output)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                1,
                0,
                true);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
