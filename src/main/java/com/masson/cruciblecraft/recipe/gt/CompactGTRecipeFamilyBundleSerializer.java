package com.masson.cruciblecraft.recipe.gt;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Network/datapack serializer for release compact-family bundles. */
public final class CompactGTRecipeFamilyBundleSerializer
        implements RecipeSerializer<CompactGTRecipeFamilyBundle> {
    @Override
    public MapCodec<CompactGTRecipeFamilyBundle> codec() {
        return CompactGTRecipeFamilyBundle.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CompactGTRecipeFamilyBundle>
            streamCodec() {
        return CompactGTRecipeFamilyStreamCodec.BUNDLE;
    }
}
