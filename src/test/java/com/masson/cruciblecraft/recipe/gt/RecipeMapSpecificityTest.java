package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

class RecipeMapSpecificityTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindComponentIngredientType();
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
    void incomparableMaximalMatchesUseStableDeclarationOrder() {
        RecipeMap.Entry ironAndGold = new RecipeMap.Entry(
                id("iron_and_gold"),
                recipe(
                        List.of(
                                Ingredient.of(Items.IRON_INGOT),
                                Ingredient.of(Items.GOLD_INGOT)),
                        List.of(1, 1),
                        Items.IRON_NUGGET));
        RecipeMap.Entry ironAndDiamond = new RecipeMap.Entry(
                id("iron_and_diamond"),
                recipe(
                        List.of(
                                Ingredient.of(Items.IRON_INGOT),
                                Ingredient.of(Items.DIAMOND)),
                        List.of(2, 1),
                        Items.DIAMOND));
        GTRecipeQuery query = GTRecipeQuery.items(
                new ItemStack(Items.IRON_INGOT, 2),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.DIAMOND));

        for (List<RecipeMap.Entry> order : List.of(
                List.of(ironAndGold, ironAndDiamond),
                List.of(ironAndDiamond, ironAndGold))) {
            RecipeMap map = new RecipeMap(id("incomparable"));
            map.replaceRecipes(order);
            assertEquals(
                    order.getFirst().id(),
                    map.findMatch(query).orElseThrow().id());
        }
    }

    @Test
    void disjointRecipesCanCoexistInOneMachineInputQuery() {
        RecipeMap map = new RecipeMap(id("disjoint"));
        map.replaceRecipes(List.of(
                new RecipeMap.Entry(id("ingot"), recipe(
                        List.of(Ingredient.of(Items.COPPER_INGOT)),
                        List.of(1),
                        Items.IRON_NUGGET)),
                new RecipeMap.Entry(id("foil"), recipe(
                        List.of(Ingredient.of(Items.PAPER)),
                        List.of(1),
                        Items.STRING))));

        assertEquals(
                id("ingot"),
                map.findMatch(GTRecipeQuery.items(
                        new ItemStack(Items.COPPER_INGOT),
                        new ItemStack(Items.PAPER)))
                        .orElseThrow()
                        .id());
    }

    @Test
    void reusableCatalystsDoNotFloodPrimaryCandidateIndex() {
        RecipeMap map = new RecipeMap(id("catalyst_candidates"));
        map.replaceRecipes(List.of(
                new RecipeMap.Entry(id("iron"), catalystRecipe(
                        Items.IRON_INGOT, Items.STICK, Items.IRON_NUGGET)),
                new RecipeMap.Entry(id("gold"), catalystRecipe(
                        Items.GOLD_INGOT, Items.STICK, Items.GOLD_NUGGET))));

        GTRecipeQuery query = GTRecipeQuery.items(
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.STICK));
        assertEquals(1, map.indexedCandidateCount(query));
        assertEquals(id("iron"), map.findMatch(query).orElseThrow().id());
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

        var duplicateId = assertThrows(IllegalArgumentException.class, () ->
                GTRecipeMapLoader.validateUniqueRecipeIds(isolated, List.of(
                        new RecipeMap.Entry(id("same"), first),
                        new RecipeMap.Entry(id("same"), second))));
        assertTrue(duplicateId.getMessage().contains("Duplicate stable recipe id"));
        assertTrue(duplicateId.getMessage().contains("data/test/recipe/same.json"));
        assertTrue(duplicateId.getMessage().contains("before epoch publication"));
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

    @Test
    void nonWhitelistedComponentIngredientsRemainExplicitlyUnindexed() {
        ItemStack namedInput = new ItemStack(Items.IRON_INGOT);
        namedInput.set(DataComponents.CUSTOM_NAME, Component.literal("indexed-by-name"));
        RecipeMap map = new RecipeMap(id("component_index_guard"));
        RecipeMap.Prepared prepared = map.prepareRecipes(List.of(new RecipeMap.Entry(
                id("component_recipe"),
                recipe(
                        List.of(DataComponentIngredient.of(false, namedInput)),
                        List.of(1),
                        Items.IRON_NUGGET))));

        assertEquals(1, prepared.unindexedRecipeCount());
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> GTRecipeMapLoader.validateNoUnindexed(
                        Map.of(map, prepared)));
        assertTrue(failure.getMessage().contains(map.id().toString()));
        assertTrue(failure.getMessage().contains("1 unindexed"));
        RecipeMap wrongOwner = new RecipeMap(id("wrong_owner"));
        IllegalArgumentException ownerFailure = assertThrows(
                IllegalArgumentException.class,
                () -> GTRecipeMapLoader.validateNoUnindexed(
                        Map.of(wrongOwner, prepared)));
        assertTrue(ownerFailure.getMessage().contains("owner"));
        assertTrue(ownerFailure.getMessage().contains(wrongOwner.id().toString()));
        assertEquals(List.of(), map.entries());
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

    private static GTRecipe catalystRecipe(
            Item material,
            Item catalyst,
            Item output) {
        return new GTRecipe(
                List.of(Ingredient.of(material), Ingredient.of(catalyst)),
                List.of(1, 0),
                List.of(ItemInputAction.CONSUME, ItemInputAction.wear(1)),
                List.of(new ItemStack(output)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                1,
                0,
                true,
                java.util.Optional.empty());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    private static void bindComponentIngredientType() {
        try {
            if (!NeoForgeRegistries.INGREDIENT_TYPES.containsKey(
                    NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE.getId())) {
                Registry.register(
                        NeoForgeRegistries.INGREDIENT_TYPES,
                        NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE.getId(),
                        new IngredientType<>(DataComponentIngredient.CODEC));
            }
            var holder = DeferredHolder.class.getDeclaredField("holder");
            holder.setAccessible(true);
            holder.set(
                    NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE,
                    NeoForgeRegistries.INGREDIENT_TYPES.getHolderOrThrow(
                            NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE.getKey()));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to install test ingredient type", exception);
        }
    }
}
