package com.masson.cruciblecraft.recipe.crafting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Network codec for the source-faithful battery-cell crafting recipe. */
public final class BatteryCellCraftingRecipeSerializer
        implements RecipeSerializer<BatteryCellCraftingRecipe> {
    private static final StreamCodec<
            RegistryFriendlyByteBuf, BatteryCellCraftingRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(
                    BatteryCellCraftingRecipe.CODEC.codec());

    @Override
    public com.mojang.serialization.MapCodec<BatteryCellCraftingRecipe> codec() {
        return BatteryCellCraftingRecipe.CODEC;
    }

    @Override
    public StreamCodec<
            RegistryFriendlyByteBuf, BatteryCellCraftingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
