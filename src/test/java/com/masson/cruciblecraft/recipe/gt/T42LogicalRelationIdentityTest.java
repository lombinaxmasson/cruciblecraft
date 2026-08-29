package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pins the T42 Python identity helper to the live
 * {@link GTRecipeMapLoader#logicalInputIdentity} /
 * {@link GTRecipeMapLoader#recipeOutputIdentity} contract. Matching is not
 * {@code Ingredient.toString()} and is not namespace equality.
 */
class T42LogicalRelationIdentityTest {
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindComponentIngredientType();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void logicalInputIgnoresIngredientWrapperAndKeepsCountActionAndComponents() {
        Ingredient plain = Ingredient.of(Items.STONE);
        Ingredient wrapped = DataComponentIngredient.of(
                true,
                new ItemStack(Items.STONE));
        GTRecipe first = recipe(plain, ItemInputAction.CONSUME, 2);
        GTRecipe second = recipe(wrapped, ItemInputAction.CONSUME, 2);

        String firstIdentity = GTRecipeMapLoader.logicalInputIdentity(first);
        String secondIdentity = GTRecipeMapLoader.logicalInputIdentity(second);
        assertEquals(firstIdentity, secondIdentity);
        assertTrue(firstIdentity.contains("minecraft:stone"));
        assertTrue(firstIdentity.contains("2:"));
        assertFalse(firstIdentity.contains(plain.getClass().getName()));
        assertNotEquals(
                GTRecipeMapLoader.inputSignature(first),
                firstIdentity);
    }

    @Test
    void preserveActionIsPartOfTheLogicalInputIdentity() {
        GTRecipe consume = new GTRecipe(
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.STONE)),
                List.of(1, 1),
                List.of(ItemInputAction.CONSUME, ItemInputAction.CONSUME),
                List.of(new ItemStack(Items.IRON_NUGGET, 1)),
                List.of(),
                List.of(),
                List.of(5_000),
                20,
                16,
                0,
                true,
                Optional.empty());
        GTRecipe preserve = new GTRecipe(
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.STONE)),
                List.of(1, 0),
                List.of(ItemInputAction.CONSUME, ItemInputAction.PRESERVE),
                List.of(new ItemStack(Items.IRON_NUGGET, 1)),
                List.of(),
                List.of(),
                List.of(5_000),
                20,
                16,
                0,
                true,
                Optional.empty());
        assertNotEquals(
                GTRecipeMapLoader.logicalInputIdentity(consume),
                GTRecipeMapLoader.logicalInputIdentity(preserve));
        assertTrue(GTRecipeMapLoader.logicalInputIdentity(preserve)
                .contains("PRESERVE"));
    }

    @Test
    void recipeOutputIdentityIncludesOutputsDurationAndEut() {
        GTRecipe recipe = new GTRecipe(
                List.of(Ingredient.of(Items.STONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_INGOT, 3)),
                List.of(),
                List.of(),
                List.of(10_000),
                40,
                32,
                7,
                true);
        String identity = GTRecipeMapLoader.recipeOutputIdentity(recipe);
        assertTrue(identity.contains("minecraft:iron_ingot"));
        assertTrue(identity.contains("3@"));
        assertTrue(identity.contains("|40|32"));
    }

    @Test
    void fluidComponentsJoinTheLogicalInputIdentity() {
        FluidStack water = new FluidStack(
                net.minecraft.world.level.material.Fluids.WATER,
                144);
        GTRecipe recipe = new GTRecipe(
                List.of(),
                List.of(),
                List.of(),
                List.of(water),
                List.of(new FluidStack(
                        net.minecraft.world.level.material.Fluids.WATER,
                        72)),
                List.of(),
                20,
                16,
                0,
                true);
        String identity = GTRecipeMapLoader.logicalInputIdentity(recipe);
        assertTrue(identity.contains("minecraft:water"));
        assertTrue(identity.contains("144@"));
        assertTrue(identity.contains("||"));
    }

    private static GTRecipe recipe(
            Ingredient input,
            ItemInputAction action,
            int count) {
        return new GTRecipe(
                List.of(input),
                List.of(count),
                List.of(action),
                List.of(new ItemStack(Items.IRON_INGOT, 1)),
                List.of(),
                List.of(),
                List.of(5_000),
                20,
                16,
                0,
                true,
                Optional.empty());
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
