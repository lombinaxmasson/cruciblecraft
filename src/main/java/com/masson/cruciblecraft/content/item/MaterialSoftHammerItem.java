package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/** GT6 soft hammer: toggle lamps/powered rails and rotate facings. */
public final class MaterialSoftHammerItem extends MaterialToolItem {
    public MaterialSoftHammerItem(Properties properties) {
        super(
                properties,
                ToolKind.SOFT_HAMMER,
                "item.cruciblecraft.material_soft_hammer");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return canApplyDurabilityDamage(context.getItemInHand())
                ? MachineToolInteractions.softHammer(context)
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
                "tooltip.cruciblecraft.soft_hammer")
                .withStyle(ChatFormatting.GRAY));
    }
}
