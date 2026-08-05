package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RecipeResourceMatcherTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void overlappingIngredientsUseAFeasibleOrderIndependentAllocation() {
        List<Ingredient> requirements = List.of(
                Ingredient.of(Items.IRON_INGOT, Items.GOLD_INGOT),
                Ingredient.of(Items.IRON_INGOT));

        assertTrue(RecipeResourceMatcher.matchesItems(
                requirements,
                List.of(1, 1),
                List.of(
                        new ItemStack(Items.IRON_INGOT),
                        new ItemStack(Items.GOLD_INGOT))));
        assertFalse(RecipeResourceMatcher.matchesItems(
                requirements,
                List.of(1, 1),
                List.of(new ItemStack(Items.IRON_INGOT))));
    }

    @Test
    void presenceOnlyIngredientReservesOneCompatibleSupplyUnit() {
        assertTrue(RecipeResourceMatcher.matchesItems(
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_INGOT)),
                List.of(0, 2),
                List.of(new ItemStack(Items.IRON_INGOT, 3))));
        assertFalse(RecipeResourceMatcher.matchesItems(
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_INGOT)),
                List.of(0, 2),
                List.of(new ItemStack(Items.IRON_INGOT, 2))));
        assertFalse(RecipeResourceMatcher.matchesItems(
                List.of(Ingredient.of(Items.IRON_INGOT)),
                List.of(0),
                List.of(ItemStack.EMPTY)));
    }

    @Test
    void emptyStacksNeverBecomeCompatibleSupplies() {
        assertFalse(RecipeResourceMatcher.matchesItems(
                List.of(Ingredient.of(Items.IRON_INGOT)),
                List.of(1),
                List.of(ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT))));
    }

    @Test
    void wearActionsDoNotUseThePresenceReservationCap() {
        List<ItemStack> offered = new java.util.ArrayList<>(
                java.util.Collections.nCopies(13, ItemStack.EMPTY));
        offered.set(12, new ItemStack(Items.IRON_PICKAXE));

        assertTrue(RecipeResourceMatcher.matchesItems(
                List.of(Ingredient.of(Items.IRON_PICKAXE)),
                List.of(0),
                List.of(ItemInputAction.wear(1)),
                offered));
    }

    @Test
    void fluidsAllocateAcrossTanksButNeverAcrossFluidIdentity() {
        List<FluidStack> requirements = List.of(
                new FluidStack(Fluids.WATER, 700),
                new FluidStack(Fluids.WATER, 500));

        assertTrue(RecipeResourceMatcher.matchesFluids(
                requirements,
                List.of(
                        new FluidStack(Fluids.WATER, 600),
                        new FluidStack(Fluids.WATER, 600))));
        assertFalse(RecipeResourceMatcher.matchesFluids(
                requirements,
                List.of(
                        new FluidStack(Fluids.WATER, 600),
                        new FluidStack(Fluids.LAVA, 600))));
    }
}
