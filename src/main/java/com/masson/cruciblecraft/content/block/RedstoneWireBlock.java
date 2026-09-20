package com.masson.cruciblecraft.content.block;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireCovers;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
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
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code MultiTileEntityWireRedstone}: 2px connector, cutter faces,
 * vanilla redstone in/out with per-material loss. Torch/repeater/blank,
 * selectors, emitter, conductor, and progress scale attach here the way
 * GT6 hosts them on redstone connectors.
 */
public final class RedstoneWireBlock extends Block
        implements EntityBlock, ToolInteractable {
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION;
    private static final VoxelShape[] SHAPES_BARE = buildShapes(7.0D, 9.0D);
    private static final VoxelShape[] SHAPES_INSULATED = buildShapes(6.0D, 10.0D);

    static {
        EnumMap<Direction, BooleanProperty> properties =
                new EnumMap<>(Direction.class);
        properties.put(Direction.DOWN, DOWN);
        properties.put(Direction.UP, UP);
        properties.put(Direction.NORTH, NORTH);
        properties.put(Direction.SOUTH, SOUTH);
        properties.put(Direction.WEST, WEST);
        properties.put(Direction.EAST, EAST);
        PROPERTY_BY_DIRECTION = Map.copyOf(properties);
    }

    private final RedstoneWireKind kind;

    public RedstoneWireBlock(RedstoneWireKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        BlockState state = stateDefinition.any().setValue(POWER, 0);
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    public RedstoneWireKind kind() {
        return kind;
    }

    public static boolean isConnected(BlockState state, Direction direction) {
        return state.getBlock() instanceof RedstoneWireBlock
                && state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire) {
            if (action == ToolAction.CROWBAR) {
                return pryCover(context);
            }
            if (RedstoneWireCovers.onTool(
                    wire, context.getClickedFace(), action)) {
                if (!level.isClientSide) {
                    ToolClick.hurt(context);
                }
                return ToolResult.SUCCESS;
            }
        }
        if (action == ToolAction.CROWBAR) {
            return pryCover(context);
        }
        if (action != ToolAction.WIRE_CUTTER) {
            return ToolResult.PASS;
        }
        return Gt6StyleConnections.toggleConnection(
                context.getLevel(),
                context.getClickedPos(),
                ToolClick.hit(context));
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
        if (RedstoneWireCovers.tryInstall(
                level, pos, hit.getDirection(), stack, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire
                && RedstoneWireCovers.onRightClick(wire, hit, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return tool;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire
                && RedstoneWireCovers.onRightClick(wire, hit, player)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, POWER);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return Gt6StyleConnections.interactionShape(
                state, context, connectedShape(state));
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return com.masson.cruciblecraft.logistics.pipe.cover.CoverCollision.union(
                connectedShape(state), level.getBlockEntity(pos));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        return vanillaOutput(state, level, pos, direction, false);
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return vanillaOutput(state, level, pos, direction, true);
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction) {
        if (direction == null) {
            return true;
        }
        if (level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire
                && RedstoneWireCovers.connectsRedstone(wire, direction)) {
            return true;
        }
        return isConnected(state, direction);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire) {
            return wire.comparator();
        }
        return 0;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide
                && !oldState.is(state.getBlock())
                && level.getBlockEntity(pos)
                        instanceof RedstoneWireBlockEntity wire) {
            wire.onNeighborChanged();
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        super.neighborChanged(
                state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide
                && level.getBlockEntity(pos)
                        instanceof RedstoneWireBlockEntity wire) {
            wire.onNeighborChanged();
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneWireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.REDSTONE_WIRE.get()
                ? (currentLevel, pos, currentState, blockEntity) ->
                        RedstoneWireBlockEntity.serverTick(
                                currentLevel,
                                pos,
                                currentState,
                                (RedstoneWireBlockEntity) blockEntity)
                : null;
    }

    private int vanillaOutput(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction,
            boolean strong) {
        // 1.21 getSignal direction is the querier's facing toward this
        // block. GT6 isProvidingWeakPower2 flips with OPOS so the face is
        // the side of this wire toward the querier.
        Direction face = direction.getOpposite();
        if (!(level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire)) {
            return 0;
        }
        int cover = strong
                ? RedstoneWireCovers.strongOut(wire, face)
                : RedstoneWireCovers.weakOut(wire, face);
        if (cover >= 0) {
            return cover;
        }
        if (!isConnected(state, face)) {
            return 0;
        }
        if (wire.received() == face) {
            return 0;
        }
        BlockPos neighborPos = pos.relative(face);
        if (level.getBlockEntity(neighborPos)
                instanceof RedstoneWireBlockEntity) {
            return 0;
        }
        int levelValue = wire.visual();
        if (levelValue <= 0) {
            return 0;
        }
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof RedStoneWireBlock
                || neighbor.isRedstoneConductor(level, neighborPos)) {
            return Math.max(0, levelValue - 1);
        }
        return levelValue;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!level.isClientSide
                && state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos)
                        instanceof RedstoneWireBlockEntity wire) {
            wire.dropCovers();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static ToolResult pryCover(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire)) {
            return ToolResult.PASS;
        }
        Direction side = context.getClickedFace();
        if (level.isClientSide) {
            return wire.covers().get(side).isPresent()
                    ? ToolResult.SUCCESS
                    : ToolResult.PASS;
        }
        if (!wire.removeCover(side, context.getPlayer())) {
            return ToolResult.PASS;
        }
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    private VoxelShape connectedShape(BlockState state) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (isConnected(state, direction)) {
                mask |= 1 << direction.ordinal();
            }
        }
        VoxelShape[] shapes = kind.insulated() ? SHAPES_INSULATED : SHAPES_BARE;
        return shapes[mask];
    }

    private static VoxelShape[] buildShapes(double minimum, double maximum) {
        VoxelShape core = box(
                minimum, minimum, minimum, maximum, maximum, maximum);
        VoxelShape[] arms = new VoxelShape[Direction.values().length];
        arms[Direction.DOWN.ordinal()] = box(
                minimum, 0.0D, minimum, maximum, minimum, maximum);
        arms[Direction.UP.ordinal()] = box(
                minimum, maximum, minimum, maximum, 16.0D, maximum);
        arms[Direction.NORTH.ordinal()] = box(
                minimum, minimum, 0.0D, maximum, maximum, minimum);
        arms[Direction.SOUTH.ordinal()] = box(
                minimum, minimum, maximum, maximum, maximum, 16.0D);
        arms[Direction.WEST.ordinal()] = box(
                0.0D, minimum, minimum, minimum, maximum, maximum);
        arms[Direction.EAST.ordinal()] = box(
                maximum, minimum, minimum, 16.0D, maximum, maximum);
        VoxelShape[] result = new VoxelShape[64];
        for (int mask = 0; mask < result.length; mask++) {
            VoxelShape shape = core;
            for (Direction direction : Direction.values()) {
                if ((mask & 1 << direction.ordinal()) != 0) {
                    shape = Shapes.or(shape, arms[direction.ordinal()]);
                }
            }
            result[mask] = shape;
        }
        return result;
    }
}
