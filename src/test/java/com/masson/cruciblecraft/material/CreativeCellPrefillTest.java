package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Pins the FLUID_CELLS creative-tab prefill plan: every cell gate row is a
 * source fluid id (no flowing_ ids), rows are enumerated in stable id order,
 * and each row maps to a cell kind. The stack-component writing itself is
 * exercised in-game by the cell GameTests; the closed gate keeps this
 * projection registry-free.
 */
class CreativeCellPrefillTest {
    @Test
    void prefillPlanIsSortedAndSourceOnly() {
        List<Map.Entry<net.minecraft.resources.ResourceLocation,
                CellContentGate.Kind>> entries =
                CellContentGate.sortedEntries();
        assertTrue(!entries.isEmpty());
        assertEquals(entries.size(), CellContentGate.entries().size());

        net.minecraft.resources.ResourceLocation previous = null;
        for (var entry : entries) {
            assertTrue(
                    !entry.getKey().getPath().startsWith("flowing_"),
                    "gate row must be a source fluid id: " + entry.getKey());
            assertTrue(
                    entry.getValue() == CellContentGate.Kind.FLUID
                            || entry.getValue() == CellContentGate.Kind.GAS,
                    "unknown kind for " + entry.getKey());
            if (previous != null) {
                assertTrue(
                        previous.compareTo(entry.getKey()) < 0,
                        "prefill plan is not sorted: "
                                + previous + " then " + entry.getKey());
            }
            previous = entry.getKey();
        }
    }

    @Test
    void gateContainsOnlyExplicitMoltenCalcite() {
        // Fluid-bed FM.FluidBed requires molten calcite. Other molten metals
        // stay out of the closed cell gate.
        java.util.Set<String> molten = CellContentGate.entries().keySet()
                .stream()
                .map(net.minecraft.resources.ResourceLocation::getPath)
                .filter(path -> path.startsWith("molten_"))
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(
                java.util.Set.of("molten_calcite"),
                molten,
                "only molten calcite may enter the cell gate");
        assertEquals(
                CellContentGate.Kind.FLUID,
                CellContentGate.entries().get(
                        net.minecraft.resources.ResourceLocation.parse(
                                "cruciblecraft:molten_calcite")));
    }
}
