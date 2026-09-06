package com.masson.cruciblecraft.recipe.crafting;

import com.masson.cruciblecraft.machine.processing.CraftingCatalystPolicy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 workbench tool wear for {@link ShapedCatalystRecipe} catalyst slots.
 *
 * <p>GregTech6 stores max durability as {@code mToolDurability * 100} and
 * charges {@code IToolStats.getToolDamagePerContainerCraft()} against that
 * scale ({@code MultiItemTool.java:182,532-538}). CrucibleCraft durability is
 * the raw {@code mToolDurability}, so container-craft costs divide by 100.
 */
public final class CraftingToolWear {
    private CraftingToolWear() {}

    public static int vanillaDamage(ResourceLocation itemId) {
        if (!CraftingCatalystPolicy.isWearCatalyst(itemId)) {
            return 0;
        }
        return switch (itemId.getPath()) {
            case "material_wrench", "material_monkey_wrench" -> 8;
            case "smithing_hammer",
                    "material_file",
                    "material_screwdriver",
                    "material_wire_cutter",
                    "material_chisel" -> 4;
            case "material_saw", "flint_knife" -> 1;
            default -> 1;
        };
    }

    public static ItemStack apply(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int amount = vanillaDamage(
                BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (amount <= 0 || !stack.isDamageableItem()) {
            return stack.copy();
        }
        ItemStack remaining = stack.copy();
        int next = remaining.getDamageValue() + amount;
        if (next >= remaining.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        remaining.setDamageValue(next);
        return remaining;
    }
}
