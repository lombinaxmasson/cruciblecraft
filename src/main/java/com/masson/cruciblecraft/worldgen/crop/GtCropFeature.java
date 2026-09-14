package com.masson.cruciblecraft.worldgen.crop;

import java.util.Random;
import java.util.Set;

import com.masson.cruciblecraft.content.block.GtBushBlock;
import com.masson.cruciblecraft.content.blockentity.GtBushBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * GT6 {@code WorldgenOnSurface} for glowtus and bushes.
 */
public final class GtCropFeature extends Feature<GtCropConfiguration> {
    public GtCropFeature() {
        super(GtCropConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GtCropConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource randomSource = context.random();
        Random random = new Random(randomSource.nextLong());
        GtCropKind kind = context.config().kind();
        BlockPos origin = context.origin();
        String biome = level.getBiome(origin)
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");
        int amount = GtCropPlacement.canGenerate(kind, Set.of(biome));
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
                if (!targets[dx][dz] || !GtCropPlacement.shouldPlaceRay(kind, random)) {
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
                    placed |= tryPlace(level, kind, x, y, z, random, contact);
                    break;
                }
            }
        }
        return placed;
    }

    public static boolean tryPlace(
            WorldGenLevel level,
            GtCropKind kind,
            int x,
            int y,
            int z,
            Random random,
            BlockState contact) {
        if (kind == GtCropKind.GLOWTUS) {
            return tryPlaceGlowtus(level, x, y, z, random, contact);
        }
        return tryPlaceBush(level, x, y, z, random, contact);
    }

    public static boolean tryPlaceGlowtus(
            WorldGenLevel level,
            int x,
            int y,
            int z,
            Random random,
            BlockState contact) {
        if (!contact.getFluidState().is(FluidTags.WATER)) {
            return false;
        }
        BlockPos above = new BlockPos(x, y + 1, z);
        if (!level.getBlockState(above).isAir()) {
            return false;
        }
        GlowtusColor color = GlowtusColor.byMeta(random.nextInt(16));
        return level.setBlock(
                above,
                ModBlocks.glowtus(color).get().defaultBlockState(),
                Block.UPDATE_CLIENTS);
    }

    public static boolean tryPlaceBush(
            WorldGenLevel level,
            int x,
            int y,
            int z,
            Random random,
            BlockState contact) {
        if (!placeBushCore(level, x, y, z, contact)) {
            return false;
        }
        if (random.nextBoolean()) {
            placeBushCore(level, x - 1, y, z, level.getBlockState(new BlockPos(x - 1, y, z)));
        }
        if (random.nextBoolean()) {
            placeBushCore(level, x + 1, y, z, level.getBlockState(new BlockPos(x + 1, y, z)));
        }
        if (random.nextBoolean()) {
            placeBushCore(level, x, y, z - 1, level.getBlockState(new BlockPos(x, y, z - 1)));
        }
        if (random.nextBoolean()) {
            placeBushCore(level, x, y, z + 1, level.getBlockState(new BlockPos(x, y, z + 1)));
        }
        placeBushSides(level, x, y, z);
        return true;
    }

    private static boolean placeBushCore(
            WorldGenLevel level, int x, int y, int z, BlockState contact) {
        if (!GtBushBlock.plantable(contact)) {
            return false;
        }
        BlockPos above = new BlockPos(x, y + 1, z);
        if (!level.getBlockState(above).isAir()
                && !level.getBlockState(above).canBeReplaced()) {
            return false;
        }
        BlockPos soil = new BlockPos(x, y, z);
        if (contact.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(soil, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BlockState bush = ModBlocks.GT_BUSH.get()
                .defaultBlockState()
                .setValue(GtBushBlock.FACING, Direction.DOWN)
                .setValue(GtBushBlock.STAGE, GtBushBlock.MAX_STAGE);
        if (!level.setBlock(above, bush, Block.UPDATE_CLIENTS)) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(above);
        if (be instanceof GtBushBlockEntity bushEntity) {
            bushEntity.setBerry(GtBushBlockEntity.DEFAULT_BERRY.copy());
        }
        return true;
    }

    private static void placeBushSides(WorldGenLevel level, int x, int y, int z) {
        placeBushSide(level, x + 1, y + 1, z, Direction.WEST);
        placeBushSide(level, x - 1, y + 1, z, Direction.EAST);
        placeBushSide(level, x, y + 1, z + 1, Direction.NORTH);
        placeBushSide(level, x, y + 1, z - 1, Direction.SOUTH);
        placeBushSide(level, x, y + 2, z, Direction.DOWN);
    }

    private static void placeBushSide(
            WorldGenLevel level, int x, int y, int z, Direction facing) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).canBeReplaced()) {
            return;
        }
        BlockState bush = ModBlocks.GT_BUSH.get()
                .defaultBlockState()
                .setValue(GtBushBlock.FACING, facing)
                .setValue(GtBushBlock.STAGE, GtBushBlock.MAX_STAGE);
        if (!bush.canSurvive(level, pos)) {
            return;
        }
        if (!level.setBlock(pos, bush, Block.UPDATE_CLIENTS)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof GtBushBlockEntity bushEntity) {
            bushEntity.setBerry(GtBushBlockEntity.DEFAULT_BERRY.copy());
        }
    }

    private static boolean isOpaque(WorldGenLevel level, BlockPos pos, BlockState state) {
        return state.isSolidRender(level, pos);
    }
}
