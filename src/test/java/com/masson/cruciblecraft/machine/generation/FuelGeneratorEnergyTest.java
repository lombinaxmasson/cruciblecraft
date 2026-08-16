package com.masson.cruciblecraft.machine.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FuelGeneratorEnergyTest {
    @Test
    void recipePowerIsConservedAcrossSmallerSourcePackets() {
        FuelGeneratorEnergy energy =
                new FuelGeneratorEnergy(16L, 65_536L);

        for (int tick = 0; tick < 8; tick++) {
            assertTrue(energy.canGenerate(64L));
            energy.generate(64L);
        }
        assertEquals(512L, energy.generated());
        assertEquals(512L, energy.stored());
        assertEquals(32L, energy.extract(16L, 64L, true));
        assertEquals(512L, energy.stored());
        assertEquals(32L, energy.extract(16L, 64L, false));
        assertEquals(0L, energy.stored());
        assertEquals(512L, energy.extracted());
        assertEquals(
                energy.generated(),
                energy.stored() + energy.extracted());
    }

    @Test
    void fullBufferRejectsGenerationWithoutPartialMutation() {
        FuelGeneratorEnergy energy =
                new FuelGeneratorEnergy(16L, 64L);
        energy.generate(64L);

        assertFalse(energy.canGenerate(1L));
        assertThrows(
                IllegalStateException.class,
                () -> energy.generate(1L));
        assertEquals(
                new FuelGeneratorEnergy.State(64L, 64L, 0L),
                energy.snapshot());
    }

    @Test
    void simulationAndRestorePreserveAuditState() {
        FuelGeneratorEnergy source =
                new FuelGeneratorEnergy(16L, 1_024L);
        source.generate(96L);
        assertEquals(2L, source.extract(16L, 2L, true));
        assertEquals(96L, source.stored());
        assertEquals(2L, source.extract(16L, 2L, false));

        FuelGeneratorEnergy restored =
                new FuelGeneratorEnergy(16L, 1_024L);
        restored.restore(source.snapshot());
        assertEquals(source.snapshot(), restored.snapshot());
        assertEquals(64L, restored.stored());
        assertEquals(96L, restored.generated());
        assertEquals(32L, restored.extracted());
    }
}
