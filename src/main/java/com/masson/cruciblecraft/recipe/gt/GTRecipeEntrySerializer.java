package com.masson.cruciblecraft.recipe.gt;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class GTRecipeEntrySerializer implements RecipeSerializer<GTRecipeEntry> {
    private static final StreamCodec<RegistryFriendlyByteBuf, GTRecipeEntry> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(GTRecipeEntry.CODEC.codec());

    @Override
    public MapCodec<GTRecipeEntry> codec() {
        return GTRecipeEntry.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, GTRecipeEntry> streamCodec() {
        return STREAM_CODEC;
    }
}
