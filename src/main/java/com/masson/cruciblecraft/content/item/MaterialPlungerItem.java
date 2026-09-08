package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** GT6 plunger: trash pipe contents, drain 1000 mB from tanks. */
public final class MaterialPlungerItem extends MaterialToolItem {
    public MaterialPlungerItem(Properties properties) {
        super(
                properties,
                ToolKind.PLUNGER,
                "item.cruciblecraft.material_plunger");
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.plunger.fluid")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.plunger.item")
                .withStyle(ChatFormatting.GRAY));
    }
}
