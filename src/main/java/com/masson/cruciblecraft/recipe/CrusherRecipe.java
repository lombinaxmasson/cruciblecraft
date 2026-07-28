package com.masson.cruciblecraft.recipe;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record CrusherRecipe(MaterialForm input, MaterialForm output, int outputCount, int power, int duration)
        implements Recipe<CrusherRecipeInput> {
    public CrusherRecipe {
        CrusherRecipeRules.validate(input, output, outputCount, power, duration);
    }

    @Override
    public boolean matches(CrusherRecipeInput recipeInput, Level level) {
        return MaterialUnits.resolve(recipeInput.stack())
                .filter(entry -> entry.form() == input)
                .flatMap(entry -> MaterialLookup.item(entry.material().id(), output))
                .isPresent();
    }

    @Override
    public ItemStack assemble(CrusherRecipeInput recipeInput, HolderLookup.Provider registries) {
        return MaterialUnits.resolve(recipeInput.stack())
                .filter(entry -> entry.form() == input)
                .flatMap(entry -> MaterialLookup.item(entry.material().id(), output))
                .map(item -> new ItemStack(item, outputCount))
                .orElse(ItemStack.EMPTY);
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.CRUSHER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.CRUSHER_TYPE.get(); }
}
