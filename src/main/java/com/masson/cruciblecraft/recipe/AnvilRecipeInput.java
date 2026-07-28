package com.masson.cruciblecraft.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record AnvilRecipeInput(ItemStack first, ItemStack second, AnvilMode mode) implements RecipeInput {
    public AnvilRecipeInput(ItemStack stack) {
        this(stack, ItemStack.EMPTY, AnvilMode.ANVIL);
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case 0 -> first;
            case 1 -> second;
            default -> throw new IllegalArgumentException("No anvil item for index " + slot);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
