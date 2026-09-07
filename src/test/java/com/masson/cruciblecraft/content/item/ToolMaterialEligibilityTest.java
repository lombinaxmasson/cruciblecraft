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
    @Test
    void allToolsApplySharedIdentityAndAntimatterExclusions() {
        for (ToolKind kind : ToolKind.values()) {
            ToolStats stats = sufficient(kind);
            List<String> base = baseTags(kind);
            assertTrue(kind.isEligible("iron", stats, base), kind.name());
            List<String> noAdvanced = new ArrayList<>(base);
            noAdvanced.add("PROPERTIES.NO_ADVANCED_TOOLS");
            assertTrue(kind.isEligible("iron", stats, noAdvanced), kind.name());
            assertFalse(kind.isEligible("wood", stats, base), kind.name());
            List<String> antimatter = new ArrayList<>(base);
            antimatter.add(ToolMaterialRules.ANTIMATTER_TAG);
            assertFalse(kind.isEligible("iron", stats, antimatter), kind.name());
            assertFalse(kind.isEligible("iron", stats, List.of()), kind.name());
            List<String> coated = new ArrayList<>(base);
            coated.add(ToolMaterialRules.COATED_TAG);
            if (kind.allowsCoated()) {
                assertTrue(kind.isEligible("iron", stats, coated), kind.name());
            } else {
                assertFalse(kind.isEligible("iron", stats, coated), kind.name());
            }
        }
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
    void softHammerRequiresWoodBouncyOrStretchyAndRejectsIron() {
        assertFalse(ToolKind.SOFT_HAMMER.isEligible(
                "iron", stats(1, 1), tags()));
        assertTrue(ToolKind.SOFT_HAMMER.isEligible(
                "rubber",
                stats(1, 1),
                tags(ToolMaterialRules.BOUNCY_TAG)));
        assertFalse(ToolKind.SOFT_HAMMER.isEligible(
                "rubber",
                stats(1, 1),
                tags(
                        ToolMaterialRules.BOUNCY_TAG,
                        ToolMaterialRules.COATED_TAG)));
    }

    @Test
    void crowbarAndPlungerAllowCoatedWithoutTypeMinimum() {
        assertTrue(ToolKind.CROWBAR.isEligible(
                "iron", stats(0, 1), tags(ToolMaterialRules.COATED_TAG)));
        assertTrue(ToolKind.PLUNGER.isEligible(
                "iron", stats(0, 1), tags(ToolMaterialRules.COATED_TAG)));
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
        assertTrue(ToolKind.MONKEY_WRENCH.isEligible(
                "iron", stats(1, 2), tags()));
        assertFalse(ToolKind.MONKEY_WRENCH.isEligible(
                "iron",
                stats(1, 2),
                tags(ToolMaterialRules.STRETCHY_TAG)));

        assertTrue(ToolKind.WIRE_CUTTER.isEligible(
                "iron", stats(0, 2), tags()));
        assertFalse(ToolKind.WIRE_CUTTER.isEligible(
                "iron", stats(1, 1), tags()));
        assertFalse(ToolKind.WIRE_CUTTER.isEligible(
                "iron",
                stats(1, 2),
                tags(ToolMaterialRules.BOUNCY_TAG)));

        assertFalse(ToolKind.HAND_DRILL.isEligible(
                "iron", stats(1, 2), tags()));
        assertTrue(ToolKind.HAND_DRILL.isEligible(
                "iron", stats(2, 2), tags()));
        assertFalse(ToolKind.POCKET_MULTITOOL.isEligible(
                "iron", stats(1, 2), tags()));
        assertTrue(ToolKind.POCKET_MULTITOOL.isEligible(
                "iron", stats(1, 3), tags()));
    }

    private static ToolStats sufficient(ToolKind kind) {
        return stats(
                Math.max(1, kind.minQuality()),
                Math.max(2L, kind.minTypes()));
    }

    private static List<String> baseTags(ToolKind kind) {
        return kind == ToolKind.SOFT_HAMMER
                ? tags(ToolMaterialRules.BOUNCY_TAG)
                : tags();
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
