package com.masson.cruciblecraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * GT6 {@code WorldgenSticks} amount and per-candidate probability.
 */
public record StickConfiguration(int amount, int probability)
        implements FeatureConfiguration {
    public static final Codec<StickConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                            Codec.INT.fieldOf("amount")
                                    .forGetter(StickConfiguration::amount),
                            Codec.INT.fieldOf("probability")
                                    .forGetter(StickConfiguration::probability))
                    .apply(instance, StickConfiguration::new));
}
