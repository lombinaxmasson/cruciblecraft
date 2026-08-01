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
        assertEquals(8L, FuelDefinition.COAL_COKE.energyPerTick());
        assertEquals(25_600L, FuelDefinition.COAL_COKE.totalEnergy());
        assertEquals(3_200, FuelDefinition.COAL_COKE.burnTicks());
    }

    @Test
    void simulationAndExtractionRespectOutputRate() {
        FireboxHeatBuffer buffer = new FireboxHeatBuffer();
        buffer.deposit(FuelDefinition.CHARCOAL);
        long initial = buffer.storedHeat();

        assertEquals(8L, buffer.extract(100L, true));
        assertEquals(initial, buffer.storedHeat());
        assertEquals(8L, buffer.extract(100L, false));
        assertEquals(initial - 8L, buffer.storedHeat());
    }

    @Test
    void rejectsFuelWithDifferentOutputRateWhileHeatRemains() {
        FireboxHeatBuffer buffer = new FireboxHeatBuffer();
        buffer.deposit(FuelDefinition.CHARCOAL);

        assertFalse(buffer.deposit(new FuelDefinition("hotter", 16L, 100)));
        assertEquals(FuelDefinition.CHARCOAL.totalEnergy(), buffer.storedHeat());
    }

    @Test
    void legacyMigrationFloorsFractionalHuAndClampsCapacity() {
        float legacyRate = 8.25F;
        FireboxHeatBuffer buffer =
                FireboxHeatBuffer.migrateLegacy(12_345, legacyRate, "legacy");

        assertEquals(FireboxHeatBuffer.capacity(8L), buffer.storedHeat());
        assertEquals(8L, buffer.outputRate());
        assertEquals("legacy", buffer.fuelId());
    }
}
