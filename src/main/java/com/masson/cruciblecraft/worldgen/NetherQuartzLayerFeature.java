package com.masson.cruciblecraft.worldgen;

import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * GT6 {@code WorldgenNetherQuartz}: two noise samples per column replace
 * netherrack with {@code BlockRockOres} meta 8. Not an overworld
 * {@code StoneLayer}.
 */
public class NetherQuartzLayerFeature extends Feature<NoneFeatureConfiguration> {
    public static final int BASE_Y = 40;
    public static final int SPAN = 200;
    public static final int SAMPLE_Y_LOW = 0;
    public static final int SAMPLE_Y_HIGH = 64;
    /** GT6 {@code NoiseGenerator(World)} uses {@code 512 * dimensionId} (-1). */
    public static final int NETHER_NOISE_OFFSET = -512;
    public static final String MATERIAL = "nether_quartz";

    public NetherQuartzLayerFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (level.getLevel().dimension() != Level.NETHER) {
            return false;
        }
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        StoneLayerNoise noise = new StoneLayerNoise(
                (int) level.getSeed(),
                NETHER_NOISE_OFFSET);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = minX + dx;
                int z = minZ + dz;
                placed |= tryPlace(
                        level,
                        cursor.set(x, sampleY(noise, x, SAMPLE_Y_LOW, z), z));
                placed |= tryPlace(
                        level,
                        cursor.set(x, sampleY(noise, x, SAMPLE_Y_HIGH, z), z));
            }
        }
        return placed;
    }

    public static int sampleY(StoneLayerNoise noise, int x, int sampleY, int z) {
        return BASE_Y + noise.get(x, sampleY, z, SPAN);
    }

    public static boolean tryPlace(WorldGenLevel level, BlockPos pos) {
        int y = pos.getY();
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return false;
        }
        if (!level.getBlockState(pos).is(Blocks.NETHERRACK)) {
            return false;
        }
        if (!ModBlocks.hasLayerStone(MATERIAL + "/dense_ore")) {
            return false;
        }
        BlockState quartz = StoneLayerStones.cube(
                MATERIAL,
                StoneLayerStones.Role.STONE);
        return level.setBlock(pos, quartz, Block.UPDATE_CLIENTS);
    }
}
