package com.masson.cruciblecraft.energy.transformer;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Normal placement; tooltip names the voltage pair. */
public final class TransformerBlockItem extends BlockItem {
    public TransformerBlockItem(TransformerBlock block, Properties properties) {
        super(block, properties);
    }

    public EnergyTransformerProfile profile() {
        return ((TransformerBlock) getBlock()).profile();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.transformer.front_in")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.transformer.wrench")
                .withStyle(ChatFormatting.GRAY));
    }
}
