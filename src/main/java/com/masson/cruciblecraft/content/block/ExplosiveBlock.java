package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.ExplosiveBlockEntity;
import com.masson.cruciblecraft.content.explosive.DynamiteType;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.RandomSource;

/**
 * A GT6 dynamite stick represented as a facing, ticking block entity.
 *
 * <p>The block itself stays deliberately light and non-solid. Fuse state,
 * sunk placement, and the variant's blast parameters live in the block
 * entity, just as they do in GT6's shared MultiTileEntityDynamite.</p>
 */
public final class ExplosiveBlock extends Block
        implements EntityBlock, ToolInteractable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final DynamiteType type;

    public ExplosiveBlock(DynamiteType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    public DynamiteType type() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING, context.getClickedFace());
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
        boolean sunk = level.getBlockEntity(pos) instanceof ExplosiveBlockEntity explosive
                && explosive.isSunk();
        return shape(state.getValue(FACING), sunk);
    }

    private static VoxelShape shape(Direction facing, boolean sunk) {
        if (sunk) {
            return switch (facing) {
                case NORTH -> Block.box(5.0D, 5.0D, 14.0D, 11.0D, 11.0D, 16.0D);
                case SOUTH -> Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 2.0D);
                case WEST -> Block.box(14.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);
                case EAST -> Block.box(0.0D, 5.0D, 5.0D, 2.0D, 11.0D, 11.0D);
                case DOWN -> Block.box(5.0D, 14.0D, 5.0D, 11.0D, 16.0D, 11.0D);
                case UP -> Block.box(5.0D, 0.0D, 5.0D, 11.0D, 2.0D, 11.0D);
            };
        }
        return switch (facing) {
            case NORTH, SOUTH -> Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 16.0D);
            case WEST, EAST -> Block.box(0.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);
            case DOWN, UP -> Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExplosiveBlockEntity(pos, state);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof ExplosiveBlockEntity explosive) {
            explosive.explodeInstant();
            return;
        }
        super.wasExploded(level, pos, explosion);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.DYNAMITE.get()
                ? (world, pos, blockState, blockEntity) ->
                        ExplosiveBlockEntity.serverTick(
                                world,
                                pos,
                                blockState,
                                (ExplosiveBlockEntity) blockEntity)
                : null;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (action != ToolAction.IGNITER
                || !(context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof ExplosiveBlockEntity explosive)) {
            return ToolResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return ToolResult.SUCCESS;
        }
        if (!explosive.ignite()) {
            return ToolResult.PASS;
        }
        ToolClick.hurt(context);
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
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        triggerShortFuse(level, pos);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random) {
        triggerShortFuse(level, pos);
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
        triggerShortFuse(level, pos);
    }

    private static void triggerShortFuse(Level level, BlockPos pos) {
        if (level.isClientSide
                || !(level.getBlockEntity(pos) instanceof ExplosiveBlockEntity explosive)
                || explosive.isLit()) {
            return;
        }
        boolean fireNearby = level.getBlockState(pos).is(BlockTags.FIRE);
        for (Direction direction : Direction.values()) {
            fireNearby |= level.getBlockState(pos.relative(direction)).is(BlockTags.FIRE);
        }
        if (level.hasNeighborSignal(pos) || fireNearby) {
            explosive.remoteActivate();
        }
    }
}
