package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.ElectricEngineBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.energy.converter.EnergyConverterHost;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.registry.ModBlockEntities;

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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 six-way EU to KU engine with state and piston-phase output semantics. */
public final class ElectricEngineBlock extends Block
        implements EntityBlock, EnergyConverterHost, ToolInteractable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final net.minecraft.resources.ResourceLocation converterId;

    public ElectricEngineBlock(
            net.minecraft.resources.ResourceLocation converterId,
            Properties properties) {
        super(properties);
        this.converterId = converterId;
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, false));
    }

    @Override
    public net.minecraft.resources.ResourceLocation converterId() {
        return converterId;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING,
                context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MachineCoverHost machine
                && MachineCoverBlockInteraction.rightClick(
                        machine, level, pos, player, hit)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof MachineCoverHost machine) {
            ToolResult coverResult = MachineCoverBlockInteraction.useTool(
                    machine, action, context);
            if (coverResult != ToolResult.PASS) {
                return coverResult;
            }
        }
        if (action != ToolAction.SCREWDRIVER) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide
                && level.getBlockEntity(context.getClickedPos())
                        instanceof ElectricEngineBlockEntity engine) {
            engine.cycleState();
            ToolClick.hurt(context);
        }
        return ToolResult.SUCCESS;
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
        return MachineCoverBlockInteraction.directRedstone(
                level, pos, direction);
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
            BlockState next,
            boolean moved) {
        if (state.getBlock() != next.getBlock()) {
            MachineCoverBlockInteraction.dropCovers(level, pos);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricEngineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.ELECTRIC_ENGINE.get()
                ? (currentLevel, pos, currentState, blockEntity) ->
                        ElectricEngineBlockEntity.serverTick(
                                currentLevel,
                                pos,
                                currentState,
                                (ElectricEngineBlockEntity) blockEntity)
                : null;
    }
}
