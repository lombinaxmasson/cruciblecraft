package com.masson.cruciblecraft.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BronzeDynamoEnergyTest {
    @Test
    void simulationIsSideEffectFreeAcrossBothBuffers() {
        BronzeDynamoEnergy energy = new BronzeDynamoEnergy();

        assertEquals(1L, energy.insertKinetic(-32L, 1L, true));
        assertEquals(0L, energy.kineticStored());
        assertEquals(1L, energy.insertKinetic(-32L, 1L, false));
        assertEquals(32L, energy.kineticStored());
        assertEquals(22L, energy.outputSize());

        assertEquals(1L, energy.extractElectric(22L, 1L, true));
        assertEquals(32L, energy.kineticStored());
        assertEquals(1L, energy.extractElectric(22L, 1L, false));
        assertEquals(0L, energy.kineticStored());
        assertEquals(32L, energy.kineticConsumed());
        assertEquals(22L, energy.electricExtracted());
        assertEquals(10L, energy.conversionLoss());
    }

    @Test
    void sourceWindowEfficiencyAndWastePolicyAreExact() {
        BronzeDynamoEnergy energy = new BronzeDynamoEnergy();

        assertEquals(0L, energy.insertKinetic(15L, 1L, false));
        assertEquals(1L, energy.insertKinetic(16L, 1L, false));
        assertEquals(0L, energy.outputSize());
        assertFalse(energy.wasteBlockedInput());
        assertEquals(1L, energy.insertKinetic(16L, 1L, false));
        assertEquals(22L, energy.outputSize());
        assertTrue(energy.wasteBlockedInput());
        assertEquals(32L, energy.kineticConsumed());
        assertEquals(0L, energy.electricExtracted());
        assertEquals(32L, energy.conversionLoss());

        assertEquals(1L, energy.insertKinetic(64L, 1L, false));
        assertEquals(44L, energy.outputSize());
        assertEquals(1L, energy.extractElectric(44L, 1L, false));
        assertEquals(
                energy.kineticConsumed(),
                energy.electricExtracted() + energy.conversionLoss());

        assertEquals(1L, energy.insertKinetic(65L, 1L, true));
        assertFalse(energy.overloaded());
        assertEquals(1L, energy.insertKinetic(65L, 1L, false));
        assertTrue(energy.overloaded());
        assertEquals(0L, energy.kineticStored());
    }

    @Test
    void snapshotRestoresInputAndAuditableConservationTotals() {
        BronzeDynamoEnergy source = new BronzeDynamoEnergy();
        assertEquals(1L, source.insertKinetic(32L, 1L, false));
        assertEquals(1L, source.extractElectric(22L, 1L, false));
        assertEquals(1L, source.insertKinetic(16L, 1L, false));

        BronzeDynamoEnergy restored = new BronzeDynamoEnergy();
        restored.restore(source.snapshot());

        assertEquals(16L, restored.kineticStored());
        assertEquals(32L, restored.kineticConsumed());
        assertEquals(22L, restored.electricExtracted());
        assertEquals(10L, restored.conversionLoss());
        assertEquals(0L, restored.outputSize());
    }
}
