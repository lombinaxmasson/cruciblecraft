package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** Stack-size-one universal fluid container backed by a data component. */
public final class PortableFluidTankItem extends Item {
    public static final int CAPACITY = 64_000;

    public PortableFluidTankItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        SimpleFluidContent content = stack.getOrDefault(
                ModComponents.PORTABLE_FLUID.get(), SimpleFluidContent.EMPTY);
        if (content.isEmpty()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.portable_fluid_tank.empty",
                    CAPACITY).withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.portable_fluid_tank.contents",
                content.copy().getHoverName(),
                content.getAmount(),
                CAPACITY).withStyle(ChatFormatting.GRAY));
    }
}
