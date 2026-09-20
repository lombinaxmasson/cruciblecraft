package com.masson.cruciblecraft.energy.flux;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Placement item; tooltip names the GT6 RF/FE conversion. */
public final class FluxBlockItem extends BlockItem {
    public FluxBlockItem(FluxBlock block, Properties properties) {
        super(block, properties);
    }

    public FluxProfile profile() {
        return ((FluxBlock) getBlock()).profile();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        FluxProfile profile = profile();
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.flux." + profile.kind(),
                        profile.nbtInput(),
                        profile.nbtOutput())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.flux.sides." + profile.kind())
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
