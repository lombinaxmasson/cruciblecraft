package com.masson.cruciblecraft.energy.zpm;

import java.util.List;

import com.masson.cruciblecraft.energy.battery.BatteryCharge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Sneak-only placement. A charged module does not stack. */
public final class ZpmModuleItem extends BlockItem {
    public ZpmModuleItem(ZpmModuleBlock block, Properties properties) {
        super(block, properties);
    }

    public ItemStack fullStack() {
        ItemStack stack = new ItemStack(this);
        BatteryCharge.set(stack, ZpmModule.CAPACITY, ZpmModule.CAPACITY);
        return stack;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getPlayer() == null || !context.isSecondaryUseActive()) {
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(
                13.0F * ZpmModule.charge(stack) / (float) ZpmModule.CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xffdd00;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        long charge = ZpmModule.charge(stack);
        if (charge > 0L && charge < ZpmModule.CAPACITY) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.battery.charge",
                            charge,
                            ZpmModule.CAPACITY,
                            "QU",
                            ZpmModule.INPUT)
                    .withStyle(ChatFormatting.WHITE));
        } else {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.zpm.artifact")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.zpm.capacity",
                            ZpmModule.CAPACITY)
                    .withStyle(ChatFormatting.WHITE));
        }
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.battery.sneak_place")
                .withStyle(ChatFormatting.GRAY));
    }
}
