package com.masson.cruciblecraft.recipe.gt;

import java.util.Objects;

import com.masson.cruciblecraft.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Vanilla datapack shell for a concrete {@link GTRecipe}. */
public record GTRecipeEntry(ResourceLocation map, GTRecipe recipe)
        implements Recipe<RecipeInput> {
    public static final MapCodec<GTRecipeEntry> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("map").forGetter(GTRecipeEntry::map),
                    GTRecipe.MAP_CODEC.forGetter(GTRecipeEntry::recipe)
            ).apply(instance, GTRecipeEntry::new));

    public GTRecipeEntry {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(recipe, "recipe");
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return recipe.itemOutputs().stream().findFirst().orElse(ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GT_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.GT_RECIPE_TYPE.get();
    }
}
