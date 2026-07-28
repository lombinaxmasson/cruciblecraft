package com.masson.cruciblecraft.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FireboxHeatBufferTest {
    @Test
    void fuelDepositsPreserveTotalEnergyAndEquivalentTime() {
        FireboxHeatBuffer buffer = new FireboxHeatBuffer();

        assertTrue(buffer.deposit(FuelDefinition.CHARCOAL));
        assertEquals(FuelDefinition.CHARCOAL.totalEnergy(), buffer.storedHeat());
        assertEquals(FuelDefinition.CHARCOAL.burnTicks(), buffer.equivalentTicks());
        assertEquals(80, buffer.equivalentSeconds());

        assertTrue(buffer.deposit(FuelDefinition.COAL_COKE));
        assertEquals(
                FuelDefinition.CHARCOAL.totalEnergy() + FuelDefinition.COAL_COKE.totalEnergy(),
                buffer.storedHeat());
        assertEquals(FuelDefinition.CHARCOAL.id(), buffer.fuelId());
    }

    @Test
    void depositsCapAtTheOldTwelveThousandTickLimit() {
        FireboxHeatBuffer buffer = new FireboxHeatBuffer();
        for (int index = 0; index < 4; index++) {
            assertTrue(buffer.deposit(FuelDefinition.COAL_COKE));
        }

        assertEquals(
                FireboxHeatBuffer.capacity(FuelDefinition.COAL_COKE.energyPerTick()),
                buffer.storedHeat());
        assertFalse(buffer.deposit(FuelDefinition.COAL_COKE));
        assertEquals(FireboxHeatBuffer.MAX_EQUIVALENT_TICKS, buffer.equivalentTicks());
    }

    @Test
    void cokeKeepsFireboxRateAndDoublesBurnDuration() {
        assertEquals(8.0F, FuelDefinition.COAL_COKE.energyPerTick());
        assertEquals(25_600.0, FuelDefinition.COAL_COKE.totalEnergy());
        assertEquals(3_200, FuelDefinition.COAL_COKE.burnTicks());
    }

    @Test
    void simulationAndExtractionRespectOutputRate() {
        FireboxHeatBuffer buffer = new FireboxHeatBuffer();
        buffer.deposit(FuelDefinition.CHARCOAL);
        double initial = buffer.storedHeat();

        assertEquals(8.0, buffer.extract(100.0, true));
        assertEquals(initial, buffer.storedHeat());
        assertEquals(8.0, buffer.extract(100.0, false));
        assertEquals(initial - 8.0, buffer.storedHeat());
    }

    @Test
    void rejectsFuelWithDifferentOutputRateWhileHeatRemains() {
        FireboxHeatBuffer buffer = new FireboxHeatBuffer();
        buffer.deposit(FuelDefinition.CHARCOAL);

        assertFalse(buffer.deposit(new FuelDefinition("hotter", 16.0F, 100)));
        assertEquals(FuelDefinition.CHARCOAL.totalEnergy(), buffer.storedHeat());
    }

    @Test
    void legacyMigrationMultipliesTicksBySavedFloatRateExactly() {
        float legacyRate = 8.25F;
        FireboxHeatBuffer buffer =
                FireboxHeatBuffer.migrateLegacy(12_345, legacyRate, "legacy");

        assertEquals((double) 12_345 * legacyRate, buffer.storedHeat());
        assertEquals(legacyRate, buffer.outputRate());
        assertEquals("legacy", buffer.fuelId());
    }
}
