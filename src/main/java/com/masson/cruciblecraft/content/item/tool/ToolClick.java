package com.masson.cruciblecraft.content.item.tool;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolActionSource;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.item.MaterialToolItem;
import com.masson.cruciblecraft.content.item.MaterialElectricToolItem;
import com.masson.cruciblecraft.content.item.tool.ElectricToolCharge;
import com.masson.cruciblecraft.content.item.ToolBreakScrap;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Routes a tool click: typed block handlers first, then vanilla/capability
 * adapters. Durability is applied by the handler that did the work.
 */
public final class ToolClick {
    private ToolClick() {}

    public static boolean canDispatch(ItemStack stack) {
        if (!(stack.getItem() instanceof ToolActionSource)) {
            return false;
        }
        return !(stack.getItem() instanceof MaterialToolItem tool)
                || tool.canApplyDurabilityDamage(stack);
    }

    /**
     * Item {@code useOn} path: ask the clicked block, then adapters.
     */
    public static InteractionResult useOn(UseOnContext context) {
        return use(context, true).toInteractionResult(
                context.getLevel().isClientSide);
    }

    /**
     * Block {@code useItemOn} path: ask only that block so remaining item
     * uses (covers, hopper GUI, barrel insert) can still run.
     */
    public static ItemInteractionResult useItemOn(
            ItemStack stack,
            Level level,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return interactable(new UseOnContext(level, player, hand, stack, hit))
                .toItemResult(level.isClientSide);
    }

    public static ToolResult interactable(UseOnContext context) {
        return use(context, false);
    }

    public static BlockHitResult hit(UseOnContext context) {
        return new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside());
    }

    public static ToolResult plunger(UseOnContext context, boolean emptied) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        if (!emptied) {
            return ToolResult.PASS;
        }
        hurt(context);
        level.playSound(
                null,
                context.getClickedPos(),
                SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS,
                0.6F,
                0.6F);
        return ToolResult.SUCCESS;
    }

    public static void hurt(UseOnContext context) {
        hurt(context.getItemInHand(), context.getPlayer(), context.getHand());
    }

    public static void give(Player player, ItemStack stack) {
        if (player == null || stack.isEmpty()) {
            return;
        }
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    public static void hurt(
            ItemStack stack, Player player, InteractionHand hand) {
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        if (stack.getItem() instanceof MaterialToolItem tool
                && !tool.canApplyDurabilityDamage(stack)) {
            return;
        }
        if (stack.getItem() instanceof MaterialElectricToolItem electric) {
            ElectricToolCharge.spend(
                    stack,
                    electric.spec().damagePerBlock(),
                    player,
                    LivingEntity.getSlotForHand(hand));
            return;
        }
        ToolBreakScrap.hurtAndBreak(
                stack, 1, player, LivingEntity.getSlotForHand(hand));
    }

    private static ToolResult use(
            UseOnContext context, boolean includeAdapters) {
        ItemStack stack = context.getItemInHand();
        if (!canDispatch(stack)) {
            return ToolResult.PASS;
        }
        ToolActionSource source = (ToolActionSource) stack.getItem();
        Block block = context.getLevel()
                .getBlockState(context.getClickedPos())
                .getBlock();
        ToolResult lastReject = ToolResult.PASS;
        for (ToolAction action : ToolAction.values()) {
            if (!source.provides(stack, action)) {
                continue;
            }
            ToolResult result = ToolResult.PASS;
            if (block instanceof ToolInteractable target) {
                result = target.useTool(action, context);
            }
            if (result == ToolResult.SUCCESS) {
                return result;
            }
            if (result == ToolResult.REJECT) {
                lastReject = ToolResult.REJECT;
                continue;
            }
            if (includeAdapters) {
                result = VanillaToolAdapters.use(action, context);
                if (result == ToolResult.SUCCESS) {
                    return result;
                }
                if (result == ToolResult.REJECT) {
                    lastReject = ToolResult.REJECT;
                    continue;
                }
            }
            if (action == ToolAction.MAGNIFYING_GLASS) {
                result = MagnifyingInspect.tryUse(context);
                if (result == ToolResult.SUCCESS) {
                    return result;
                }
            }
        }
        return lastReject;
    }
}
