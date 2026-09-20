package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SlashItemModelsTest {
    @Test
    void prefixesSlashedRegistryPathsWithItemFolder() {
        assertEquals("item/hslasteel/double_wire", SlashItemModels.path("hslasteel/double_wire"));
        assertEquals(
                "item/gt_wood/blue_mahoe_planks",
                SlashItemModels.path("gt_wood/blue_mahoe_planks"));
        assertEquals("item/wood_treated/fluid_pipe", SlashItemModels.path("wood_treated/fluid_pipe"));
        assertEquals("ingot", SlashItemModels.path("ingot"));
        assertEquals("item/already/prefixed", SlashItemModels.path("item/already/prefixed"));
    }
}
