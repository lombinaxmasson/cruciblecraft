package com.masson.cruciblecraft.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Fortune / silk from a block loot tool. */
public record OreHarvest(int fortune, boolean silkTouch) {
    public static OreHarvest from(LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null || tool.isEmpty()) {
            return new OreHarvest(0, false);
        }
        var enchantments =
                params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.FORTUNE), tool);
        int silk = EnchantmentHelper.getItemEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool);
        return new OreHarvest(fortune, silk > 0);
    }
}
