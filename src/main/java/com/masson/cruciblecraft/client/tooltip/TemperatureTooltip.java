package com.masson.cruciblecraft.client.tooltip;

import java.util.Locale;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.config.ModConfig;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class TemperatureTooltip {
    private TemperatureTooltip() {}

    @SubscribeEvent
    public static void appendTemperature(ItemTooltipEvent event) {
        if (!event.getItemStack().has(ModComponents.HEAT.get())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float celsius = ItemHeat.temperature(event.getItemStack(), minecraft.level.getGameTime());
        if (celsius <= ItemHeat.AMBIENT_TEMPERATURE + 0.01f) {
            return;
        }

        boolean fahrenheit = "F".equalsIgnoreCase(ModConfig.TEMPERATURE_UNIT.get());
        float displayed = fahrenheit ? celsius * 9.0f / 5.0f + 32.0f : celsius;
        String value = String.format(Locale.ROOT, "%.1f", displayed);
        event.getToolTip().add(Component.translatable(
                "tooltip.cruciblecraft.temperature",
                value,
                fahrenheit ? "°F" : "°C").withStyle(ChatFormatting.GOLD));
    }
}
