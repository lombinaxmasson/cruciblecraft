package com.masson.cruciblecraft.energy.longdistance;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class LongDistanceTransformerBlockItem extends BlockItem {
    public LongDistanceTransformerBlockItem(
            LongDistanceTransformerBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!(getBlock() instanceof LongDistanceTransformerBlock block)) {
            return;
        }
        tooltip.add(Component.literal("EU " + block.profile().voltage())
                .withStyle(ChatFormatting.GRAY));
    }
}
