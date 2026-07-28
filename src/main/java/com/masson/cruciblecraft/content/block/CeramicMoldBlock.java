package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.item.CeramicMoldBlockItem;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public final class CeramicMoldBlock extends Block implements EntityBlock {
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 5.0, 14.0);

    public CeramicMoldBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILLED, false));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold)) {
            return InteractionResult.PASS;
        }

        ItemStack output = mold.takeOutput();
        if (!output.isEmpty()) {
            if (!player.addItem(output)) {
                player.drop(output, false);
            }
            return InteractionResult.CONSUME;
        }
        if (mold.isFilled()) {
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.mold_cooling", Math.round(mold.temperature())),
                    true);
            return InteractionResult.CONSUME;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof CrucibleBlockEntity crucible) {
                var transfer = crucible.cast(mold.shape().form());
                if (transfer.isPresent()) {
                    mold.fill(transfer.get());
                    player.displayClientMessage(
                            Component.translatable("message.cruciblecraft.mold_filled"),
                            true);
                    return InteractionResult.CONSUME;
                }
            }
        }
        player.displayClientMessage(
                Component.translatable("message.cruciblecraft.mold_no_molten_material"),
                true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.getItem() instanceof CeramicMoldBlockItem item
                && level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold) {
            mold.setShape(item.shape());
        }
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold) {
            popResource(level, pos, mold.moldStack());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CeramicMoldBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CERAMIC_MOLD.get()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        BlockEntityTicker<T> ticker = (BlockEntityTicker<T>) (BlockEntityTicker<CeramicMoldBlockEntity>)
                CeramicMoldBlockEntity::serverTick;
        return ticker;
    }
}
