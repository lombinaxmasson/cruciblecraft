package com.masson.cruciblecraft.recipe.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

class CraftingToolWearTest {
    @Test
    void containerCraftDamageMatchesGt6Scale() {
        // GT6 MultiItemTool max = mToolDurability * 100; CC durability is
        // the raw value, so container-craft costs divide by 100.
        assertEquals(8, CraftingToolWear.vanillaDamage(id("material_wrench")));
        assertEquals(8, CraftingToolWear.vanillaDamage(id("material_monkey_wrench")));
        assertEquals(4, CraftingToolWear.vanillaDamage(id("smithing_hammer")));
        assertEquals(4, CraftingToolWear.vanillaDamage(id("material_file")));
        assertEquals(4, CraftingToolWear.vanillaDamage(id("material_screwdriver")));
        assertEquals(4, CraftingToolWear.vanillaDamage(id("material_wire_cutter")));
        assertEquals(4, CraftingToolWear.vanillaDamage(id("material_chisel")));
        assertEquals(1, CraftingToolWear.vanillaDamage(id("material_saw")));
        assertEquals(1, CraftingToolWear.vanillaDamage(id("flint_knife")));
        assertEquals(0, CraftingToolWear.vanillaDamage(id("programmed_circuit")));
        assertEquals(
                0,
                CraftingToolWear.vanillaDamage(
                        ResourceLocation.withDefaultNamespace("stick")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
