package com.masson.cruciblecraft.energy.cooler;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Placement item; tooltip names CU/HU split and the input kind. */
public final class CoolerBlockItem extends BlockItem {
    public CoolerBlockItem(CoolerBlock block, Properties properties) {
        super(block, properties);
    }

    public CoolerProfile profile() {
        return ((CoolerBlock) getBlock()).profile();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CoolerProfile profile = profile();
        tooltip.add(Component.translatable(
                        profile.electric()
                                ? "tooltip.cruciblecraft.cooler.electric"
                                : "tooltip.cruciblecraft.cooler.flux",
                        profile.nbtInput(),
                        profile.nbtOutput())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.cooler.sides")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
