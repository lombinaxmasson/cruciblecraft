package com.masson.cruciblecraft.energy.battery;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Sneak-only placement; charged stacks do not stack. */
public final class BatteryBlockItem extends BlockItem {
    public BatteryBlockItem(BatteryBlock block, Properties properties) {
        super(block, properties);
    }

    public EnergyBatteryProfile profile() {
        return ((BatteryBlock) getBlock()).profile();
    }

    public ItemStack fullStack() {
        ItemStack stack = new ItemStack(this);
        BatteryCharge.set(stack, profile().capacity(), profile().capacity());
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
        return profile().capacity() > 0L;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long capacity = profile().capacity();
        if (capacity <= 0L) {
            return 0;
        }
        return Math.round(13.0F * BatteryCharge.get(stack) / (float) capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return profile().color();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        EnergyBatteryProfile profile = profile();
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.battery.charge",
                        BatteryCharge.get(stack),
                        profile.capacity(),
                        profile.energyType().name(),
                        profile.inputSize())
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.battery.sneak_place")
                .withStyle(ChatFormatting.GRAY));
    }
}
