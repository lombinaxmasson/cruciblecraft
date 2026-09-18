package com.masson.cruciblecraft.energy.cable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElectricWireRemainderIdsTest {
    @Test
    void parsesGoldAndLeadOddGauges() {
        var gold = ElectricWireRemainderIds.parse("electric_wire/3x_gold_wire")
                .orElseThrow();
        assertEquals(3, gold.strands());
        assertEquals("gold", gold.materialId());
        assertEquals("wireGt03", gold.specification());
        assertEquals(
                "conductor/wiregt03_item",
                ElectricWireRemainderIds.itemModelParent(gold));

        var lead = ElectricWireRemainderIds.parse("electric_wire/15x_lead_wire")
                .orElseThrow();
        assertEquals("lead", lead.materialId());
        assertEquals("wireGt15", lead.specification());
    }

    @Test
    void parsesKnownCableRemainder() {
        var cable = ElectricWireRemainderIds.parse(
                        "electric_wire/12x_yttrium_barium_cuprate_cable")
                .orElseThrow();
        assertTrue(cable.cable());
        assertEquals("yttrium_barium_cuprate", cable.materialId());
        assertEquals("cableGt12", cable.specification());
    }
}
