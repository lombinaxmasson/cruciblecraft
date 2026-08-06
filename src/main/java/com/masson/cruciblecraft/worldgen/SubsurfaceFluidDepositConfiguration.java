package com.masson.cruciblecraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Data-only specification for one finite subsurface fluid reserve. */
public record SubsurfaceFluidDepositConfiguration(
        ResourceLocation material,
        long minAmountMb,
        long maxAmountMb,
        int minY,
        int maxY,
        int searchRange,
        TagKey<Block> replaceable,
        int regionSizeChunks,
        float generationChance,
        int salt,
        int productionAmountMb,
        int productionIntervalTicks,
        int accumulationCapMb,
        boolean ventOverflow) implements FeatureConfiguration {

    private static final Codec<TagKey<Block>> BLOCK_TAG_CODEC =
            ResourceLocation.CODEC.xmap(
                    id -> TagKey.create(Registries.BLOCK, id),
                    TagKey::location);
    private static final Codec<Long> AMOUNT_CODEC = Codec.LONG.validate(
            amount -> amount >= 1L && amount <= 1_000_000_000L
                    ? DataResult.success(amount)
                    : DataResult.error(
                            () -> "deposit amount must be between 1 and "
                                    + "1000000000 mB"));

    public static final MapCodec<SubsurfaceFluidDepositConfiguration> MAP_CODEC =
            RecordCodecBuilder.<SubsurfaceFluidDepositConfiguration>mapCodec(
                    instance -> instance.group(
                            ResourceLocation.CODEC.fieldOf("material")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::material),
                            AMOUNT_CODEC.fieldOf("min_amount_mb")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::minAmountMb),
                            AMOUNT_CODEC.fieldOf("max_amount_mb")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::maxAmountMb),
                            Codec.intRange(-64, 320).fieldOf("min_y")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::minY),
                            Codec.intRange(-64, 320).fieldOf("max_y")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::maxY),
                            Codec.intRange(0, 64).fieldOf("search_range")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::searchRange),
                            BLOCK_TAG_CODEC.fieldOf("replaceable")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::replaceable),
                            Codec.intRange(2, 64).fieldOf("region_size_chunks")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::regionSizeChunks),
                            Codec.floatRange(0.0F, 1.0F).fieldOf("generation_chance")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::generationChance),
                            Codec.INT.fieldOf("salt")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::salt),
                            Codec.intRange(1, 1_000_000)
                                    .fieldOf("production_amount_mb")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::productionAmountMb),
                            Codec.intRange(1, 72_000)
                                    .fieldOf("production_interval_ticks")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::productionIntervalTicks),
                            Codec.intRange(1, 1_000_000_000)
                                    .fieldOf("accumulation_cap_mb")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::accumulationCapMb),
                            Codec.BOOL.fieldOf("vent_overflow")
                                    .forGetter(SubsurfaceFluidDepositConfiguration::ventOverflow))
                            .apply(instance, SubsurfaceFluidDepositConfiguration::new));

    public static final Codec<SubsurfaceFluidDepositConfiguration> CODEC =
            MAP_CODEC.codec().validate(SubsurfaceFluidDepositConfiguration::validate);

    private static DataResult<SubsurfaceFluidDepositConfiguration> validate(
            SubsurfaceFluidDepositConfiguration configuration) {
        if (configuration.minY > configuration.maxY) {
            return DataResult.error(() -> "min_y must not exceed max_y");
        }
        if (configuration.minAmountMb > configuration.maxAmountMb) {
            return DataResult.error(
                    () -> "min_amount_mb must not exceed max_amount_mb");
        }
        if (configuration.accumulationCapMb
                < configuration.productionAmountMb) {
            return DataResult.error(
                    () -> "accumulation_cap_mb must cover one production cycle");
        }
        return DataResult.success(configuration);
    }
}
