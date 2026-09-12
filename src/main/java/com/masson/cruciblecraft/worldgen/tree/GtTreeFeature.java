package com.masson.cruciblecraft.worldgen.tree;

import java.util.Random;
import java.util.Set;

import com.masson.cruciblecraft.content.block.GtTreeHoleBlock;
import com.masson.cruciblecraft.content.block.GtTreeLeavesBlock;
import com.masson.cruciblecraft.content.block.GtTreeLogBlock;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeGrower;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreePlacement;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * GT6 {@code WorldgenOnSurface} + sapling {@code grow}. Placement probability
 * stays inside the feature; biome modifiers only restrict which chunks roll.
 */
public final class GtTreeFeature extends Feature<GtTreeConfiguration> {
    public GtTreeFeature() {
        super(GtTreeConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GtTreeConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource randomSource = context.random();
        Random random = new Random(randomSource.nextLong());
        GtTreeSpecies species = context.config().species();
        BlockPos origin = context.origin();
        String biome = level.getBiome(origin)
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");
        int amount = GtTreePlacement.canGenerate(species, Set.of(biome), random);
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
                if (!targets[dx][dz] || !GtTreePlacement.shouldPlaceRay(species, random)) {
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
                                    || isWood(contact)
                                    || isLeaves(contact))) {
                        continue;
                    }
                    placed |= tryPlace(level, species, x, y, z, random, contact);
                    break;
                }
            }
        }
        return placed;
    }

    private static boolean tryPlace(
            WorldGenLevel level,
            GtTreeSpecies species,
            int x,
            int y,
            int z,
            Random random,
            BlockState contact) {
        if (!isPlantable(species, contact)) {
            return false;
        }
        BlockPos sapling = new BlockPos(x, y + 1, z);
        BlockState above = level.getBlockState(sapling);
        if (!above.isAir() && !above.canBeReplaced()) {
            return false;
        }
        return GtTreeGrower.grow(
                species, new LevelGtTreeWorld(level, species), x, y + 1, z, random);
    }

    private static boolean isPlantable(GtTreeSpecies species, BlockState contact) {
        if (contact.is(BlockTags.DIRT)) {
            return true;
        }
        return species == GtTreeSpecies.COCONUT && contact.is(BlockTags.SAND);
    }

    private static boolean isOpaque(WorldGenLevel level, BlockPos pos, BlockState state) {
        return state.isSolidRender(level, pos);
    }

    private static boolean isWood(BlockState state) {
        return state.is(BlockTags.LOGS)
                || state.getBlock() instanceof GtTreeLogBlock
                || state.getBlock() instanceof GtTreeHoleBlock;
    }

    private static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.getBlock() instanceof GtTreeLeavesBlock;
    }
}
