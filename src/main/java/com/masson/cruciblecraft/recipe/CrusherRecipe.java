package com.masson.cruciblecraft.recipe;

import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record CrusherRecipe(
        MaterialPrefix input,
        MaterialPrefix output,
        int outputCount,
        int power,
        int duration,
        Map<String, Integer> materialDurations)
        implements Recipe<CrusherRecipeInput> {
    public CrusherRecipe {
        materialDurations = Map.copyOf(materialDurations);
        CrusherRecipeRules.validate(
                input, output, outputCount, power, duration, materialDurations);
    }

    public CrusherRecipe(
            MaterialPrefix input,
            MaterialPrefix output,
            int outputCount,
            int power,
            int duration) {
        this(input, output, outputCount, power, duration, Map.of());
    }

    public int durationFor(String materialId) {
        return CrusherRecipeRules.durationFor(
                materialId,
                duration,
                materialDurations);
    }

    @Override
    public boolean matches(CrusherRecipeInput recipeInput, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CrusherRecipeInput recipeInput, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.CRUSHER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.CRUSHER_TYPE.get(); }
}
