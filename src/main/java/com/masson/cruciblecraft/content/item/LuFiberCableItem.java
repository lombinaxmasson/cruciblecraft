package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** Source-backed, lossless fiber cable used for GT6 LU transport. */
public final class LuFiberCableItem extends BlockItem {
    public LuFiberCableItem(CableBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.cruciblecraft.lu_fiber_cable");
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        return Gt6StyleConnections.placeBlock(
                context, super.placeBlock(context, state));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.lu_fiber_cable")
                .withStyle(ChatFormatting.GRAY));
    }
}
