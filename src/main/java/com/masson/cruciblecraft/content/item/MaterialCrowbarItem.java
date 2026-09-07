package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * GT6 crowbar: pry covers, pick up mass-storage barrels with contents,
 * rotate rails.
 */
public final class MaterialCrowbarItem extends MaterialToolItem {
    public MaterialCrowbarItem(Properties properties) {
        super(
                properties,
                ToolKind.CROWBAR,
                "item.cruciblecraft.material_crowbar");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return canApplyDurabilityDamage(context.getItemInHand())
                ? MachineToolInteractions.crowbar(context)
                : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.crowbar")
                .withStyle(ChatFormatting.GRAY));
    }
}
