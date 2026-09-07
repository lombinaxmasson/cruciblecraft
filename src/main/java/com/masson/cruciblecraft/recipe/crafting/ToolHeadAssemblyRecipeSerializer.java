package com.masson.cruciblecraft.recipe.crafting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Network codec for GT6 tool-head plus stick assembly. */
public final class ToolHeadAssemblyRecipeSerializer
        implements RecipeSerializer<ToolHeadAssemblyRecipe> {
    private static final StreamCodec<
            RegistryFriendlyByteBuf, ToolHeadAssemblyRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(
                    ToolHeadAssemblyRecipe.CODEC.codec());

    @Override
    public com.mojang.serialization.MapCodec<ToolHeadAssemblyRecipe> codec() {
        return ToolHeadAssemblyRecipe.CODEC;
    }

    @Override
    public StreamCodec<
            RegistryFriendlyByteBuf, ToolHeadAssemblyRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
