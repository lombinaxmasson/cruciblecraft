package com.masson.cruciblecraft.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record CrusherRecipeInput(ItemStack stack) implements RecipeInput {
    @Override public ItemStack getItem(int index) { return index == 0 ? stack : ItemStack.EMPTY; }
    @Override public int size() { return 1; }
}
