package com.masson.cruciblecraft.content.item.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

class PocketMultitoolModeTest {
    @Test
    void cycleMatchesGt6MetadataOrder() {
        assertEquals(PocketMultitoolMode.KNIFE, PocketMultitoolMode.CLOSED.next());
        assertEquals(PocketMultitoolMode.SAW, PocketMultitoolMode.KNIFE.next());
        assertEquals(PocketMultitoolMode.FILE, PocketMultitoolMode.SAW.next());
        assertEquals(PocketMultitoolMode.SCREWDRIVER, PocketMultitoolMode.FILE.next());
        assertEquals(
                PocketMultitoolMode.WIRE_CUTTER,
                PocketMultitoolMode.SCREWDRIVER.next());
        assertEquals(
                PocketMultitoolMode.SCISSORS,
                PocketMultitoolMode.WIRE_CUTTER.next());
        assertEquals(PocketMultitoolMode.CHISEL, PocketMultitoolMode.SCISSORS.next());
        assertEquals(PocketMultitoolMode.CLOSED, PocketMultitoolMode.CHISEL.next());
    }

    @Test
    void closedHasNoInheritedKind() {
        assertNull(PocketMultitoolMode.CLOSED.kind());
        assertEquals(ToolKind.KNIFE, PocketMultitoolMode.KNIFE.kind());
        assertEquals(ToolKind.CHISEL, PocketMultitoolMode.CHISEL.kind());
    }
}
