package com.masson.cruciblecraft.content.block;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.storage.StorageVariant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public abstract class StorageHostBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final StorageVariant variant;

    protected StorageHostBlock(StorageVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public StorageVariant variant() {
        return variant;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity>
            BlockEntityTicker<A> createTicker(
                    Level level,
                    BlockEntityType<A> actual,
                    BlockEntityType<E> expected,
                    BlockEntityTicker<? super E> ticker) {
        return expected == actual && !level.isClientSide
                ? (BlockEntityTicker<A>) ticker
                : null;
    }

    protected static boolean stillValid(
            Level level, BlockPos pos, Predicate<Block> expected) {
        return level.getBlockState(pos).getBlock() instanceof StorageHostBlock block
                && expected.test(block);
    }
}
