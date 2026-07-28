package com.masson.cruciblecraft.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record CokeOvenRecipeInput(ItemStack stack) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        if (index != 0) {
            throw new IllegalArgumentException("Coke oven recipe input only has slot 0");
        }
        return stack;
    }

    @Override
    public int size() {
        return 1;
    }
}
