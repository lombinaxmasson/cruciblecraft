package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RemainderCreativeTabsTest {
    @Test
    void routesKnownRemainderIdentities() {
        assertEquals(
                RemainderCreativeTabs.Tab.COMPONENTS,
                RemainderCreativeTabs.of("part/circuit_t1_basic", "multiitem"));
        assertEquals(
                RemainderCreativeTabs.Tab.NATURE,
                RemainderCreativeTabs.of("food/tomato_solid_ketchup", "multiitem"));
        assertEquals(
                RemainderCreativeTabs.Tab.WIRES,
                RemainderCreativeTabs.of("electric_wire/3x_lead_wire", "mte_item"));
        assertEquals(
                RemainderCreativeTabs.Tab.PIPES,
                RemainderCreativeTabs.of("quadruple/wood_fluid_pipe", "mte_item"));
        assertEquals(
                RemainderCreativeTabs.Tab.BUILDING,
                RemainderCreativeTabs.of("panel/concrete_white", "concrete_panel"));
        assertEquals(
                RemainderCreativeTabs.Tab.MACHINES,
                RemainderCreativeTabs.of(
                        "processing/automatic_hammer_bronze", "mte_item"));
        assertEquals(
                RemainderCreativeTabs.Tab.TOOLS,
                RemainderCreativeTabs.of("heat/protection_suit_helmet", "armor"));
        assertEquals(
                RemainderCreativeTabs.Tab.NATURE,
                RemainderCreativeTabs.of("heavy_water/plant_gt_berry", "object"));
        assertNull(RemainderCreativeTabs.of("iron/tool_head_pickaxe", "tool_head"));
    }
}
