package com.masson.cruciblecraft.worldgen;

import java.util.Random;

import com.masson.cruciblecraft.content.block.GtSmallOreBlock;
import com.masson.cruciblecraft.worldgen.SmallOreCatalog.Coltan;
import com.masson.cruciblecraft.worldgen.SmallOreCatalog.Entry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * GT6 {@code WorldgenOresSmall} scatter plus overworld {@code WorldgenColtan}.
 * Chunk RNG copies {@code WD.random}.
 */
public class SmallOreFeature extends Feature<NoneFeatureConfiguration> {
    public SmallOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        Random random = gt6ChunkRandom(level.getSeed(), dimId(level), minX, minZ);
        boolean nether = level.getLevel().dimension() == Level.NETHER;
        boolean placed = false;
        if (!nether) {
            placed |= generateColtan(level, minX, minZ, random);
        }
        for (Entry entry : SmallOreCatalog.forNether(nether)) {
            placed |= generateEntry(level, minX, minZ, entry, random);
        }
        return placed;
    }

    public static boolean tryPlace(WorldGenLevel level, BlockPos pos, String materialId) {
        return BedrockOreVeins.placeSmallOre(level, pos, materialId);
    }

    public static int placeCount(int amount, Random random) {
        return Math.max(1, amount / 2 + random.nextInt(1 + amount) / 2);
    }

    public static BlockPos coltanCenter(long worldSeed) {
        Coltan coltan = SmallOreCatalog.COLTAN;
        Random seeded = new Random(worldSeed + coltan.seedOffset());
        int x = (int) (seeded.nextGaussian() * coltan.gaussianScale());
        int z = (int) (seeded.nextGaussian() * coltan.gaussianScale());
        return new BlockPos(x, 0, z);
    }

    public static Random gt6ChunkRandom(long worldSeed, int dimId, int minX, int minZ) {
        long seed = worldSeed ^ (long) dimId;
        long chunkX = minX >> 4;
        long chunkZ = minZ >> 4;
        Random random = new Random(seed);
        for (int i = 0; i < 50; i++) {
            random.nextInt(0x00ffffff);
        }
        random = new Random(
                seed
                        ^ ((random.nextLong() >> 2 + 1L) * chunkX
                                + (random.nextLong() >> 2 + 1L) * chunkZ));
        for (int i = 0; i < 50; i++) {
            random.nextInt(0x00ffffff);
        }
        return random;
    }

    static int dimId(WorldGenLevel level) {
        var dimension = level.getLevel().dimension();
        if (dimension == Level.NETHER) {
            return -1;
        }
        if (dimension == Level.END) {
            return 1;
        }
        return 0;
    }

    static boolean generateEntry(
            WorldGenLevel level, int minX, int minZ, Entry entry, Random random) {
        boolean placed = false;
        int count = placeCount(entry.amount(), random);
        for (int i = 0; i < count; i++) {
            placed |= scatter(level, minX, minZ, entry.minY(), entry.ySpan(), random, entry.materialId(), true);
        }
        return placed;
    }

    static boolean generateColtan(
            WorldGenLevel level, int minX, int minZ, Random random) {
        Coltan coltan = SmallOreCatalog.COLTAN;
        BlockPos center = coltanCenter(level.getSeed());
        if ((center.getX() >> 4) == (minX >> 4) && (center.getZ() >> 4) == (minZ >> 4)) {
            BedrockOreVeins.generateVein(
                    level,
                    minX,
                    minZ,
                    level.getMinBuildHeight(),
                    coltan.materials().getFirst(),
                    random);
        }
        int distance = (center.getX() - minX) * (center.getX() - minX)
                + (center.getZ() - minZ) * (center.getZ() - minZ);
        if (distance > coltan.range() * coltan.range()) {
            return false;
        }
        boolean placed = false;
        int smallCount = placeCount(coltan.amount(), random);
        for (int i = 0; i < smallCount; i++) {
            String material = coltan.pickMaterial(random.nextInt(5));
            placed |= scatter(
                    level, minX, minZ, coltan.minY(), coltan.ySpan(), random, material, true);
        }
        if (distance > coltan.largeOreRangeSquared()) {
            return placed;
        }
        int largeCount = placeCount(coltan.amount(), random);
        for (int i = 0; i < largeCount; i++) {
            String material = coltan.pickMaterial(random.nextInt(5));
            placed |= scatter(
                    level, minX, minZ, coltan.minY(), coltan.ySpan(), random, material, false);
        }
        return placed;
    }

    private static boolean scatter(
            WorldGenLevel level,
            int minX,
            int minZ,
            int minY,
            int span,
            Random random,
            String materialId,
            boolean small) {
        int x = minX + random.nextInt(16);
        int y = minY + random.nextInt(span);
        int z = minZ + random.nextInt(16);
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (small) {
            return BedrockOreVeins.placeSmallOre(level, pos, materialId);
        }
        return BedrockOreVeins.placeNormalOre(level, pos, materialId);
    }

    public static boolean isSmallOre(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof GtSmallOreBlock;
    }
}
