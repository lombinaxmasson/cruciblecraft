package com.masson.cruciblecraft.api.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnergyTypePacketContractTest {
    @Test
    void heatIsSizeIrrelevantAndEmitsPacketSizeOne() {
        assertTrue(EnergyType.HEAT.sizeIrrelevant());
        assertFalse(EnergyType.HEAT.allowsNegativeSize());
        assertEquals(1L, EnergyType.HEAT.emitPacketSize(24L));
        assertEquals(1L, EnergyType.HEAT.emitPacketSize(1L));
    }

    @Test
    void kineticAndElectricKeepCallerPacketSize() {
        assertFalse(EnergyType.KINETIC_ROTATION.sizeIrrelevant());
        assertTrue(EnergyType.KINETIC_ROTATION.allowsNegativeSize());
        assertEquals(32L, EnergyType.KINETIC_ROTATION.emitPacketSize(32L));
        assertEquals(128L, EnergyType.ELECTRIC.emitPacketSize(128L));
    }
}
