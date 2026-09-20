package com.masson.cruciblecraft.content.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;

/** Stack-safe copies of vanilla tool interactions using NeoForge abilities. */
final class VanillaToolUseOn {
    private static final int UPDATE_ALL_IMMEDIATE = 11;

    private VanillaToolUseOn() {}

    static InteractionResult axe(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        BlockState original = level.getBlockState(pos);
        BlockState modified = original.getToolModifiedState(
                context, ItemAbilities.AXE_STRIP, false);
        if (modified != null) {
            level.playSound(
                    player,
                    pos,
                    SoundEvents.AXE_STRIP,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        } else {
            modified = original.getToolModifiedState(
                    context, ItemAbilities.AXE_SCRAPE, false);
            if (modified != null) {
                soundAndParticle(
                        level,
                        pos,
                        player,
                        SoundEvents.AXE_SCRAPE,
                        3005);
            } else {
                modified = original.getToolModifiedState(
                        context, ItemAbilities.AXE_WAX_OFF, false);
                if (modified != null) {
                    soundAndParticle(
                            level,
                            pos,
                            player,
                            SoundEvents.AXE_WAX_OFF,
                            3004);
                }
            }
        }
        if (modified == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(
                    serverPlayer, pos, stack);
        }
        level.setBlock(pos, modified, UPDATE_ALL_IMMEDIATE);
        level.gameEvent(
                GameEvent.BLOCK_CHANGE,
                pos,
                GameEvent.Context.of(player, modified));
        hurt(stack, player, context);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static InteractionResult shovel(UseOnContext context) {
        if (context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState original = level.getBlockState(pos);
        Player player = context.getPlayer();

        BlockState modified = original.getToolModifiedState(
                context, ItemAbilities.SHOVEL_FLATTEN, false);
        if (modified != null && level.getBlockState(pos.above()).isAir()) {
            level.playSound(
                    player,
                    pos,
                    SoundEvents.SHOVEL_FLATTEN,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        } else {
            modified = original.getToolModifiedState(
                    context, ItemAbilities.SHOVEL_DOUSE, false);
            if (modified != null && !level.isClientSide()) {
                level.levelEvent(null, 1009, pos, 0);
            }
        }
        if (modified == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, modified, UPDATE_ALL_IMMEDIATE);
            level.gameEvent(
                    GameEvent.BLOCK_CHANGE,
                    pos,
                    GameEvent.Context.of(player, modified));
            hurt(context.getItemInHand(), player, context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static InteractionResult hoe(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState modified = level.getBlockState(pos).getToolModifiedState(
                context, ItemAbilities.HOE_TILL, false);
        if (modified == null) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        level.playSound(
                player,
                pos,
                SoundEvents.HOE_TILL,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        if (!level.isClientSide) {
            level.setBlock(pos, modified, UPDATE_ALL_IMMEDIATE);
            level.gameEvent(
                    GameEvent.BLOCK_CHANGE,
                    pos,
                    GameEvent.Context.of(player, modified));
            hurt(context.getItemInHand(), player, context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void soundAndParticle(
            Level level,
            BlockPos pos,
            Player player,
            SoundEvent sound,
            int eventId) {
        level.playSound(
                player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.levelEvent(player, eventId, pos, 0);
    }

    private static void hurt(
            ItemStack stack,
            Player player,
            UseOnContext context) {
        if (player != null) {
            ToolBreakScrap.hurtAndBreak(
                    stack,
                    1,
                    player,
                    LivingEntity.getSlotForHand(context.getHand()));
        }
    }
}
