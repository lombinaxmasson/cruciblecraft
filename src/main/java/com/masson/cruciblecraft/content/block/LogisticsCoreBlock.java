package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.LogisticsCoreBlockEntity;
import com.masson.cruciblecraft.logistics.core.LogisticsCorePart;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreTagged;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Outward-facing Logistics Core controller. */
public final class LogisticsCoreBlock extends Block
        implements EntityBlock, LogisticsCoreTagged {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public LogisticsCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public LogisticsCorePart corePart() {
        return LogisticsCorePart.CONTROLLER;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!level.isClientSide
                && level.getBlockEntity(pos)
                        instanceof LogisticsCoreBlockEntity core) {
            for (Component line : core.statusLines()) {
                player.displayClientMessage(line, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsCoreBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos)
                        instanceof LogisticsCoreBlockEntity core) {
            core.unbindAll();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.LOGISTICS_CORE.get()
                ? (lvl, blockPos, blockState, be) ->
                        LogisticsCoreBlockEntity.serverTick(
                                lvl, blockPos, blockState,
                                (LogisticsCoreBlockEntity) be)
                : null;
    }
}
