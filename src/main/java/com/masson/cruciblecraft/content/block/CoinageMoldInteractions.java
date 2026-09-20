package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.CoinageMoldBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 coinage mold click / hammer. Shape selection is still a no-op there. */
public final class CoinageMoldInteractions {
    private CoinageMoldInteractions() {}

    public static ToolResult useTool(ToolAction action, UseOnContext context) {
        if (action != ToolAction.HAMMER || context.getClickedFace() != Direction.UP) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof CoinageMoldBlockEntity mold)) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        if (!mold.stamp()) {
            return ToolResult.PASS;
        }
        ToolClick.hurt(context);
        level.playSound(
                null,
                context.getClickedPos(),
                SoundEvents.ANVIL_USE,
                SoundSource.BLOCKS,
                0.6F,
                1.4F);
        return ToolResult.SUCCESS;
    }

    public static ItemInteractionResult useItemOn(
            ItemStack stack,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CoinageMoldBlockEntity mold)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Direction face = hit.getDirection();
        if (face != Direction.UP && !face.getAxis().isHorizontal()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (mold.contents().isEmpty()) {
            if (!CoinageMoldBlockEntity.isBlankTinyPlate(stack) || !mold.insertBlank(stack)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(
                    null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4F, 1.0F);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static InteractionResult useWithoutItem(
            Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CoinageMoldBlockEntity mold)) {
            return InteractionResult.PASS;
        }
        Direction face = hit.getDirection();
        if (face != Direction.UP && !face.getAxis().isHorizontal()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack taken = mold.takeContents();
        if (taken.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!player.addItem(taken)) {
            player.drop(taken, false);
        }
        return InteractionResult.SUCCESS;
    }
}
