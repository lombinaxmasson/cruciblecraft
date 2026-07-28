package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class SmithingHammerItem extends Item {
    public SmithingHammerItem(Properties properties) {
        super(properties.component(
                ModComponents.TOOL_MATERIAL,
                MachineMaterialRules.DEFAULT_HAMMER_MATERIAL));
    }

    public String material(ItemStack stack) {
        return MachineMaterialRules.sanitize(
                Device.HAMMER,
                stack.getOrDefault(
                        ModComponents.TOOL_MATERIAL,
                        MachineMaterialRules.DEFAULT_HAMMER_MATERIAL));
    }

    public ItemStack variant(String materialId) {
        String material = MachineMaterialRules.sanitize(Device.HAMMER, materialId);
        ItemStack stack = new ItemStack(this);
        stack.set(ModComponents.TOOL_MATERIAL, material);
        stack.set(DataComponents.MAX_DAMAGE, MachineMaterialRules.hammerMaxDurability(material));
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String material = material(stack);
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.tool_material",
                Component.translatable("material.cruciblecraft." + material),
                MachineMaterialRules.materialTier(material)));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.durability",
                Math.max(0, stack.getMaxDamage() - stack.getDamageValue()),
                stack.getMaxDamage()));
    }
}
