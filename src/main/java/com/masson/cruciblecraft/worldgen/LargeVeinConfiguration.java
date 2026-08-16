package com.masson.cruciblecraft.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * JSON-friendly configuration for a GT6-inspired layered deposit.
 * Every ore chunk represents 144 material units (one ingot).
 */
public record LargeVeinConfiguration(
        int profileVersion,
        ResourceLocation profileId,
        List<WeightedState> top,
        List<WeightedState> bottom,
        List<WeightedState> between,
        List<WeightedState> spread,
        int minY,
        int maxY,
        int horizontalRadius,
        int verticalRadius,
        float density,
        TagKey<Block> replaceable,
        int regionSizeChunks,
        float generationChance,
        int salt) implements FeatureConfiguration {

    /*
     * Large veins run inside a 3x3-chunk WorldGenRegion. Its anchor is min + 8,
     * leaving 24 blocks toward the minimum edge but only 23 toward the maximum
     * edge, so 23 is the largest radius that is safe in both directions.
     */
    static final int MAX_SAFE_HORIZONTAL_RADIUS = 23;

    private static final Codec<TagKey<Block>> BLOCK_TAG_CODEC = ResourceLocation.CODEC.xmap(
            id -> TagKey.create(Registries.BLOCK, id),
            TagKey::location);

    public static final MapCodec<LargeVeinConfiguration> MAP_CODEC =
            RecordCodecBuilder.<LargeVeinConfiguration>mapCodec(instance -> instance.group(
            Codec.intRange(1, 2).fieldOf("profile_version")
                    .forGetter(LargeVeinConfiguration::profileVersion),
            ResourceLocation.CODEC.fieldOf("profile_id")
                    .forGetter(LargeVeinConfiguration::profileId),
            WeightedState.CODEC.listOf().fieldOf("top").forGetter(LargeVeinConfiguration::top),
            WeightedState.CODEC.listOf().fieldOf("bottom").forGetter(LargeVeinConfiguration::bottom),
            WeightedState.CODEC.listOf().fieldOf("between").forGetter(LargeVeinConfiguration::between),
            WeightedState.CODEC.listOf().fieldOf("spread").forGetter(LargeVeinConfiguration::spread),
            Codec.intRange(-64, 320).fieldOf("min_y").forGetter(LargeVeinConfiguration::minY),
            Codec.intRange(-64, 320).fieldOf("max_y").forGetter(LargeVeinConfiguration::maxY),
            Codec.intRange(4, MAX_SAFE_HORIZONTAL_RADIUS).fieldOf("horizontal_radius")
                    .forGetter(LargeVeinConfiguration::horizontalRadius),
            Codec.intRange(2, 24).fieldOf("vertical_radius").forGetter(LargeVeinConfiguration::verticalRadius),
            Codec.floatRange(0.01F, 1.0F).fieldOf("density").forGetter(LargeVeinConfiguration::density),
            BLOCK_TAG_CODEC.fieldOf("replaceable").forGetter(LargeVeinConfiguration::replaceable),
            Codec.intRange(2, 32).fieldOf("region_size_chunks").forGetter(LargeVeinConfiguration::regionSizeChunks),
            Codec.floatRange(0.0F, 1.0F).fieldOf("generation_chance").forGetter(LargeVeinConfiguration::generationChance),
            Codec.INT.fieldOf("salt").forGetter(LargeVeinConfiguration::salt)
    ).apply(instance, LargeVeinConfiguration::new));
    public static final Codec<LargeVeinConfiguration> CODEC =
            MAP_CODEC.codec().validate(LargeVeinConfiguration::validate);

    private static DataResult<LargeVeinConfiguration> validate(LargeVeinConfiguration config) {
        if (!"cruciblecraft".equals(config.profileId.getNamespace())) {
            return DataResult.error(() -> "profile_id must use the cruciblecraft namespace");
        }
        if (config.minY > config.maxY) {
            return DataResult.error(() -> "min_y must not exceed max_y");
        }
        if (config.top.isEmpty() || config.bottom.isEmpty()) {
            return DataResult.error(() -> "top and bottom weighted lists must not be empty");
        }
        // Keep this invariant explicit even if a future codec stops using intRange.
        if (config.horizontalRadius > MAX_SAFE_HORIZONTAL_RADIUS) {
            return DataResult.error(() -> "horizontal_radius must not exceed "
                    + MAX_SAFE_HORIZONTAL_RADIUS
                    + " for the 3x3 WorldGenRegion anchor-at-min+8 assumption");
        }
        return DataResult.success(config);
    }

    public record WeightedState(BlockState state, int weight) {
        public static final Codec<WeightedState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("state").forGetter(WeightedState::state),
                Codec.intRange(1, 10_000).fieldOf("weight").forGetter(WeightedState::weight)
        ).apply(instance, WeightedState::new));
    }
}
