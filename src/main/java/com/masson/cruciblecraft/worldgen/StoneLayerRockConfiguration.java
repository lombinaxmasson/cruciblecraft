package com.masson.cruciblecraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * GT6 {@code WorldgenStoneLayers} pebble chance: {@code nextInt(128) == 0}.
 */
public record StoneLayerRockConfiguration(int probability)
        implements FeatureConfiguration {
    public static final Codec<StoneLayerRockConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                            Codec.INT.fieldOf("probability")
                                    .forGetter(StoneLayerRockConfiguration::probability))
                    .apply(instance, StoneLayerRockConfiguration::new));
}
