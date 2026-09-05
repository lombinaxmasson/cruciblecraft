package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.blockentity.DynamoBlockEntity;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import org.jetbrains.annotations.Nullable;

/** Fixed bronze kinetic-to-electric converter. */
public final class DynamoBlock extends Block implements EntityBlock, com.masson.cruciblecraft.energy.converter.EnergyConverterHost {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private final net.minecraft.resources.ResourceLocation converterId;

    public DynamoBlock(
            net.minecraft.resources.ResourceLocation converterId,
            Properties properties) {
        super(properties);
        this.converterId = converterId;
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    public net.minecraft.resources.ResourceLocation converterId() {
        return converterId;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DynamoBlockEntity(pos, state);
    }

    @Nullable
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.DYNAMO.get()
                ? (currentLevel, pos, currentState, blockEntity) ->
                        DynamoBlockEntity.serverTick(
                                currentLevel,
                                pos,
                                currentState,
                                (DynamoBlockEntity) blockEntity)
                : null;
    }
}
