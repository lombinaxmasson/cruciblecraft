package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.GtTreeHoleBlockEntity;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.worldgen.tree.GtTreeHoleTracker;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Tapped tree hole. Not in the creative tab; drops the matching log.
 */
public final class GtTreeHoleBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HAS_PRODUCT = BooleanProperty.create("has_product");

    private final GtTreeSpecies species;

    public GtTreeHoleBlock(GtTreeSpecies species, Properties properties) {
        super(properties);
        this.species = species;
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(HAS_PRODUCT, Boolean.FALSE));
    }

    public GtTreeSpecies species() {
        return species;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_PRODUCT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            face = context.getHorizontalDirection().getOpposite();
        }
        return defaultBlockState().setValue(FACING, face).setValue(HAS_PRODUCT, Boolean.FALSE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GtTreeHoleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.TREE_HOLE.get()) {
            return null;
        }
        return (lvl, blockPos, blockState, blockEntity) ->
                GtTreeHoleBlockEntity.serverTick(
                        lvl, blockPos, blockState, (GtTreeHoleBlockEntity) blockEntity);
    }

    @Override
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (species == GtTreeSpecies.RUBBER && !level.isClientSide) {
            GtTreeHoleTracker.add(level, pos);
        }
    }

    @Override
    protected void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (species == GtTreeSpecies.RUBBER && !state.is(newState.getBlock())) {
            GtTreeHoleTracker.remove(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.treeLog(species).get());
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
        if (hit.getDirection() != state.getValue(FACING)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof BlockItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof GtTreeHoleBlockEntity hole)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (hole.hasProduct() && FluidUtil.interactWithFluidHandler(player, hand, hole.fluids())) {
            hole.extractProduct();
            return ItemInteractionResult.CONSUME;
        }
        if (hole.harvestItem(player)) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (hit.getDirection() != state.getValue(FACING)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof GtTreeHoleBlockEntity hole)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (hole.harvestItem(player)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}
