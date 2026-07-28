package com.masson.cruciblecraft.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Persistent identity retained when a configured material item is unavailable. */
public record MissingMaterialComponent(String originalItemId, String materialId, String form) {
    public static final Codec<MissingMaterialComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("original_item_id").forGetter(MissingMaterialComponent::originalItemId),
            Codec.STRING.fieldOf("material_id").forGetter(MissingMaterialComponent::materialId),
            Codec.STRING.fieldOf("form").forGetter(MissingMaterialComponent::form)
    ).apply(instance, MissingMaterialComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MissingMaterialComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    MissingMaterialComponent::originalItemId,
                    ByteBufCodecs.STRING_UTF8,
                    MissingMaterialComponent::materialId,
                    ByteBufCodecs.STRING_UTF8,
                    MissingMaterialComponent::form,
                    MissingMaterialComponent::new);
}
