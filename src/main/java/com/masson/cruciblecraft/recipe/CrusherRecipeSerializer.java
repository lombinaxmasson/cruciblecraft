package com.masson.cruciblecraft.recipe;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class CrusherRecipeSerializer implements RecipeSerializer<CrusherRecipe> {
    private static final MapCodec<CrusherRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MaterialPrefix.CODEC.fieldOf("input").forGetter(CrusherRecipe::input),
            MaterialPrefix.CODEC.fieldOf("output").forGetter(CrusherRecipe::output),
            Codec.INT.optionalFieldOf("output_count", 1).forGetter(CrusherRecipe::outputCount),
            Codec.INT.fieldOf("power").forGetter(CrusherRecipe::power),
            Codec.INT.fieldOf("duration").forGetter(CrusherRecipe::duration),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("material_durations", Map.of())
                    .forGetter(CrusherRecipe::materialDurations)
    ).apply(instance, CrusherRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> STREAM_CODEC =
            StreamCodec.of((buffer, recipe) -> {
                buffer.writeUtf(recipe.input().serializedName());
                buffer.writeUtf(recipe.output().serializedName());
                buffer.writeVarInt(recipe.outputCount());
                buffer.writeVarInt(recipe.power());
                buffer.writeVarInt(recipe.duration());
                Map<String, Integer> orderedDurations =
                        new TreeMap<>(recipe.materialDurations());
                buffer.writeVarInt(orderedDurations.size());
                orderedDurations.forEach((material, duration) -> {
                    buffer.writeUtf(material);
                    buffer.writeVarInt(duration);
                });
            }, buffer -> new CrusherRecipe(
                    MaterialPrefixCatalog.require(buffer.readUtf()),
                    MaterialPrefixCatalog.require(buffer.readUtf()),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    readMaterialDurations(buffer)));

    private static Map<String, Integer> readMaterialDurations(
            RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<String, Integer> durations = new HashMap<>();
        for (int index = 0; index < size; index++) {
            durations.put(buffer.readUtf(), buffer.readVarInt());
        }
        return durations;
    }

    @Override public MapCodec<CrusherRecipe> codec() { return CODEC; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> streamCodec() { return STREAM_CODEC; }
}
