package com.masson.cruciblecraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * GT6 {@code WorldgenOnSurface} knobs for {@code overworld.rocks}:
 * {@code amount=2}, {@code probability=3}.
 */
public record SurfaceRockConfiguration(
        int amount,
        int probability) implements FeatureConfiguration {
    public static final Codec<SurfaceRockConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                            Codec.INT.fieldOf("amount")
                                    .forGetter(SurfaceRockConfiguration::amount),
                            Codec.INT.fieldOf("probability")
                                    .forGetter(SurfaceRockConfiguration::probability))
                    .apply(instance, SurfaceRockConfiguration::new));
}
