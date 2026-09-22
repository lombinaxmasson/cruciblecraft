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
 * GT6 {@code WorldgenSticks}: small wooden sticks scattered on the surface.
 */
public final class GtStickFeature extends Feature<StickConfiguration> {
    public GtStickFeature() {
        super(StickConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<StickConfiguration> context) {
        WorldGenLevel level = context.level();
        Random random = new Random(context.random().nextLong());
        StickConfiguration config = context.config();
        BlockPos origin = context.origin();
        String biome = level.getBiome(origin)
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");
        int amount = StickPlacement.canGenerate(Set.of(biome), config.amount());
        if (amount <= 0) {
            return false;
        }

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
                if (!targets[dx][dz]
                        || !StickPlacement.shouldPlaceRay(random, config.probability())) {
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
                            && (!contact.isSolidRender(level, cursor)
                                    || contact.is(BlockTags.LOGS)
                                    || contact.is(BlockTags.LEAVES))) {
                        continue;
                    }
                    placed |= tryPlace(level, x, y, z, contact);
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
            BlockState contact) {
        if (!StickPlacement.isPlantable(contact)) {
            return false;
        }
        BlockPos above = new BlockPos(x, y + 1, z);
        BlockState existing = level.getBlockState(above);
        if (!easyReplaceable(existing)) {
            return false;
        }
        return level.setBlock(
                above,
                ModBlocks.GT_STICK.get().defaultBlockState(),
                Block.UPDATE_CLIENTS);
    }

    private static boolean easyReplaceable(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SNOW);
    }
}
