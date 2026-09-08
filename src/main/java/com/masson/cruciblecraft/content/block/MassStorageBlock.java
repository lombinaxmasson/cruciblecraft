package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
        if (level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage) {
            if (!level.isClientSide) {
                storage.playerInsertOrExtract(player, stack);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (action != ToolAction.CROWBAR) {
            return ToolResult.PASS;
        }
        return pickUp(context);
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
