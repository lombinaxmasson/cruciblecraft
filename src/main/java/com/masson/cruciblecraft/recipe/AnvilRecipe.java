package com.masson.cruciblecraft.recipe;

import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.registry.ModRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record AnvilRecipe(
        MaterialPrefix input,
        int inputCount,
        Optional<MaterialPrefix> secondInput,
        int secondInputCount,
        MaterialPrefix output,
        int outputCount,
        int hits,
        Optional<String> material,
        AnvilMode mode,
        Optional<MaterialPrefix> secondaryOutput,
        int secondaryOutputCount,
        double secondaryChance,
        long recipePower) implements Recipe<AnvilRecipeInput> {

    public AnvilRecipe(MaterialPrefix input, MaterialPrefix output, int outputCount, int hits) {
        this(input, 1, Optional.empty(), 1, output, outputCount, hits, Optional.empty(),
                AnvilMode.ANVIL, Optional.empty(), 1, 0.0, 10_000L);
    }

    public AnvilRecipe(
            MaterialPrefix input,
            MaterialPrefix output,
            int outputCount,
            int hits,
            Optional<String> material) {
        this(input, 1, Optional.empty(), 1, output, outputCount, hits, material,
                AnvilMode.ANVIL, Optional.empty(), 1, 0.0, 10_000L);
    }

    public AnvilRecipe {
        material = AnvilRecipeRules.normalizeMaterial(material);
        AnvilRecipeRules.validate(
                input, inputCount, secondInput, secondInputCount, output, outputCount,
                hits, secondaryOutput, secondaryOutputCount, secondaryChance, recipePower);
    }

    @Override
    public boolean matches(AnvilRecipeInput recipeInput, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(AnvilRecipeInput recipeInput, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ANVIL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ANVIL_TYPE.get();
    }
}
