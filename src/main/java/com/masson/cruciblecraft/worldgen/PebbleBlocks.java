package com.masson.cruciblecraft.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Shared GT6 {@code MultiTileEntityRock} placer physics: 0.25 hardness, no
 * collision, pick-up, and drop when the floor or a liquid neighbor fails.
 */
public final class PebbleBlocks {
    private PebbleBlocks() {}

    public static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(0.25F)
                .sound(SoundType.STONE)
                .noCollission()
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .pushReaction(PushReaction.DESTROY);
    }

    public static boolean canSurvive(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    public static boolean adjacentLiquid(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN) {
                continue;
            }
            if (!level.getFluidState(pos.relative(direction)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static InteractionResult collect(
            Level level,
            BlockPos pos,
            Player player,
            ItemStack drop) {
        if (!level.isClientSide) {
            if (!player.addItem(drop)) {
                Block.popResource(level, pos, drop);
            }
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void dropIfUnsupported(
            BlockState state,
            Level level,
            BlockPos pos,
            ItemStack drop) {
        if (level.isClientSide) {
            return;
        }
        if (!state.canSurvive(level, pos) || adjacentLiquid(level, pos)) {
            Block.popResource(level, pos, drop);
            level.removeBlock(pos, false);
        }
    }
}
