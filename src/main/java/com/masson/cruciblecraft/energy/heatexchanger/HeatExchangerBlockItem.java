package com.masson.cruciblecraft.energy.heatexchanger;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Placement item; tooltip names HU rate and efficiency. */
public final class HeatExchangerBlockItem extends BlockItem {
    public HeatExchangerBlockItem(
            HeatExchangerBlock block, Properties properties) {
        super(block, properties);
    }

    public HeatExchangerProfile profile() {
        return ((HeatExchangerBlock) getBlock()).profile();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        HeatExchangerProfile profile = profile();
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.heat_exchanger.hu_rate",
                        profile.huRate())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.heat_exchanger.efficiency",
                        profile.efficiencyBps() / 100)
                .withStyle(ChatFormatting.GRAY));
    }
}
