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
 * GT6 {@code WorldgenStoneLayers}: replace vanilla stone/cobble/deepslate with
 * the noise-selected layer cube, then 1/128 32757 pebbles using
 * {@code tLastRock}. Does not emit {@code StoneLayerOres}.
 */
public class StoneLayerRockFeature extends Feature<StoneLayerRockConfiguration> {
    public StoneLayerRockFeature() {
        super(StoneLayerRockConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<StoneLayerRockConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int chunkX = minX >> 4;
        int chunkZ = minZ >> 4;
        long chunkSeed = level.getSeed()
                ^ (chunkX * 341873128712L + chunkZ * 132897987541L)
                ^ 32757L;
        RandomSource random = RandomSource.create(chunkSeed);
        int minY = level.getMinBuildHeight() + 1;
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
            String material = StoneLayerCatalog.surfaceMaterial(noise, x, y, z);
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
            } else if (isReplaceableStone(state)) {
                canPlace = true;
                lastRock = material;
                placed |= tryReplace(
                        level,
                        cursor.immutable(),
                        material,
                        StoneLayerStones.Role.STONE);
            } else if (isCobble(state)) {
                canPlace = true;
                lastRock = material;
                placed |= tryReplace(
                        level,
                        cursor.immutable(),
                        material,
                        StoneLayerStones.Role.COBBLE);
            } else if (isMossyCobble(state)) {
                canPlace = true;
                lastRock = material;
                placed |= tryReplace(
                        level,
                        cursor.immutable(),
                        material,
                        StoneLayerStones.Role.MOSSY_COBBLE);
            } else if (StoneLayerStones.isNaturalLayerCube(state)) {
                canPlace = true;
            } else if (isVanillaOre(state)) {
                canPlace = true;
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

    public static boolean tryReplace(
            WorldGenLevel level,
            BlockPos pos,
            String material,
            StoneLayerStones.Role role) {
        BlockState target = StoneLayerStones.cube(material, role);
        BlockState existing = level.getBlockState(pos);
        if (existing.getBlock() == target.getBlock()) {
            return false;
        }
        return level.setBlock(pos, target, Block.UPDATE_CLIENTS);
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

    static boolean isReplaceableStone(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.INFESTED_STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.INFESTED_DEEPSLATE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.TUFF);
    }

    static boolean isCobble(BlockState state) {
        return state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE);
    }

    static boolean isMossyCobble(BlockState state) {
        return state.is(Blocks.MOSSY_COBBLESTONE);
    }

    static boolean isVanillaOre(BlockState state) {
        return state.is(BlockTags.COAL_ORES)
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
