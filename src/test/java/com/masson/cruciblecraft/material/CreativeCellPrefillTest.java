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
        assertEquals(110, entries.size());

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
    void gateContainsNoMoltenFluids() {
        // 4.5 disposition: molten fluids stay out of cells; the closed gate
        // must not silently grow molten rows.
        assertTrue(
                CellContentGate.entries().keySet().stream()
                        .noneMatch(id -> id.getPath().startsWith("molten_")),
                "molten fluids must not enter the cell gate");
    }
}
