package com.masson.cruciblecraft.recipe.crafting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Network codec for GT6 prefix pack / unpack crafting. */
public final class PrefixPackRecipeSerializer
        implements RecipeSerializer<PrefixPackRecipe> {
    private static final StreamCodec<
            RegistryFriendlyByteBuf, PrefixPackRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(PrefixPackRecipe.CODEC.codec());

    @Override
    public com.mojang.serialization.MapCodec<PrefixPackRecipe> codec() {
        return PrefixPackRecipe.CODEC;
    }

    @Override
    public StreamCodec<
            RegistryFriendlyByteBuf, PrefixPackRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
