package com.masson.cruciblecraft.content.block;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One immutable material/form conductor with six derived connection bits. */
public final class CableBlock extends Block implements EntityBlock {
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
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

    private final ElectricalConductorCatalog.Entry conductor;
    private final VoxelShape[] shapes;

    public CableBlock(
            ElectricalConductorCatalog.Entry conductor,
            Properties properties) {
        super(properties);
        this.conductor = conductor;
        int width = conductor.bareWire()
                ? 2
                : switch (conductor.sourceSpecification()) {
                    case "cableGt01" -> 4;
                    case "cableGt02" -> 6;
                    case "cableGt04" -> 8;
                    case "cableGt08" -> 10;
                    case "cableGt12" -> 12;
                    default -> throw new IllegalArgumentException(
                            "Unsupported conductor specification "
                                    + conductor.sourceSpecification());
                };
        this.shapes = shapesForWidth(width);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    public ElectricalConductorCatalog.Entry conductor() {
        return conductor;
    }

    public static boolean isConnected(BlockState state, Direction direction) {
        return state.getBlock() instanceof CableBlock
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

    boolean connectsTo(
            LevelAccessor level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        if (!level.hasChunkAt(neighborPos)) {
            return false;
        }
        if (level.getBlockState(neighborPos).getBlock() instanceof CableBlock) {
            return true;
        }
        if (!(level instanceof Level world)) {
            return false;
        }
        var handler = world.getCapability(
                ModCapabilities.ENERGY,
                neighborPos,
                direction.getOpposite());
        return handler != null
                && handler.handles(EnergyType.ELECTRIC, direction.getOpposite());
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
        return Gt6StyleConnections.wrench(
                stack, state, level, pos, player, hand, hit);
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
                width, CableBlock::buildShapes);
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
        VoxelShape[] arms = new VoxelShape[Direction.values().length];
        for (Direction direction : Direction.values()) {
            arms[direction.ordinal()] = arm(
                    direction, minimum, maximum);
        }
        VoxelShape[] result = new VoxelShape[64];
        for (int mask = 0; mask < result.length; mask++) {
            VoxelShape shape = core;
            for (Direction direction : Direction.values()) {
                if ((mask & 1 << direction.ordinal()) != 0) {
                    shape = Shapes.or(
                            shape, arms[direction.ordinal()]);
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

    @Override
    protected void entityInside(
            BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!level.isClientSide
                && level.getBlockEntity(pos)
                        instanceof CableBlockEntity cable) {
            cable.applyContactDamage(entity);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.CABLE.get()
                ? (currentLevel, pos, currentState, blockEntity) ->
                        CableBlockEntity.serverTick(
                                currentLevel,
                                pos,
                                currentState,
                                (CableBlockEntity) blockEntity)
                : null;
    }
}
