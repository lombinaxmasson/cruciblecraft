package com.masson.cruciblecraft.recipe.gt;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class CompactGTRecipeFamilySerializer
        implements RecipeSerializer<CompactGTRecipeFamilyEntry> {
    @Override
    public MapCodec<CompactGTRecipeFamilyEntry> codec() {
        return CompactGTRecipeFamilyEntry.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CompactGTRecipeFamilyEntry> streamCodec() {
        return CompactGTRecipeFamilyStreamCodec.INSTANCE;
    }
}
