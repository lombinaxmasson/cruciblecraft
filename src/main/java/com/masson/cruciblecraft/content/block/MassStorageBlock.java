package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class MassStorageBlock extends StorageHostBlock
        implements ToolInteractable {
    public MassStorageBlock(StorageVariant variant, Properties properties) {
        super(variant, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MassStorageBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTicker(
                level,
                type,
                ModBlockEntities.MASS_STORAGE.get(),
                MassStorageBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (coverClick(level, pos, player, hit)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage)) {
            return InteractionResult.PASS;
        }
        if (hit.getDirection() != state.getValue(FACING)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            storage.onActivated(player, ItemStack.EMPTY, hit);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
        if (level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage
                && hit.getDirection() == state.getValue(FACING)) {
            if (!level.isClientSide) {
                storage.onActivated(player, stack, hit);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        if (level.getBlockEntity(context.getClickedPos())
                instanceof MachineCoverHost machine) {
            ToolResult coverResult = MachineCoverBlockInteraction.useTool(
                    machine, action, context);
            if (coverResult != ToolResult.PASS) {
                return coverResult;
            }
        }
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof MassStorageBlockEntity storage)) {
            return ToolResult.PASS;
        }
        if (action == ToolAction.CROWBAR) {
            return pickUp(context);
        }
        if (action == ToolAction.WRENCH) {
            Direction target = Gt6StyleConnections.sideFromHit(ToolClick.hit(context));
            if (!target.getAxis().isHorizontal()) {
                return ToolResult.PASS;
            }
            BlockState state = level.getBlockState(context.getClickedPos());
            if (!level.isClientSide) {
                if (state.getValue(FACING) != target) {
                    level.setBlock(
                            context.getClickedPos(),
                            state.setValue(FACING, target),
                            Block.UPDATE_ALL);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.PINCERS) {
            if (!level.isClientSide && context.getPlayer() != null) {
                storage.giveToPlayer(context.getPlayer());
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (!level.isClientSide) {
                storage.dumpInFront();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SCREWDRIVER) {
            if (!level.isClientSide) {
                storage.toggleResetFilterWhenEmpty();
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            storage.filterMessage(), true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.MONKEY_WRENCH) {
            if (!level.isClientSide) {
                storage.toggleAutoOutput();
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            storage.autoOutputMessage(), true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.WIRE_CUTTER) {
            if (!level.isClientSide) {
                storage.toggleOverflow();
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            storage.overflowMessage(), true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    private static ToolResult pickUp(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage)) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        BlockState state = level.getBlockState(pos);
        ItemStack packed = new ItemStack(state.getBlock());
        storage.saveToItem(packed, level.registryAccess());
        storage.clearContents();
        level.removeBlock(pos, false);
        Player player = context.getPlayer();
        if (player == null || !player.addItem(packed)) {
            Block.popResource(level, pos, packed);
        }
        ToolClick.hurt(context);
        level.playSound(
                null,
                pos,
                SoundEvents.WOOD_BREAK,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        return ToolResult.SUCCESS;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage) {
            storage.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
