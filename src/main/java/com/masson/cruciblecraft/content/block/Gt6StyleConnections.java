package com.masson.cruciblecraft.content.block;

import java.util.Map;

import com.masson.cruciblecraft.content.item.MaterialWrenchItem;
import com.masson.cruciblecraft.logistics.pipe.PipeTopology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * GTM default {@code gt6StylePipesCables} connections.
 *
 * <p>Placing against a compatible neighbor opens that face and mirrors the
 * neighbor. Other faces stay closed until a wrench toggles them, including
 * machine attachments. Neighbor updates never auto-open or auto-close.
 */
public final class Gt6StyleConnections {
    private Gt6StyleConnections() {}

    public static Map<Direction, BooleanProperty> properties(BlockState state) {
        if (state.getBlock() instanceof CableBlock) {
            return CableBlock.PROPERTY_BY_DIRECTION;
        }
        if (state.getBlock() instanceof AbstractPipeBlock) {
            return AbstractPipeBlock.PROPERTY_BY_DIRECTION;
        }
        return Map.of();
    }

    public static boolean isOpen(BlockState state, Direction side) {
        BooleanProperty property = properties(state).get(side);
        return property != null && state.getValue(property);
    }

    public static BlockState withSide(
            BlockState state, Direction side, boolean open) {
        BooleanProperty property = properties(state).get(side);
        return property == null ? state : state.setValue(property, open);
    }

    public static boolean sameNetwork(BlockState self, BlockState neighbor) {
        if (self.getBlock() instanceof CableBlock
                && neighbor.getBlock() instanceof CableBlock) {
            return true;
        }
        return self.getBlock() instanceof AbstractPipeBlock selfPipe
                && neighbor.getBlock() instanceof AbstractPipeBlock neighborPipe
                && selfPipe.pipe().kind() == neighborPipe.pipe().kind();
    }

    public static boolean canConnect(
            Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CableBlock cable) {
            return cable.connectsTo(level, pos, side);
        }
        if (state.getBlock() instanceof AbstractPipeBlock pipe) {
            return pipe.connectsTo(level, pos, side);
        }
        return false;
    }

    public static void setConnection(
            Level level,
            BlockPos pos,
            Direction side,
            boolean open) {
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (properties(state).isEmpty() || isOpen(state, side) == open) {
            return;
        }
        BlockState updated = withSide(state, side, open);
        level.setBlock(pos, updated, Block.UPDATE_ALL);
        if (updated.getBlock() instanceof AbstractPipeBlock) {
            PipeTopology.invalidate(level, pos);
        }
        BlockPos neighborPos = pos.relative(side);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (sameNetwork(updated, neighbor)
                && isOpen(neighbor, side.getOpposite()) != open) {
            level.setBlock(
                    neighborPos,
                    withSide(neighbor, side.getOpposite(), open),
                    Block.UPDATE_ALL);
            if (neighbor.getBlock() instanceof AbstractPipeBlock) {
                PipeTopology.invalidate(level, neighborPos);
            }
        }
    }

    public static void applyPlacement(
            Level level, BlockPos pos, Direction clickedFace) {
        if (level.isClientSide) {
            return;
        }
        Direction towardsClicked = clickedFace.getOpposite();
        if (canConnect(level, pos, towardsClicked)) {
            setConnection(level, pos, towardsClicked, true);
        }
        BlockState self = level.getBlockState(pos);
        for (Direction facing : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(facing));
            if (sameNetwork(self, neighbor)
                    && isOpen(neighbor, facing.getOpposite())
                    && !isOpen(self, facing)) {
                setConnection(level, pos, facing, true);
                self = level.getBlockState(pos);
            }
        }
    }

    public static boolean placeBlock(
            BlockPlaceContext context, boolean placed) {
        if (placed && !context.getLevel().isClientSide) {
            applyPlacement(
                    context.getLevel(),
                    context.getClickedPos(),
                    context.getClickedFace());
        }
        return placed;
    }

    public static ItemInteractionResult wrench(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(stack.getItem() instanceof MaterialWrenchItem)
                || properties(state).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        Direction side = hit.getDirection();
        setConnection(level, pos, side, !isOpen(state, side));
        return ItemInteractionResult.SUCCESS;
    }
}
