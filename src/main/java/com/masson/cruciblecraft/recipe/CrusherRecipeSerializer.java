package com.masson.cruciblecraft.recipe;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class CrusherRecipeSerializer implements RecipeSerializer<CrusherRecipe> {
    private static final MapCodec<CrusherRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MaterialForm.CODEC.fieldOf("input").forGetter(CrusherRecipe::input),
            MaterialForm.CODEC.fieldOf("output").forGetter(CrusherRecipe::output),
            com.mojang.serialization.Codec.INT.optionalFieldOf("output_count", 1).forGetter(CrusherRecipe::outputCount),
            com.mojang.serialization.Codec.INT.fieldOf("power").forGetter(CrusherRecipe::power),
            com.mojang.serialization.Codec.INT.fieldOf("duration").forGetter(CrusherRecipe::duration)
    ).apply(instance, CrusherRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> STREAM_CODEC =
            StreamCodec.of((buffer, recipe) -> {
                buffer.writeUtf(recipe.input().serializedName());
                buffer.writeUtf(recipe.output().serializedName());
                buffer.writeVarInt(recipe.outputCount());
                buffer.writeVarInt(recipe.power());
                buffer.writeVarInt(recipe.duration());
            }, buffer -> new CrusherRecipe(
                    MaterialForm.parse(buffer.readUtf()), MaterialForm.parse(buffer.readUtf()),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

    @Override public MapCodec<CrusherRecipe> codec() { return CODEC; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> streamCodec() { return STREAM_CODEC; }
}
