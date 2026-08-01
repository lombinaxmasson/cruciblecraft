package com.masson.cruciblecraft.recipe;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class AnvilRecipeSerializer implements RecipeSerializer<AnvilRecipe> {
    public static final MapCodec<AnvilRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MaterialPrefix.CODEC.fieldOf("input").forGetter(AnvilRecipe::input),
            com.mojang.serialization.Codec.INT.optionalFieldOf("input_count", 1)
                    .forGetter(AnvilRecipe::inputCount),
            MaterialPrefix.CODEC.optionalFieldOf("second_input").forGetter(AnvilRecipe::secondInput),
            com.mojang.serialization.Codec.INT.optionalFieldOf("second_input_count", 1)
                    .forGetter(AnvilRecipe::secondInputCount),
            MaterialPrefix.CODEC.fieldOf("output").forGetter(AnvilRecipe::output),
            com.mojang.serialization.Codec.INT.optionalFieldOf("output_count", 1)
                    .forGetter(AnvilRecipe::outputCount),
            com.mojang.serialization.Codec.INT.optionalFieldOf("hits", 4)
                    .forGetter(AnvilRecipe::hits),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("material")
                    .forGetter(AnvilRecipe::material),
            AnvilMode.CODEC.optionalFieldOf("mode", AnvilMode.ANVIL)
                    .forGetter(AnvilRecipe::mode),
            MaterialPrefix.CODEC.optionalFieldOf("secondary_output")
                    .forGetter(AnvilRecipe::secondaryOutput),
            com.mojang.serialization.Codec.INT.optionalFieldOf("secondary_output_count", 1)
                    .forGetter(AnvilRecipe::secondaryOutputCount),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("secondary_chance", 1.0)
                    .forGetter(AnvilRecipe::secondaryChance),
            com.mojang.serialization.Codec.LONG.optionalFieldOf("recipe_power", 10_000L)
                    .forGetter(AnvilRecipe::recipePower)
    ).apply(instance, AnvilRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilRecipe> STREAM_CODEC =
            StreamCodec.of(AnvilRecipeSerializer::encode, AnvilRecipeSerializer::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, AnvilRecipe recipe) {
        writeForm(buffer, recipe.input());
        buffer.writeVarInt(recipe.inputCount());
        writeOptionalForm(buffer, recipe.secondInput());
        buffer.writeVarInt(recipe.secondInputCount());
        writeForm(buffer, recipe.output());
        buffer.writeVarInt(recipe.outputCount());
        buffer.writeVarInt(recipe.hits());
        writeOptionalString(buffer, recipe.material());
        buffer.writeEnum(recipe.mode());
        writeOptionalForm(buffer, recipe.secondaryOutput());
        buffer.writeVarInt(recipe.secondaryOutputCount());
        buffer.writeDouble(recipe.secondaryChance());
        buffer.writeVarLong(recipe.recipePower());
    }

    private static AnvilRecipe decode(RegistryFriendlyByteBuf buffer) {
        return new AnvilRecipe(
                readForm(buffer),
                buffer.readVarInt(),
                readOptionalForm(buffer),
                buffer.readVarInt(),
                readForm(buffer),
                buffer.readVarInt(),
                buffer.readVarInt(),
                readOptionalString(buffer),
                buffer.readEnum(AnvilMode.class),
                readOptionalForm(buffer),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readVarLong());
    }

    private static void writeForm(RegistryFriendlyByteBuf buffer, MaterialPrefix form) {
        buffer.writeUtf(form.serializedId());
    }

    private static MaterialPrefix readForm(RegistryFriendlyByteBuf buffer) {
        return MaterialPrefixCatalog.require(buffer.readUtf());
    }

    private static void writeOptionalForm(
            RegistryFriendlyByteBuf buffer,
            Optional<MaterialPrefix> form) {
        buffer.writeBoolean(form.isPresent());
        form.ifPresent(value -> writeForm(buffer, value));
    }

    private static Optional<MaterialPrefix> readOptionalForm(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(readForm(buffer)) : Optional.empty();
    }

    private static void writeOptionalString(
            RegistryFriendlyByteBuf buffer,
            Optional<String> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(buffer::writeUtf);
    }

    private static Optional<String> readOptionalString(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(buffer.readUtf()) : Optional.empty();
    }

    @Override
    public MapCodec<AnvilRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AnvilRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
