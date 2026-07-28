package com.masson.cruciblecraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CokeOvenRecipeSerializer implements RecipeSerializer<CokeOvenRecipe> {
    public static final MapCodec<CokeOvenRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(CokeOvenRecipe::input),
                    ItemStack.STRICT_CODEC.fieldOf("output").forGetter(CokeOvenRecipe::output),
                    FluidStack.CODEC.fieldOf("fluid_output").forGetter(CokeOvenRecipe::fluidOutput),
                    com.mojang.serialization.Codec.INT.fieldOf("duration")
                            .forGetter(CokeOvenRecipe::duration)
            ).apply(instance, CokeOvenRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CokeOvenRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    CokeOvenRecipe::input,
                    ItemStack.STREAM_CODEC,
                    CokeOvenRecipe::output,
                    FluidStack.STREAM_CODEC,
                    CokeOvenRecipe::fluidOutput,
                    ByteBufCodecs.VAR_INT,
                    CokeOvenRecipe::duration,
                    CokeOvenRecipe::new);

    @Override
    public MapCodec<CokeOvenRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CokeOvenRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
