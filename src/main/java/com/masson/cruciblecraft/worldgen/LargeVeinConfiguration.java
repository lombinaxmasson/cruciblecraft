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

    private static final Codec<TagKey<Block>> BLOCK_TAG_CODEC = ResourceLocation.CODEC.xmap(
            id -> TagKey.create(Registries.BLOCK, id),
            TagKey::location);

    public static final MapCodec<LargeVeinConfiguration> MAP_CODEC =
            RecordCodecBuilder.<LargeVeinConfiguration>mapCodec(instance -> instance.group(
            WeightedState.CODEC.listOf().fieldOf("top").forGetter(LargeVeinConfiguration::top),
            WeightedState.CODEC.listOf().fieldOf("bottom").forGetter(LargeVeinConfiguration::bottom),
            WeightedState.CODEC.listOf().fieldOf("between").forGetter(LargeVeinConfiguration::between),
            WeightedState.CODEC.listOf().fieldOf("spread").forGetter(LargeVeinConfiguration::spread),
            Codec.intRange(-64, 320).fieldOf("min_y").forGetter(LargeVeinConfiguration::minY),
            Codec.intRange(-64, 320).fieldOf("max_y").forGetter(LargeVeinConfiguration::maxY),
            Codec.intRange(4, 48).fieldOf("horizontal_radius").forGetter(LargeVeinConfiguration::horizontalRadius),
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
        if (config.minY > config.maxY) {
            return DataResult.error(() -> "min_y must not exceed max_y");
        }
        if (config.top.isEmpty() || config.bottom.isEmpty()) {
            return DataResult.error(() -> "top and bottom weighted lists must not be empty");
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
