package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Pins the pure route-path filter used by the TOOLS creative tab and the EMI
 * plugin. Live assembler consistency is pinned in-game by the GameTest tool
 * maps (cruciblecraft:gametest — toolCounts loop), which the JUnit classpath
 * cannot resolve.
 */
class ToolDisplayPlanTest {
    @Test
    void routedMaterialsFilterSortsAndDeduplicates() {
        List<String> paths = List.of(
                "tool/assembler/wrench/metal/iron",
                "tool/assembler/wrench/gem/amber",
                "tool/assembler/wrench/metal/copper",
                "tool/assembler/wrench/metal/iron",
                "tool/assembler/screwdriver/rod/iron",
                "other/thing");
        assertEquals(
                List.of("amber", "copper", "iron"),
                ToolDisplayPlan.routedMaterials(paths, "wrench"));
        assertEquals(
                List.of("iron"),
                ToolDisplayPlan.routedMaterials(paths, "screwdriver"));
        assertEquals(
                List.of(),
                ToolDisplayPlan.routedMaterials(paths, "pickaxe"));
    }

    @Test
    void routedMaterialsIgnoresNonRoutePrefixes() {
        assertEquals(
                List.of(),
                ToolDisplayPlan.routedMaterials(
                        List.of(
                                "tool/assemblerx/wrench/metal/iron",
                                "tool/assembler/wrench",
                                "chemical/chemical/anything"),
                        "wrench"));
    }
}
