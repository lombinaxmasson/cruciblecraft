package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Face-placed GT6 MTE host. Attachments sit on the clicked face; this is not
 * a {@code PipeCover}.
 */
public final class MteInPlaceBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape DOWN = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    private static final VoxelShape UP = Block.box(4.0, 8.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape NORTH = Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0);
    private static final VoxelShape SOUTH = Block.box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0);
    private static final VoxelShape WEST = Block.box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0);
    private static final VoxelShape EAST = Block.box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape PANEL = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
    private static final VoxelShape ROPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    private final MteInPlaceSpec spec;

    public MteInPlaceBlock(MteInPlaceSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public MteInPlaceSpec spec() {
        return spec;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction facing = spec.kind().attachment()
                ? clicked.getOpposite()
                : context.getHorizontalDirection().getOpposite();
        if (!spec.kind().attachment() && facing.getAxis().isVertical()) {
            facing = context.getHorizontalDirection().getOpposite();
        }
        return defaultBlockState().setValue(FACING, facing);
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
        MteInPlaceKind kind = spec.kind();
        if (kind == MteInPlaceKind.ROPE) {
            return ROPE;
        }
        if (kind == MteInPlaceKind.WOOD_PANEL) {
            return PANEL;
        }
        if (!kind.attachment()) {
            return super.getShape(state, level, pos, context);
        }
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
            host.transferOnce();
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState next,
            boolean moved) {
        if (!state.is(next.getBlock())
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
            host.dropContents();
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MteInPlaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.MTE_INPLACE.get()
                ? (lvl, pos, st, be) -> MteInPlaceBlockEntity.serverTick(
                        lvl, pos, st, (MteInPlaceBlockEntity) be)
                : null;
    }
}
