package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class HopperBlockItem extends BlockItem {
    public HopperBlockItem(HopperBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        HopperBlock block = (HopperBlock) getBlock();
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.hopper.slots",
                block.variant().slots()));
        if (block.variant().kind() == HopperKind.QUEUE_HOPPER) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.hopper.fifo"));
        }
    }
}
