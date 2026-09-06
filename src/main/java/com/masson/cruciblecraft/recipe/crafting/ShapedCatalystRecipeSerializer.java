package com.masson.cruciblecraft.recipe.crafting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Network codec for GT6 workbench recipes with tool catalyst slots. */
public final class ShapedCatalystRecipeSerializer
        implements RecipeSerializer<ShapedCatalystRecipe> {
    private static final StreamCodec<
            RegistryFriendlyByteBuf, ShapedCatalystRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(
                    ShapedCatalystRecipe.CODEC.codec());

    @Override
    public com.mojang.serialization.MapCodec<ShapedCatalystRecipe> codec() {
        return ShapedCatalystRecipe.CODEC;
    }

    @Override
    public StreamCodec<
            RegistryFriendlyByteBuf, ShapedCatalystRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
