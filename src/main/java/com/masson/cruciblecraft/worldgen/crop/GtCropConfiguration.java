package com.masson.cruciblecraft.worldgen.crop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record GtCropConfiguration(GtCropKind kind) implements FeatureConfiguration {
    public static final Codec<GtCropConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                            Codec.STRING.fieldOf("kind")
                                    .forGetter(config -> config.kind().id()))
                    .apply(instance, GtCropConfiguration::fromId));

    public static GtCropConfiguration fromId(String id) {
        return new GtCropConfiguration(GtCropKind.fromId(id));
    }
}
