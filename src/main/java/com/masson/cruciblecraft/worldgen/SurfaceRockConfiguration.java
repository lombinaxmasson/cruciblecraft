package com.masson.cruciblecraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration for the surface rock scatter (GT6 {@code WorldgenRocks}
 * shape, CC surface placement): one rock per {@code rarity} surface
 * positions, drawn from the {@code rock_tag} block tag.
 */
public record SurfaceRockConfiguration(
        int rarity,
        TagKey<Block> rockTag) implements FeatureConfiguration {
    public static final Codec<SurfaceRockConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                            Codec.INT.fieldOf("rarity")
                                    .forGetter(SurfaceRockConfiguration::rarity),
                            ResourceLocation.CODEC
                                    .xmap(
                                            location -> TagKey.create(
                                                    Registries.BLOCK, location),
                                            TagKey::location)
                                    .fieldOf("rock_tag")
                                    .forGetter(SurfaceRockConfiguration::rockTag))
                    .apply(instance, SurfaceRockConfiguration::new));
}
