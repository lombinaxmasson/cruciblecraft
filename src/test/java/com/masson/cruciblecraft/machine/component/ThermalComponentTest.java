package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import org.junit.jupiter.api.Test;

class ThermalComponentTest {
    @Test
    void queuesPacketsAndConsumesPendingHeatOnce() {
        ThermalComponent thermal = new ThermalComponent(20.0F);
        assertEquals(3L, thermal.queueHeat(2L, 3L, true));
        assertEquals(0L, thermal.pendingHeat());
        assertEquals(3L, thermal.queueHeat(2L, 3L, false));
        assertEquals(6L, thermal.totalStoredHeat());
        assertEquals(6L, thermal.takePendingHeat());
        assertEquals(0L, thermal.takePendingHeat());
    }

    @Test
    void advancesWithExplicitMassAndRestoresSanitizedState() {
        ThermalComponent thermal = new ThermalComponent(20.0F);
        thermal.advance(2L, 200.0);
        assertEquals(21.0F, thermal.authoritativeTemperature());
        assertEquals(CrucibleThermalModel.HOT_BUFFER_TICKS, thermal.cooldownTicks());
        assertFalse(thermal.isQuiescent());

        thermal.restore(Float.NaN, -2L, -3L, Integer.MAX_VALUE, false);
        assertEquals(20.0F, thermal.authoritativeTemperature());
        assertEquals(0L, thermal.totalStoredHeat());
        assertEquals(CrucibleThermalModel.HOT_BUFFER_TICKS, thermal.cooldownTicks());
    }

    @Test
    void mixesTemperatureWithoutOwningContentOrCasing() {
        ThermalComponent thermal = new ThermalComponent(20.0F);
        thermal.mixWith(100.0F, 3.0, 1.0);
        assertEquals(40.0F, thermal.authoritativeTemperature());
        assertTrue(Float.isFinite(thermal.authoritativeTemperature()));
    }

    @Test
    void coolingPacketsSubtractFromTheSignedAccumulator() {
        ThermalComponent thermal = new ThermalComponent(20.0F);
        assertEquals(4L, thermal.queueHeat(1L, 4L, false));
        assertEquals(4L, thermal.pendingHeat());
        assertEquals(3L, thermal.queueCooling(1L, 3L, false));
        assertEquals(1L, thermal.takePendingHeat());
    }
}
