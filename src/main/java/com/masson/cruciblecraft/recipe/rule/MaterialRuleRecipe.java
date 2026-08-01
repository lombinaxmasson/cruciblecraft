package com.masson.cruciblecraft.recipe.rule;

import com.masson.cruciblecraft.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Vanilla RecipeManager envelope around the loader-independent rule value. */
public record MaterialRuleRecipe(MaterialRule rule) implements Recipe<RecipeInput> {
    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }
    @Override public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MATERIAL_RULE_SERIALIZER.get();
    }
    @Override public RecipeType<?> getType() {
        return ModRecipes.MATERIAL_RULE_TYPE.get();
    }
}
