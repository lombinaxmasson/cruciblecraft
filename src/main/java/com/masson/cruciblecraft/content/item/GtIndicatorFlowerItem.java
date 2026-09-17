package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.content.block.GtIndicatorFlowerBlock;
import com.masson.cruciblecraft.worldgen.IndicatorFlower;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 flower BlockItem: keeps meta via {@code BLOCK_STATE}. */
public final class GtIndicatorFlowerItem extends BlockItem {
    public GtIndicatorFlowerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return GtIndicatorFlowerBlock.displayName(stateOf(stack));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        IndicatorFlower flower = stateOf(stack).getValue(GtIndicatorFlowerBlock.FLOWER);
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.gt_indicator_flower." + flower.getSerializedName()));
    }

    private BlockState stateOf(ItemStack stack) {
        BlockState fallback = getBlock().defaultBlockState();
        BlockItemStateProperties properties =
                stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        return properties.apply(fallback);
    }
}
