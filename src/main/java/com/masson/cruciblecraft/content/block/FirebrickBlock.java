package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FirebrickBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * GT6 MultiTileEntityMultiBlockPart 18000. Unbound it is a brick; a formed
 * coke oven binds I/O and tool clicks onto the same identity.
 */
public final class FirebrickBlock extends Block implements EntityBlock, ToolInteractable {
    public FirebrickBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FirebrickBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        CokeOvenBlockEntity host = host(level, pos);
        if (host == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(host);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        ItemInteractionResult tool = ToolClick.useItemOn(
                stack, level, player, hand, hitResult);
        if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            return tool;
        }
        CokeOvenBlockEntity host = host(level, pos);
        if (host == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(Items.FLINT_AND_STEEL)) {
            if (!level.isClientSide && host.ignite()) {
                if (!player.getAbilities().instabuild) {
                    stack.hurtAndBreak(
                            1,
                            player,
                            hand == InteractionHand.MAIN_HAND
                                    ? EquipmentSlot.MAINHAND
                                    : EquipmentSlot.OFFHAND);
                }
                level.playSound(
                        null,
                        pos,
                        SoundEvents.FLINTANDSTEEL_USE,
                        SoundSource.BLOCKS,
                        1.0F,
                        level.random.nextFloat() * 0.4F + 0.8F);
                player.displayClientMessage(
                        Component.translatable("message.cruciblecraft.coke_oven_ignited"),
                        true);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (FluidUtil.getFluidHandler(stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        var handler = host.automationFluids(hitResult.getDirection());
        if (handler != null
                && FluidUtil.interactWithFluidHandler(player, hand, handler)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        CokeOvenBlockEntity host = host(context.getLevel(), context.getClickedPos());
        if (host == null) {
            return ToolResult.PASS;
        }
        if (action == ToolAction.IGNITER) {
            if (context.getLevel().isClientSide) {
                return ToolResult.SUCCESS;
            }
            if (host.ignite()) {
                ToolClick.hurt(context);
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            Component.translatable("message.cruciblecraft.coke_oven_ignited"),
                            true);
                }
                return ToolResult.SUCCESS;
            }
            return ToolResult.PASS;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (!context.getLevel().isClientSide) {
                host.resetBySoftHammer();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.PLUNGER) {
            return ToolClick.plunger(context, host.trashWithPlunger());
        }
        return ToolResult.PASS;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof FirebrickBlockEntity brick) {
            brick.unbind();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static CokeOvenBlockEntity host(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FirebrickBlockEntity brick
                ? brick.host()
                : null;
    }
}
