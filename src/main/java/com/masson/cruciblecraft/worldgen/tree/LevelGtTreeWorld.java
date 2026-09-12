package com.masson.cruciblecraft.worldgen.tree;

import com.masson.cruciblecraft.content.block.GtTreeHoleBlock;
import com.masson.cruciblecraft.content.block.GtTreeLeavesBlock;
import com.masson.cruciblecraft.content.block.GtTreeLogBlock;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeWorld;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;
import com.masson.cruciblecraft.worldgen.tree.prep.HorizontalFacing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LevelGtTreeWorld implements GtTreeWorld {
    private final LevelAccessor level;
    private final GtTreeSpecies species;

    public LevelGtTreeWorld(LevelAccessor level, GtTreeSpecies species) {
        this.level = level;
        this.species = species;
    }

    @Override
    public int height() {
        return level.getMaxBuildHeight();
    }

    @Override
    public boolean canPlaceTree(int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState state = level.getBlockState(pos(x, y, z));
        return state.isAir()
                || state.canBeReplaced()
                || state.getBlock() instanceof GtTreeLeavesBlock;
    }

    @Override
    public void setLog(int x, int y, int z) {
        BlockState log = ModBlocks.treeLog(species)
                .get()
                .defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        level.setBlock(pos(x, y, z), log, Block.UPDATE_ALL);
    }

    @Override
    public void setLeaves(int x, int y, int z) {
        BlockState leaves = ModBlocks.treeLeaves(species)
                .get()
                .defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, 1)
                .setValue(LeavesBlock.PERSISTENT, Boolean.TRUE);
        level.setBlock(pos(x, y, z), leaves, Block.UPDATE_ALL);
    }

    @Override
    public void setRubberResinHole(int x, int y, int z, HorizontalFacing facing) {
        BlockState hole = ModBlocks.treeHole(GtTreeSpecies.RUBBER)
                .get()
                .defaultBlockState()
                .setValue(GtTreeHoleBlock.FACING, toDirection(facing))
                .setValue(GtTreeHoleBlock.HAS_PRODUCT, Boolean.FALSE);
        BlockPos holePos = pos(x, y, z);
        level.setBlock(holePos, hole, Block.UPDATE_ALL);
        GtTreeHoleTracker.add(level, holePos);
    }

    @Override
    public boolean hasNearbyRubberResinHole(int x, int z) {
        return GtTreeHoleTracker.nearby(level, x, z);
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        return y < level.getMinBuildHeight()
                || y >= level.getMaxBuildHeight()
                || level.getBlockState(pos(x, y, z)).isAir();
    }

    @Override
    public boolean isDirtOrGrass(int x, int y, int z) {
        return level.getBlockState(pos(x, y, z)).is(BlockTags.DIRT);
    }

    @Override
    public void setPodzol(int x, int y, int z) {
        if (isDirtOrGrass(x, y, z)) {
            level.setBlock(pos(x, y, z), Blocks.PODZOL.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static Direction toDirection(HorizontalFacing facing) {
        return switch (facing) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }
}
