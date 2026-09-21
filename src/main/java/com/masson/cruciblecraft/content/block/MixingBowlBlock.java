package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.MixingBowlBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * GT6 ceramic mixing bowl. Single block, not a Large Mixer impersonation.
 */
public final class MixingBowlBlock extends Block
        implements EntityBlock, ToolInteractable {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 2.0, 8.0, 16.0),
            Block.box(14.0, 0.0, 0.0, 16.0, 8.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 2.0),
            Block.box(0.0, 0.0, 14.0, 16.0, 8.0, 16.0));

    public MixingBowlBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        if (!(level.getBlockEntity(pos) instanceof MixingBowlBlockEntity bowl)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemInteractionResult tool = ToolClick.useItemOn(
                stack, level, player, hand, hit);
        if (tool.consumesAction()) {
            return tool;
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, bowl.playerFluids())) {
            return ItemInteractionResult.SUCCESS;
        }
        if (hit.getDirection() == Direction.UP && bowl.insertFromHand(stack)) {
            return ItemInteractionResult.SUCCESS;
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
        if (!(level.getBlockEntity(pos) instanceof MixingBowlBlockEntity bowl)) {
            return InteractionResult.PASS;
        }
        if (hit.getDirection() == Direction.UP) {
            if (!level.isClientSide && bowl.mix(player, true)) {
                return InteractionResult.SUCCESS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
        }
        if (!level.isClientSide && bowl.extract(player)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof MixingBowlBlockEntity bowl)) {
            return ToolResult.PASS;
        }
        if (action == ToolAction.MIXER) {
            if (context.getClickedFace() != Direction.UP) {
                return ToolResult.PASS;
            }
            if (level.isClientSide) {
                return ToolResult.SUCCESS;
            }
            if (!bowl.mix(context.getPlayer(), false)) {
                return ToolResult.PASS;
            }
            ToolClick.hurt(context);
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.PLUNGER) {
            boolean emptied = !level.isClientSide && bowl.plunger();
            if (level.isClientSide) {
                return ToolResult.SUCCESS;
            }
            return ToolClick.plunger(context, emptied);
        }
        if (action == ToolAction.MAGNIFYING_GLASS) {
            if (!level.isClientSide) {
                for (var line : bowl.magnifyingInspect(context)) {
                    if (context.getPlayer() != null) {
                        context.getPlayer().displayClientMessage(line, false);
                    }
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState next,
            boolean moved) {
        if (state.getBlock() != next.getBlock()
                && level.getBlockEntity(pos) instanceof MixingBowlBlockEntity bowl) {
            bowl.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixingBowlBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.MIXING_BOWL.get()
                ? (current, pos, currentState, blockEntity) ->
                        MixingBowlBlockEntity.serverTick(
                                current,
                                pos,
                                currentState,
                                (MixingBowlBlockEntity) blockEntity)
                : null;
    }
}
