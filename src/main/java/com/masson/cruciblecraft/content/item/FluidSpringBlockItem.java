package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

public final class FluidSpringBlockItem extends BlockItem {
    public FluidSpringBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        SimpleFluidContent content = stack.get(ModComponents.SPRING_CONTENT.get());
        if (content != null && !content.isEmpty()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_spring.contents",
                    content.copy().getHoverName(),
                    content.getAmount()));
        }
    }
}
