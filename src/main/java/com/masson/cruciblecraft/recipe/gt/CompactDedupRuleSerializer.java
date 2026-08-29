package com.masson.cruciblecraft.recipe.gt;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class CompactDedupRuleSerializer
        implements RecipeSerializer<CompactDedupRuleEntry> {
    private static final StreamCodec<RegistryFriendlyByteBuf, CompactDedupRuleEntry>
            STREAM_CODEC =
                    ByteBufCodecs.fromCodecWithRegistries(
                            CompactDedupRuleEntry.CODEC.codec());

    @Override
    public MapCodec<CompactDedupRuleEntry> codec() {
        return CompactDedupRuleEntry.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CompactDedupRuleEntry> streamCodec() {
        return STREAM_CODEC;
    }
}
