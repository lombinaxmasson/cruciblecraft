package com.masson.cruciblecraft.recipe.gt;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class CompactPublicationPolicySerializer
        implements RecipeSerializer<CompactPublicationPolicyEntry> {
    private static final StreamCodec<RegistryFriendlyByteBuf, CompactPublicationPolicyEntry>
            STREAM_CODEC =
                    ByteBufCodecs.fromCodecWithRegistries(
                            CompactPublicationPolicyEntry.CODEC.codec());

    @Override
    public MapCodec<CompactPublicationPolicyEntry> codec() {
        return CompactPublicationPolicyEntry.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CompactPublicationPolicyEntry> streamCodec() {
        return STREAM_CODEC;
    }
}
