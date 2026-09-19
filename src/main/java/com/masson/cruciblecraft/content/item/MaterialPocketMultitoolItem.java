package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.content.item.tool.InventoryBlockPlacer;
import com.masson.cruciblecraft.content.item.tool.PocketMultitoolMode;
import com.masson.cruciblecraft.content.item.tool.ProvidedToolActions;
import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 pocket multitool: sneak-use cycles metadata; the live mode inherits
 * that tool's click actions and harvest rules.
 */
public final class MaterialPocketMultitoolItem extends MaterialToolItem {
    public MaterialPocketMultitoolItem(Properties properties) {
        super(
                properties,
                ToolKind.POCKET_MULTITOOL,
                "item.cruciblecraft.material_pocket_multitool");
    }

    public PocketMultitoolMode mode(ItemStack stack) {
        return PocketMultitoolMode.fromOrdinal(
                stack.getOrDefault(ModComponents.POCKET_MODE.get(), 0));
    }

    public void cycle(ItemStack stack) {
        PocketMultitoolMode next = mode(stack).next();
        stack.set(ModComponents.POCKET_MODE.get(), next.ordinal());
    }

    @Override
    public boolean provides(ItemStack stack, ToolAction action) {
        ToolKind kind = mode(stack).kind();
        return kind != null && ProvidedToolActions.of(kind).contains(action);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide) {
                cycle(context.getItemInHand());
                player.displayClientMessage(modeMessage(context.getItemInHand()), true);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        InteractionResult tool = super.useOn(context);
        if (tool.consumesAction()) {
            return tool;
        }
        ToolKind kind = mode(context.getItemInHand()).kind();
        if (kind == ToolKind.SAW) {
            return InventoryBlockPlacer.placeSaplingOrWorkbench(context);
        }
        return tool;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycle(stack);
                player.displayClientMessage(modeMessage(stack), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        ToolKind kind = mode(stack).kind();
        return material(stack)
                .filter(ignored -> kind != null)
                .map(materialId -> ToolMining.destroySpeed(kind, materialId, state))
                .orElse(1.0F);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        ToolKind kind = mode(stack).kind();
        return kind != null
                && material(stack).isPresent()
                && ToolMining.correctTool(kind, state);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(modeMessage(stack).copy().withStyle(ChatFormatting.GRAY));
    }

    private Component modeMessage(ItemStack stack) {
        return Component.translatable(
                "tooltip.cruciblecraft.pocket_multitool.mode."
                        + mode(stack).serializedName());
    }
}
