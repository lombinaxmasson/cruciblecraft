package com.masson.cruciblecraft.energy.flux;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 flux converter. Not an {@code EnergyConverterHost}: Jade must not look
 * the identity up in the converter catalog.
 */
public final class FluxBlock extends Block
        implements EntityBlock, ToolInteractable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final FluxProfile profile;

    public FluxBlock(FluxProfile profile, Properties properties) {
        super(properties);
        this.profile = profile;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    public FluxProfile profile() {
        return profile;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING, context.getNearestLookingDirection().getOpposite());
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        if (!profile.heater()) {
            return Shapes.block();
        }
        return switch (state.getValue(FACING)) {
            case DOWN -> Block.box(0.0, 2.0, 0.0, 16.0, 16.0, 16.0);
            case UP -> Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);
            case NORTH -> Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 16.0);
            case SOUTH -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 14.0);
            case WEST -> Block.box(2.0, 0.0, 0.0, 16.0, 16.0, 16.0);
            case EAST -> Block.box(0.0, 0.0, 0.0, 14.0, 16.0, 16.0);
        };
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!level.isClientSide
                && profile.heater()
                && level.getBlockEntity(pos) instanceof FluxBlockEntity flux) {
            flux.applyContactDamage(entity);
        }
    }

    @Override
    public void stepOn(
            Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide
                && profile.motor()
                && state.getValue(FACING) == Direction.UP
                && entity instanceof LivingEntity living
                && level.getBlockEntity(pos) instanceof FluxBlockEntity flux) {
            flux.applyWalkOver(living);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (action == ToolAction.WRENCH) {
            Direction next = context.getClickedFace();
            if (state.getValue(FACING) == next) {
                return ToolResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(FACING, next), Block.UPDATE_ALL);
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof FluxBlockEntity flux)) {
            return ToolResult.PASS;
        }
        if (action == ToolAction.SCREWDRIVER
                && (profile.converterMode() || profile.engine())) {
            if (!level.isClientSide) {
                flux.cycleMode(context.getPlayer() != null
                        && context.getPlayer().isShiftKeyDown());
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.MONKEY_WRENCH && profile.motor()) {
            if (!level.isClientSide) {
                flux.toggleReverse();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (!level.isClientSide) {
                flux.toggleStopped();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
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
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.FLUX_CONVERTER.get()
                ? (current, pos, currentState, blockEntity) ->
                        FluxBlockEntity.serverTick(
                                current,
                                pos,
                                currentState,
                                (FluxBlockEntity) blockEntity)
                : null;
    }
}
