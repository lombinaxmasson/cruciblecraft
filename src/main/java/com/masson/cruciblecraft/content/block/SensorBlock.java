package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.SensorBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.content.sensor.SensorMode;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 sensor: 2-pixel slab, display {@code FACING}, probe stored on the BE.
 * Covers cannot attach because this is not a pipe. Weak redstone only.
 */
public final class SensorBlock extends Block
        implements EntityBlock, ToolInteractable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape NORTH = Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SOUTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0);
    private static final VoxelShape WEST = Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape EAST = Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 16.0);
    private static final VoxelShape UP = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    private static final VoxelShape DOWN = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);

    private final SensorKind kind;

    public SensorBlock(SensorKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public SensorKind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
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
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
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
        if (!(level.getBlockEntity(pos) instanceof SensorBlockEntity sensor)) {
            return 0;
        }
        if (direction == sensor.probe().getOpposite()) {
            return 0;
        }
        return sensor.redstone();
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return 0;
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (hit.getDirection() != state.getValue(FACING)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof SensorBlockEntity sensor
                && sensor.mode().usesSetNumber()) {
            int delta = thresholdAdjustment(
                    pos,
                    hit.getDirection(),
                    hit.getLocation(),
                    sensor.hexadecimal());
            if (delta != 0) {
                sensor.adjustSetNumber(delta);
            }
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
        return ToolClick.useItemOn(stack, level, player, hand, hit);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof SensorBlockEntity sensor)) {
            return ToolResult.PASS;
        }
        Player player = context.getPlayer();
        if (action == ToolAction.WRENCH) {
            Direction facing = context.getClickedFace();
            if (!level.isClientSide) {
                level.setBlock(
                        pos,
                        level.getBlockState(pos).setValue(FACING, facing),
                        Block.UPDATE_ALL);
                sensor.setProbe(facing.getOpposite());
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.MONKEY_WRENCH) {
            Direction probe = context.getClickedFace();
            if (probe == level.getBlockState(pos).getValue(FACING)) {
                return ToolResult.PASS;
            }
            if (!level.isClientSide) {
                sensor.setProbe(probe);
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SCREWDRIVER) {
            if (context.getClickedFace()
                    != level.getBlockState(pos).getValue(FACING)) {
                return ToolResult.PASS;
            }
            if (!level.isClientSide) {
                boolean display = isDisplayHit(
                        pos,
                        context.getClickedFace(),
                        context.getClickLocation());
                if (display) {
                    sensor.toggleHexadecimal();
                    if (player != null) {
                        player.displayClientMessage(
                                Component.literal(
                                        sensor.hexadecimal()
                                                ? "Hexadecimal"
                                                : "Decimal"),
                                true);
                    }
                } else {
                    int delta = averagingAdjustment(
                            pos,
                            context.getClickedFace(),
                            context.getClickLocation(),
                            sensor.hexadecimal());
                    if (delta != 0) {
                        sensor.adjustAverageWindow(delta);
                        if (player != null) {
                            player.displayClientMessage(
                                    Component.literal(
                                            sensor.averageWindow() < 2
                                                    ? "Averaging disabled"
                                                    : "Averaging over "
                                                            + sensor.averageWindow()
                                                            + " values"),
                                    true);
                        }
                    } else {
                        SensorMode mode = sensor.cycleMode();
                        if (player != null) {
                            player.displayClientMessage(
                                    Component.translatable(
                                            "message.cruciblecraft.sensor.mode."
                                                    + mode.name().toLowerCase()),
                                    true);
                        }
                    }
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (!level.isClientSide) {
                sensor.reset();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    /**
     * Returns the threshold-button delta using GT6's 16x16 front-face
     * coordinates. Rows are -/+100, -/+10, and -/+1 in decimal mode; the
     * hexadecimal equivalents are -/+256, -/+16, and -/+1.
     */
    public static int thresholdAdjustment(
            BlockPos pos,
            Direction face,
            Vec3 hit,
            boolean hexadecimal) {
        return buttonAdjustment(pos, face, hit, hexadecimal);
    }

    /**
     * Returns the change to GT6's sliding-average window for a screwdriver
     * click on the same six threshold buttons. The decimal steps are
     * 100/10/1; hexadecimal steps are 256/16/1.
     */
    public static int averagingAdjustment(
            BlockPos pos,
            Direction face,
            Vec3 hit,
            boolean hexadecimal) {
        return buttonAdjustment(pos, face, hit, hexadecimal);
    }

    private static int buttonAdjustment(
            BlockPos pos,
            Direction face,
            Vec3 hit,
            boolean hexadecimal) {
        double[] coordinates = faceCoordinates(pos, face, hit);
        int x = (int) Math.floor(coordinates[0] * 16.0);
        int y = (int) Math.floor(coordinates[1] * 16.0);
        if (x < 9 || x > 14 || y < 6 || y > 14) {
            return 0;
        }
        int step = y <= 8
                ? (hexadecimal ? 256 : 100)
                : y <= 11
                        ? (hexadecimal ? 16 : 10)
                        : 1;
        return x <= 11 ? -step : step;
    }

    /**
     * GT6's display occupies pixels 2..14 by 2..4 on the front face.
     */
    public static boolean isDisplayHit(
            BlockPos pos,
            Direction face,
            Vec3 hit) {
        double[] coordinates = faceCoordinates(pos, face, hit);
        return coordinates[0] >= 2.0 / 16.0
                && coordinates[0] <= 14.0 / 16.0
                && coordinates[1] >= 2.0 / 16.0
                && coordinates[1] <= 4.0 / 16.0;
    }

    private static double[] faceCoordinates(
            BlockPos pos,
            Direction face,
            Vec3 hit) {
        double x = hit.x - pos.getX();
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ();
        return switch (face) {
            case DOWN -> new double[] {x, 1.0 - z};
            case UP -> new double[] {x, z};
            case NORTH -> new double[] {1.0 - x, 1.0 - y};
            case SOUTH -> new double[] {x, 1.0 - y};
            case WEST -> new double[] {z, 1.0 - y};
            case EAST -> new double[] {1.0 - z, 1.0 - y};
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SensorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.SENSOR.get()
                ? (current, pos, currentState, blockEntity) ->
                        SensorBlockEntity.serverTick(
                                current,
                                pos,
                                currentState,
                                (SensorBlockEntity) blockEntity)
                : null;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            notifyNeighbors(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void notifyNeighbors(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        level.updateNeighborsAt(pos, block);
        for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), block);
        }
    }
}
