package com.masson.cruciblecraft.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BronzeDynamoEnergyTest {
    @Test
    void simulationIsSideEffectFreeAcrossBothBuffers() {
        BronzeDynamoEnergy energy = new BronzeDynamoEnergy();

        assertEquals(1L, energy.insertKinetic(-24L, 1L, true));
        assertEquals(0L, energy.kineticStored());
        assertEquals(1L, energy.insertKinetic(-24L, 1L, false));
        assertEquals(24L, energy.kineticStored());

        assertTrue(energy.convertOnePacket());
        assertEquals(0L, energy.kineticStored());
        assertEquals(24L, energy.electricStored());
        assertEquals(1L, energy.extractElectric(24L, 1L, true));
        assertEquals(24L, energy.electricStored());
        assertEquals(1L, energy.extractElectric(24L, 1L, false));
        assertEquals(0L, energy.electricStored());
    }

    @Test
    void fixedPacketSpecRejectsOtherStrengthsAndPartialConversions() {
        BronzeDynamoEnergy energy = new BronzeDynamoEnergy();

        assertEquals(0L, energy.insertKinetic(23L, 1L, false));
        assertEquals(0L, energy.insertKinetic(25L, 1L, false));
        assertFalse(energy.convertOnePacket());
        assertEquals(0L, energy.extractElectric(-24L, 1L, false));
    }

    @Test
    void snapshotRestoresBothSidesOfAnInFlightConversion() {
        BronzeDynamoEnergy source = new BronzeDynamoEnergy();
        assertEquals(2L, source.insertKinetic(24L, 2L, false));
        assertTrue(source.convertOnePacket());

        BronzeDynamoEnergy restored = new BronzeDynamoEnergy();
        restored.restore(source.snapshot());

        assertEquals(24L, restored.kineticStored());
        assertEquals(24L, restored.electricStored());
        assertTrue(restored.convertOnePacket());
        assertEquals(0L, restored.kineticStored());
        assertEquals(48L, restored.electricStored());
    }
}
