package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.GtBushBlockEntity;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code MultiTileEntityBush}. Independent plant runtime, not a cover.
 */
public final class GtBushBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final int MAX_STAGE = 3;
    private static final VoxelShape CORE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);
    private static final VoxelShape NORTH = Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 4.0);
    private static final VoxelShape SOUTH = Block.box(2.0, 2.0, 12.0, 14.0, 14.0, 16.0);
    private static final VoxelShape WEST = Block.box(0.0, 2.0, 2.0, 4.0, 14.0, 14.0);
    private static final VoxelShape EAST = Block.box(12.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    private static final VoxelShape UP = Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape DOWN = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);

    public GtBushBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(0.2F)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY));
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.DOWN)
                        .setValue(STAGE, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STAGE);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace().getOpposite();
        return defaultBlockState().setValue(FACING, face).setValue(STAGE, 0);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        if (facing == Direction.DOWN) {
            return plantable(level.getBlockState(pos.below()));
        }
        BlockState host = level.getBlockState(pos.relative(facing));
        return host.getBlock() instanceof GtBushBlock
                && host.getValue(FACING) == Direction.DOWN;
    }

    public static boolean plantable(BlockState contact) {
        return contact.is(BlockTags.DIRT);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GtBushBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.GT_BUSH.get()) {
            return null;
        }
        return (lvl, pos, st, be) ->
                GtBushBlockEntity.serverTick(lvl, pos, st, (GtBushBlockEntity) be);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            net.minecraft.world.InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GtBushBlockEntity bush)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (bush.berry().isEmpty() && GtBushBlockEntity.isBerrySetter(stack)) {
            if (!level.isClientSide) {
                bush.setBerry(stack);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!bush.berry().isEmpty() && bush.stage() >= MAX_STAGE) {
            if (!level.isClientSide) {
                bush.harvest(player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GtBushBlockEntity bush)) {
            return InteractionResult.PASS;
        }
        if (bush.berry().isEmpty() || bush.stage() < MAX_STAGE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            bush.harvest(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
