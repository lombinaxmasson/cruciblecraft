package com.masson.cruciblecraft.worldgen;

import java.util.Random;
import java.util.Set;

import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * GT6 {@code WorldgenRocks} / {@code WorldgenOnSurface} overworld pebbles.
 * Places {@code gt_surface_rock} (MTE 32757), never a material catalog cube.
 */
public class SurfaceRockFeature extends Feature<SurfaceRockConfiguration> {
    public SurfaceRockFeature() {
        super(SurfaceRockConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<SurfaceRockConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource randomSource = context.random();
        Random random = new Random(randomSource.nextLong());
        SurfaceRockConfiguration config = context.config();
        BlockPos origin = context.origin();
        String biome = level.getBiome(origin)
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");
        if (SurfaceRockPlacement.canGenerate(Set.of(biome)) <= 0) {
            return false;
        }
        int amount = Math.max(1, config.amount());
        int sea = level.getSeaLevel();
        int minHeight = Math.min(level.getMaxBuildHeight() - 2, sea - 1);
        int maxHeight = Math.min(
                level.getMaxBuildHeight() - 1,
                level.dimensionType().hasCeiling() ? 80 : minHeight * 2 + 16);
        boolean[][] targets = new boolean[16][16];
        for (int i = 0; i < amount; i++) {
            targets[random.nextInt(16)][random.nextInt(16)] = true;
        }
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                if (!targets[dx][dz] || random.nextInt(config.probability()) != 0) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                for (int y = maxHeight; y >= minHeight; y--) {
                    cursor.set(x, y, z);
                    BlockState contact = level.getBlockState(cursor);
                    if (contact.is(Blocks.FARMLAND)) {
                        break;
                    }
                    if (contact.getFluidState().isEmpty()
                            && (!isOpaque(level, cursor, contact)
                                    || contact.is(BlockTags.LOGS)
                                    || contact.is(BlockTags.LEAVES))) {
                        continue;
                    }
                    placed |= tryPlace(level, x, y, z, random, contact, amount);
                    break;
                }
            }
        }
        return placed;
    }

    public static boolean tryPlace(
            WorldGenLevel level,
            int x,
            int y,
            int z,
            Random random,
            BlockState contact) {
        return tryPlace(level, x, y, z, random, contact, SurfaceRockPlacement.AMOUNT);
    }

    public static boolean tryPlace(
            WorldGenLevel level,
            int x,
            int y,
            int z,
            Random random,
            BlockState contact,
            int amount) {
        if (contact.is(Blocks.FARMLAND)) {
            return false;
        }
        if (!isSoil(contact)) {
            return false;
        }
        BlockPos above = new BlockPos(x, y + 1, z);
        BlockState existing = level.getBlockState(above);
        if (!easyRep(existing)) {
            return false;
        }
        BlockState pebble = ModBlocks.GT_SURFACE_ROCK.get().defaultBlockState()
                .setValue(SurfaceRockAppearance.PROPERTY, appearance(contact))
                .setValue(SurfaceRockContents.PROPERTY, rollContents(random, amount));
        return level.setBlock(above, pebble, Block.UPDATE_CLIENTS);
    }

    public static SurfaceRockContents rollContents(Random random, int amount) {
        if (random.nextInt(Math.max(1, amount)) != 0) {
            return SurfaceRockContents.EMPTY;
        }
        if (random.nextInt(12) == 0) {
            return random.nextInt(4) == 0
                    ? SurfaceRockContents.METEORIC_RAW
                    : SurfaceRockContents.METEORIC_ROCK;
        }
        return SurfaceRockContents.FLINT;
    }

    static boolean easyRep(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SNOW);
    }

    static boolean isSoil(BlockState contact) {
        return (contact.is(BlockTags.DIRT) && !contact.is(Blocks.FARMLAND))
                || contact.is(BlockTags.SAND)
                || contact.is(Blocks.GRAVEL);
    }

    public static SurfaceRockAppearance appearance(BlockState contact) {
        if (contact.is(BlockTags.SAND)
                || contact.is(Blocks.SANDSTONE)
                || contact.is(Blocks.RED_SANDSTONE)
                || contact.is(Blocks.CUT_SANDSTONE)
                || contact.is(Blocks.CHISELED_SANDSTONE)
                || contact.is(Blocks.SMOOTH_SANDSTONE)) {
            return SurfaceRockAppearance.SANDSTONE;
        }
        if (contact.is(Blocks.COBBLESTONE) || contact.is(Blocks.GRAVEL)) {
            return SurfaceRockAppearance.COBBLE;
        }
        return SurfaceRockAppearance.STONE;
    }

    private static boolean isOpaque(WorldGenLevel level, BlockPos pos, BlockState state) {
        return state.isSolidRender(level, pos);
    }
}
