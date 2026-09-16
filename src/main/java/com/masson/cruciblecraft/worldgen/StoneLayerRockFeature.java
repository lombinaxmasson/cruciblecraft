package com.masson.cruciblecraft.worldgen;

import com.masson.cruciblecraft.content.blockentity.GtSurfaceRockBlockEntity;
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
 * GT6 {@code WorldgenStoneLayers} 32757 pebbles: 1/128 on opaque stone/cave
 * surfaces using {@code tLastRock}. Does not replace stone cubes or emit ores.
 */
public class StoneLayerRockFeature extends Feature<StoneLayerRockConfiguration> {
    public StoneLayerRockFeature() {
        super(StoneLayerRockConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<StoneLayerRockConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int minY = Math.max(level.getMinBuildHeight() + 1, 1);
        int maxY = level.getMaxBuildHeight();
        StoneLayerNoise noise = new StoneLayerNoise((int) level.getSeed(), 0);
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                placed |= decorateColumn(
                        level,
                        minX + dx,
                        minZ + dz,
                        minY,
                        maxY,
                        noise,
                        random,
                        context.config().probability());
            }
        }
        return placed;
    }

    public static boolean decorateColumn(
            WorldGenLevel level,
            int x,
            int z,
            int minY,
            int maxY,
            StoneLayerNoise noise,
            RandomSource random,
            int probability) {
        String lastRock = StoneLayerCatalog.DEEPSLATE;
        boolean canPlace = false;
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int chance = Math.max(1, probability);
        for (int y = minY; y < maxY; y++) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (state.is(Blocks.BEDROCK)) {
                canPlace = true;
            } else if (state.isAir()) {
                if (canPlace && random.nextInt(chance) == 0) {
                    placed |= tryPlace(
                            level,
                            cursor.immutable(),
                            lastRock);
                }
                canPlace = false;
            } else if (isStoneCell(state)) {
                canPlace = true;
                lastRock = StoneLayerCatalog.surfaceMaterial(noise, x, y, z);
            } else if (SurfaceRockFeature.easyRep(state)
                    && state.getFluidState().isEmpty()) {
                if (canPlace && random.nextInt(chance) == 0) {
                    placed |= tryPlace(
                            level,
                            cursor.immutable(),
                            lastRock);
                }
                canPlace = false;
            } else if (state.isSolidRender(level, cursor)) {
                canPlace = isGroundSurface(state);
            } else {
                canPlace = false;
            }
        }
        return placed;
    }

    public static boolean tryPlace(
            WorldGenLevel level,
            BlockPos pos,
            String material) {
        if (material == null || material.isEmpty()) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        if (!existing.getFluidState().isEmpty()) {
            return false;
        }
        if (!existing.isAir() && !SurfaceRockFeature.easyRep(existing)) {
            return false;
        }
        BlockState pebble = ModBlocks.GT_SURFACE_ROCK.get().defaultBlockState()
                .setValue(
                        SurfaceRockAppearance.PROPERTY,
                        SurfaceRockFeature.appearance(level.getBlockState(pos.below())))
                .setValue(SurfaceRockContents.PROPERTY, SurfaceRockContents.EMPTY);
        if (!level.setBlock(pos, pebble, Block.UPDATE_CLIENTS)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof GtSurfaceRockBlockEntity rock) {
            rock.setMaterial(material);
        }
        return true;
    }

    static boolean isStoneCell(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.STONE)
                || state.is(Blocks.INFESTED_STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.COPPER_ORES);
    }

    static boolean isGroundSurface(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(BlockTags.TERRACOTTA);
    }
}
