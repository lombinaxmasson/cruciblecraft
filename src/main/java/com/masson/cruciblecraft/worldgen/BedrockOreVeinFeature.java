package com.masson.cruciblecraft.worldgen;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** GT6 {@code WorldgenOresBedrock}: chunk-seeded independent catalog rolls. */
public class BedrockOreVeinFeature extends Feature<NoneFeatureConfiguration> {
    public BedrockOreVeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int chunkX = minX >> 4;
        int chunkZ = minZ >> 4;
        long chunkSeed = level.getSeed()
                ^ (chunkX * 341873128712L + chunkZ * 132897987541L)
                ^ 0x00BEDB01L;
        Random random = new Random(chunkSeed);
        return BedrockOreVeins.generate(level, origin, random);
    }
}
