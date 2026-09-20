package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.recipe.crafting.WorkbenchToolRecipePlan;

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

    @Test
    void workbenchMaterialsIncludePincersAndMergeWithAssembler() {
        List<WorkbenchToolRecipePlan.Recipe> recipes = List.of(
                recipe("tools/iron/pincers", "cruciblecraft:material_pincers", "iron"),
                recipe("tools/steel/pincers", "cruciblecraft:material_pincers", "steel"),
                recipe("tools/iron/wrench", "cruciblecraft:material_wrench", "iron"),
                recipe("tools/steel/pincers", "cruciblecraft:material_pincers", "steel"));
        assertEquals(
                List.of("iron", "steel"),
                ToolDisplayPlan.workbenchMaterials(
                        recipes, "cruciblecraft:material_pincers"));
        assertEquals(
                List.of("copper", "iron"),
                ToolDisplayPlan.displayMaterials(
                        List.of("tool/assembler/wrench/metal/copper"),
                        recipes,
                        "wrench",
                        "cruciblecraft:material_wrench"));
        assertEquals(
                List.of("iron", "steel"),
                ToolDisplayPlan.displayMaterials(
                        List.of(),
                        recipes,
                        "pincers",
                        "cruciblecraft:material_pincers"));
    }

    private static WorkbenchToolRecipePlan.Recipe recipe(
            String path, String resultId, String material) {
        return new WorkbenchToolRecipePlan.Recipe(
                path,
                List.of(),
                Map.of(),
                Map.of(),
                resultId,
                material,
                true,
                1,
                false,
                0L,
                0L);
    }
}
