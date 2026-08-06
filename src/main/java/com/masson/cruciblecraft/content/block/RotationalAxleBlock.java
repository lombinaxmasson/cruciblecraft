package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.blockentity.RotationalAxleBlockEntity;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/** Straight RU connector; unlike cable it only exposes its configured axis. */
public final class RotationalAxleBlock extends RotatedPillarBlock
        implements EntityBlock {
    public RotationalAxleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        return defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos, BlockState state) {
        return new RotationalAxleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.ROTATIONAL_AXLE.get()
                ? (currentLevel, pos, currentState, blockEntity) ->
                        RotationalAxleBlockEntity.serverTick(
                                currentLevel,
                                pos,
                                currentState,
                                (RotationalAxleBlockEntity) blockEntity)
                : null;
    }
}
