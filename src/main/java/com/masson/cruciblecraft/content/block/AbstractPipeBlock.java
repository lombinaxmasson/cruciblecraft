package com.masson.cruciblecraft.content.block;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuWriteback;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeTopology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Compact six-direction connection state shared by item and fluid pipes. */
public abstract class AbstractPipeBlock extends Block
        implements EntityBlock, ToolInteractable {
    public static final BooleanProperty DOWN =
            BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH =
            BooleanProperty.create("north");
    public static final BooleanProperty SOUTH =
            BooleanProperty.create("south");
    public static final BooleanProperty WEST =
            BooleanProperty.create("west");
    public static final BooleanProperty EAST =
            BooleanProperty.create("east");
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION;
    private static final Map<Integer, VoxelShape[]> SHAPES_BY_WIDTH =
            new HashMap<>();

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

    private final PipeCatalog.Entry pipe;
    private final VoxelShape[] shapes;

    protected AbstractPipeBlock(
            PipeCatalog.Entry pipe, Properties properties) {
        super(properties);
        this.pipe = java.util.Objects.requireNonNull(pipe, "pipe");
        this.shapes = shapesForWidth(pipe.width());
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    public final PipeCatalog.Entry pipe() {
        return pipe;
    }

    public static boolean isConnected(
            BlockState state, Direction direction) {
        return state.getBlock() instanceof AbstractPipeBlock
                && state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        return state;
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
        return switch (action) {
            case PLUNGER -> plunger(context);
            case CROWBAR -> pryCover(context);
            case WRENCH -> Gt6StyleConnections.toggleConnection(
                    context.getLevel(),
                    context.getClickedPos(),
                    ToolClick.hit(context));
            default -> ToolResult.PASS;
        };
    }

    private static ToolResult plunger(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidPipeBlockEntity pipe) {
            if (level.isClientSide) {
                return ToolResult.SUCCESS;
            }
            if (!pipe.trashContents()) {
                return ToolResult.REJECT;
            }
            ToolClick.hurt(context);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.BUCKET_EMPTY,
                    SoundSource.BLOCKS,
                    0.6F,
                    0.6F);
            return ToolResult.SUCCESS;
        }
        if (be instanceof ItemPipeBlockEntity pipe) {
            if (level.isClientSide) {
                return ToolResult.SUCCESS;
            }
            if (!pipe.ejectRecovery()) {
                return ToolResult.REJECT;
            }
            ToolClick.hurt(context);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS,
                    0.6F,
                    0.6F);
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    private static ToolResult pryCover(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        Direction side = Gt6StyleConnections.sideFromHit(ToolClick.hit(context));
        if (be instanceof ItemPipeBlockEntity pipe) {
            if (level.isClientSide) {
                return ToolResult.SUCCESS;
            }
            if (!pipe.removeCover(side, context.getPlayer())) {
                return ToolResult.PASS;
            }
            ToolClick.hurt(context);
            return ToolResult.SUCCESS;
        }
        if (be instanceof FluidPipeBlockEntity pipe) {
            if (level.isClientSide) {
                return ToolResult.SUCCESS;
            }
            if (!pipe.removeCover(side, context.getPlayer())) {
                return ToolResult.PASS;
            }
            ToolClick.hurt(context);
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            PipeTopology.invalidate(level, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Direction face = hit.getDirection();
        if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe
                && pipe.removeCover(face, player)) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe
                && pipe.removeCover(face, player)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!level.isClientSide
                && state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ItemPipeBlockEntity itemPipe) {
                itemPipe.dropCovers();
            } else if (be instanceof FluidPipeBlockEntity fluidPipe) {
                fluidPipe.dropCovers();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide
                && state.getBlock() != newState.getBlock()) {
            PipeTopology.invalidate(level, pos);
        }
    }

    protected abstract boolean connectsToEndpoint(
            Level level, BlockPos neighborPos, Direction neighborSide);

    boolean connectsTo(
            LevelAccessor level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        if (!level.hasChunkAt(neighborPos)) {
            return false;
        }
        if (level.getBlockState(neighborPos).getBlock()
                instanceof AbstractPipeBlock neighbor) {
            return neighbor.pipe.kind() == pipe.kind();
        }
        return level instanceof Level world
                && connectsToEndpoint(
                        world, neighborPos, direction.getOpposite());
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
        return DisplayCpuWriteback.redstoneOut(
                level.getBlockEntity(pos), direction);
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction) {
        if (direction == null) {
            return DisplayCpuWriteback.hasAnyDisplay(
                    level.getBlockEntity(pos));
        }
        return DisplayCpuWriteback.hasDisplay(
                level.getBlockEntity(pos), direction);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
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
        return connectedShape(state);
    }

    private VoxelShape connectedShape(BlockState state) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (isConnected(state, direction)) {
                mask |= 1 << direction.ordinal();
            }
        }
        return shapes[mask];
    }

    private static synchronized VoxelShape[] shapesForWidth(int width) {
        return SHAPES_BY_WIDTH.computeIfAbsent(
                width, AbstractPipeBlock::buildShapes);
    }

    private static VoxelShape[] buildShapes(int width) {
        double minimum = 8.0D - width / 2.0D;
        double maximum = 8.0D + width / 2.0D;
        VoxelShape core = box(
                minimum,
                minimum,
                minimum,
                maximum,
                maximum,
                maximum);
        VoxelShape[] result = new VoxelShape[64];
        for (int mask = 0; mask < result.length; mask++) {
            VoxelShape shape = core;
            for (Direction direction : Direction.values()) {
                if ((mask & 1 << direction.ordinal()) != 0) {
                    shape = Shapes.or(
                            shape,
                            arm(
                                    direction,
                                    minimum,
                                    maximum));
                }
            }
            result[mask] = shape;
        }
        return result;
    }

    private static VoxelShape arm(
            Direction direction, double minimum, double maximum) {
        return switch (direction) {
            case DOWN -> box(
                    minimum, 0.0D, minimum,
                    maximum, minimum, maximum);
            case UP -> box(
                    minimum, maximum, minimum,
                    maximum, 16.0D, maximum);
            case NORTH -> box(
                    minimum, minimum, 0.0D,
                    maximum, maximum, minimum);
            case SOUTH -> box(
                    minimum, minimum, maximum,
                    maximum, maximum, 16.0D);
            case WEST -> box(
                    0.0D, minimum, minimum,
                    minimum, maximum, maximum);
            case EAST -> box(
                    maximum, minimum, minimum,
                    16.0D, maximum, maximum);
        };
    }
}
