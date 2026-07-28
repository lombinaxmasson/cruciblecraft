package com.masson.cruciblecraft.recipe;

import com.masson.cruciblecraft.registry.ModRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public record CokeOvenRecipe(
        Ingredient input,
        ItemStack output,
        FluidStack fluidOutput,
        int duration) implements Recipe<CokeOvenRecipeInput> {

    public CokeOvenRecipe {
        output = output.copy();
        fluidOutput = fluidOutput.copy();
        if (output.isEmpty()) {
            throw new IllegalArgumentException("Coke oven output must not be empty");
        }
        if (fluidOutput.isEmpty()) {
            throw new IllegalArgumentException("Coke oven fluid output must not be empty");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Coke oven duration must be positive");
        }
    }

    @Override
    public boolean matches(CokeOvenRecipeInput recipeInput, Level level) {
        return input.test(recipeInput.stack());
    }

    @Override
    public ItemStack assemble(
            CokeOvenRecipeInput recipeInput,
            HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COKE_OVEN_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COKE_OVEN_TYPE.get();
    }
}
