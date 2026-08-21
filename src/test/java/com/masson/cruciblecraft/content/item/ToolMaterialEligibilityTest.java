package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.ToolStats;

class ToolMaterialEligibilityTest {
    private static final ToolStats CORE_STATS =
            new ToolStats(100, 4.0, 1, 2);

    @Test
    void allToolsApplySharedIdentityAndAntimatterExclusions() {
        for (ToolKind kind : ToolKind.values()) {
            assertTrue(kind.isEligible(
                    "iron", CORE_STATS, tags()));
            assertTrue(kind.isEligible(
                    "iron",
                    CORE_STATS,
                    tags("PROPERTIES.NO_ADVANCED_TOOLS")));
            assertFalse(kind.isEligible(
                    "wood", CORE_STATS, tags()));
            assertFalse(kind.isEligible(
                    "iron",
                    CORE_STATS,
                    tags(ToolMaterialRules.ANTIMATTER_TAG)));
            assertFalse(kind.isEligible(
                    "iron", CORE_STATS, List.of()));
        }
        for (ToolKind kind : ToolKind.values()) {
            if (kind != ToolKind.WRENCH && kind != ToolKind.WIRE_CUTTER) {
                assertFalse(kind.isEligible(
                        "iron",
                        CORE_STATS,
                        tags(ToolMaterialRules.COATED_TAG)));
            }
        }
        // GT6 Loader_Tools.java:310/324: wrench and wire cutter listeners
        // both omit COATED.NOT, so coated material stays eligible.
        assertTrue(ToolKind.WRENCH.isEligible(
                "iron",
                CORE_STATS,
                tags(ToolMaterialRules.COATED_TAG)));
        assertTrue(ToolKind.WIRE_CUTTER.isEligible(
                "iron",
                CORE_STATS,
                tags(ToolMaterialRules.COATED_TAG)));
    }

    @Test
    void coreHandToolsRequireOneToolType() {
        ToolStats noTypes = stats(1, 0);
        for (ToolKind kind : List.of(
                ToolKind.PICKAXE,
                ToolKind.SHOVEL,
                ToolKind.AXE,
                ToolKind.HOE,
                ToolKind.SWORD)) {
            assertFalse(kind.isEligible("iron", noTypes, tags()));
            assertTrue(kind.isEligible(
                    "iron", stats(1, 1), tags()));
        }
    }

    @Test
    void hammerIntersectsHardHeadAndQualityRules() {
        assertFalse(ToolKind.SMITHING_HAMMER.isEligible(
                "iron", stats(0, 1), tags()));
        assertTrue(ToolKind.SMITHING_HAMMER.isEligible(
                "iron", stats(1, 1), tags()));
        for (String excluded : List.of(
                ToolMaterialRules.WOOD_TAG,
                ToolMaterialRules.BOUNCY_TAG,
                ToolMaterialRules.STRETCHY_TAG)) {
            assertFalse(ToolKind.SMITHING_HAMMER.isEligible(
                    "iron", stats(1, 1), tags(excluded)));
        }
    }

    @Test
    void workshopToolsUseTheirExactPrefixIntersections() {
        assertTrue(ToolKind.FILE.isEligible(
                "iron", stats(2, 2), tags()));
        assertFalse(ToolKind.FILE.isEligible(
                "iron", stats(3, 2), tags()));
        assertFalse(ToolKind.FILE.isEligible(
                "iron",
                stats(2, 2),
                tags(ToolMaterialRules.BOUNCY_TAG)));

        for (ToolKind kind : List.of(ToolKind.CHISEL, ToolKind.SAW)) {
            assertFalse(kind.isEligible(
                    "iron", stats(1, 1), tags()));
            assertTrue(kind.isEligible(
                    "iron", stats(4, 2), tags()));
            assertFalse(kind.isEligible(
                    "iron",
                    stats(4, 2),
                    tags(ToolMaterialRules.STRETCHY_TAG)));
        }

        assertFalse(ToolKind.SCREWDRIVER.isEligible(
                "iron", stats(1, 1), tags()));
        assertTrue(ToolKind.SCREWDRIVER.isEligible(
                "iron",
                stats(0, 2),
                tags(ToolMaterialRules.BOUNCY_TAG)));

        assertFalse(ToolKind.WRENCH.isEligible(
                "iron", stats(0, 2), tags()));
        assertTrue(ToolKind.WRENCH.isEligible(
                "iron", stats(1, 2), tags()));
        assertFalse(ToolKind.WRENCH.isEligible(
                "iron",
                stats(1, 2),
                tags(ToolMaterialRules.BOUNCY_TAG)));

        // GT6 Loader_Tools.java:324 has no qualmin and excludes BOUNCY.
        assertTrue(ToolKind.WIRE_CUTTER.isEligible(
                "iron", stats(0, 2), tags()));
        assertFalse(ToolKind.WIRE_CUTTER.isEligible(
                "iron", stats(1, 1), tags()));
        assertFalse(ToolKind.WIRE_CUTTER.isEligible(
                "iron",
                stats(1, 2),
                tags(ToolMaterialRules.BOUNCY_TAG)));
    }

    private static ToolStats stats(int quality, long types) {
        return new ToolStats(100, 4.0, quality, types);
    }

    private static List<String> tags(String... extras) {
        List<String> tags = new ArrayList<>();
        tags.add(ToolMaterialRules.TOOL_DOMAIN_TAG);
        tags.addAll(List.of(extras));
        return List.copyOf(tags);
    }
}
