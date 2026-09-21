package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.HopperBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverCollision;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverInstall;
import com.masson.cruciblecraft.logistics.hopper.HopperVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One Hopper-family block. Kind/material live on the variant, not subclasses. */
public final class HopperBlock extends Block
        implements EntityBlock, ToolInteractable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape TOP = Block.box(0.0, 10.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape FUNNEL = Block.box(4.0, 4.0, 4.0, 12.0, 10.0, 12.0);
    private static final VoxelShape CONVEX = Shapes.or(TOP, FUNNEL);
    private static final VoxelShape DOWN = Shapes.or(
            CONVEX, Block.box(6.0, 0.0, 6.0, 10.0, 4.0, 10.0));
    private static final VoxelShape NORTH = Shapes.or(
            CONVEX, Block.box(6.0, 4.0, 0.0, 10.0, 8.0, 4.0));
    private static final VoxelShape SOUTH = Shapes.or(
            CONVEX, Block.box(6.0, 4.0, 12.0, 10.0, 8.0, 16.0));
    private static final VoxelShape WEST = Shapes.or(
            CONVEX, Block.box(0.0, 4.0, 6.0, 4.0, 8.0, 10.0));
    private static final VoxelShape EAST = Shapes.or(
            CONVEX, Block.box(12.0, 4.0, 6.0, 16.0, 8.0, 10.0));
    private static final VoxelShape UP = Shapes.join(
            CONVEX, Block.box(6.0, 10.0, 6.0, 10.0, 16.0, 10.0), BooleanOp.ONLY_FIRST);

    private final HopperVariant variant;

    public HopperBlock(HopperVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    public HopperVariant variant() {
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
                FACING, context.getClickedFace().getOpposite());
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
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        VoxelShape base = switch (state.getValue(FACING)) {
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case UP -> UP;
        };
        return CoverCollision.union(base, level.getBlockEntity(pos));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof HopperBlockEntity hopper
                && MachineCoverBlockInteraction.rightClick(
                        hopper, level, pos, player, hit)) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof HopperBlockEntity hopper) {
            serverPlayer.openMenu(
                    hopper, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
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
        ItemInteractionResult tool = ToolClick.useItemOn(
                stack, level, player, hand, hit);
        if (tool.consumesAction()) {
            return tool;
        }
        if (CoverInstall.tryPlace(level, pos, hit, stack, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof HopperBlockEntity hopper
                && MachineCoverBlockInteraction.rightClick(
                        hopper, level, pos, player, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return tool;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof HopperBlockEntity hopper)) {
            return ToolResult.PASS;
        }
        ToolResult cover = MachineCoverBlockInteraction.useTool(
                hopper, action, context);
        if (cover != ToolResult.PASS) {
            return cover;
        }
        if (action == ToolAction.PINCERS) {
            Player player = context.getPlayer();
            if (player == null) {
                return ToolResult.PASS;
            }
            if (!level.isClientSide) {
                boolean extracted = false;
                var inventory = hopper.inventory();
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    ItemStack taken = inventory.extractItem(slot, 64, false);
                    if (!taken.isEmpty()) {
                        ToolClick.give(player, taken);
                        extracted = true;
                        break;
                    }
                }
                if (extracted) {
                    ToolClick.hurt(context);
                }
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.WRENCH) {
            Direction target = Gt6StyleConnections.sideFromHit(ToolClick.hit(context));
            BlockState state = level.getBlockState(context.getClickedPos());
            if (!level.isClientSide) {
                if (state.getValue(FACING) != target) {
                    level.setBlock(
                            context.getClickedPos(),
                            state.setValue(FACING, target),
                            Block.UPDATE_ALL);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SCREWDRIVER) {
            if (!level.isClientSide) {
                hopper.cycleMode(context.getPlayer() != null
                        && context.getPlayer().isShiftKeyDown());
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            hopper.statusMessage(), true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.MONKEY_WRENCH) {
            if (variant.kind() == HopperKind.QUEUE_HOPPER) {
                return ToolResult.PASS;
            }
            if (!level.isClientSide) {
                hopper.toggleExactMode();
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            hopper.statusMessage(), true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (!level.isClientSide) {
                hopper.resetModes();
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            hopper.statusMessage(), true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState next,
            boolean moved) {
        if (!state.is(next.getBlock())
                && level.getBlockEntity(pos) instanceof HopperBlockEntity hopper) {
            hopper.dropContents();
            hopper.dropCovers();
        }
        super.onRemove(state, level, pos, next, moved);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HopperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.HOPPER.get()
                ? (current, pos, currentState, blockEntity) ->
                        HopperBlockEntity.serverTick(
                                current,
                                pos,
                                currentState,
                                (HopperBlockEntity) blockEntity)
                : null;
    }

    public static Component queueHasNoExactMode() {
        return Component.translatable(
                "message.cruciblecraft.hopper.queue_no_exact");
    }
}
