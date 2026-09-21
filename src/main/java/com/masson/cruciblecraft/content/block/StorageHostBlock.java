package com.masson.cruciblecraft.content.block;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.BlockHitResult;

public abstract class StorageHostBlock extends Block
        implements EntityBlock, ToolInteractable {
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

    protected static boolean coverClick(
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        return level.getBlockEntity(pos) instanceof MachineCoverHost machine
                && MachineCoverBlockInteraction.rightClick(
                        machine, level, pos, player, hit);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return ToolClick.useItemOn(stack, level, player, hand, hit);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof MachineCoverHost machine) {
            return MachineCoverBlockInteraction.useTool(machine, action, context);
        }
        return ToolResult.PASS;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return MachineCoverBlockInteraction.weakRedstone(level, pos, direction);
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return MachineCoverBlockInteraction.directRedstone(level, pos, direction);
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction) {
        return MachineCoverBlockInteraction.canConnectRedstone(
                level, pos, direction);
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            MachineCoverBlockInteraction.dropCovers(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
