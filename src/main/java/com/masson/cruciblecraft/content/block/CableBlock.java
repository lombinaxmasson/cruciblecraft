package com.masson.cruciblecraft.content.block;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.energy.cable.CableCovers;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverPlacement;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.ElectricalProperties;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One immutable conductor with six derived EU/LU connection bits. */
public final class CableBlock extends Block
        implements EntityBlock, ToolInteractable {
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
    private final ElectricalProperties transportProperties;
    private final boolean luFiber;
    private final VoxelShape[] shapes;

    public CableBlock(
            ElectricalConductorCatalog.Entry conductor,
            Properties properties) {
        this(
                conductor,
                properties,
                conductor.electrical(),
                false,
                ElectricalConductorCatalog.widthPixels(
                        conductor.sourceSpecification()));
    }

    /** Creates the source-backed, lossless LU fiber wire. */
    public static CableBlock luFiber(Properties properties) {
        return new CableBlock(
                null,
                properties,
                new ElectricalProperties(
                        Long.MAX_VALUE, Long.MAX_VALUE, 0L, true, false),
                true,
                2);
    }

    private CableBlock(
            ElectricalConductorCatalog.Entry conductor,
            Properties properties,
            ElectricalProperties transportProperties,
            boolean luFiber,
            int width) {
        super(properties);
        this.conductor = conductor;
        this.transportProperties = transportProperties;
        this.luFiber = luFiber;
        this.shapes = shapesForWidth(width);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    public ElectricalConductorCatalog.Entry conductor() {
        if (conductor == null) {
            throw new IllegalStateException(
                    "LU fiber has no material conductor identity");
        }
        return conductor;
    }

    public ElectricalProperties transportProperties() {
        return transportProperties;
    }

    public boolean supports(EnergyType type) {
        return luFiber
                ? type == EnergyType.LU
                : type == EnergyType.ELECTRIC;
    }

    public boolean isLuFiber() {
        return luFiber;
    }

    public boolean bareWire() {
        return !luFiber && conductor.bareWire();
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
        if (level.getBlockState(neighborPos).getBlock()
                instanceof CableBlock neighbor
                && ((supports(EnergyType.ELECTRIC)
                                && neighbor.supports(EnergyType.ELECTRIC))
                        || (supports(EnergyType.LU)
                                && neighbor.supports(EnergyType.LU)))) {
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
                && ((supports(EnergyType.ELECTRIC)
                                && handler.handles(
                                        EnergyType.ELECTRIC,
                                        direction.getOpposite()))
                        || (supports(EnergyType.LU)
                                && handler.handles(
                                        EnergyType.LU,
                                        direction.getOpposite())));
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
        if (CableCovers.tryInstall(level, pos, hit, stack, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable
                && CableCovers.onRightClick(cable, hit, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return tool;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            if (action == ToolAction.CROWBAR) {
                return pryCover(context);
            }
            if (CableCovers.onTool(
                    cable,
                    CoverPlacement.interactSide(
                            cable, ToolClick.hit(context)),
                    action)) {
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
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable
                && CableCovers.onRightClick(cable, hit, player)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
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
                state, context, connectedShape(state), level.getBlockEntity(pos));
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
        return coverOutput(level, pos, direction, false);
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return coverOutput(level, pos, direction, true);
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction) {
        if (direction == null) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return false;
        }
        return CableCovers.connectsRedstone(cable, direction);
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
                && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            CableCovers.tickOutputs(cable);
        }
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
                && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.dropCovers();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static int coverOutput(
            BlockGetter level,
            BlockPos pos,
            Direction direction,
            boolean strong) {
        Direction face = direction.getOpposite();
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return 0;
        }
        int cover = strong
                ? CableCovers.strongOut(cable, face)
                : CableCovers.weakOut(cable, face);
        return Math.max(0, cover);
    }

    private static ToolResult pryCover(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return ToolResult.PASS;
        }
        Direction side = CoverPlacement.interactSide(
                cable, ToolClick.hit(context));
        if (level.isClientSide) {
            return cable.covers().get(side).isPresent()
                    ? ToolResult.SUCCESS
                    : ToolResult.PASS;
        }
        if (!cable.removeCover(side, context.getPlayer())) {
            return ToolResult.PASS;
        }
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
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
