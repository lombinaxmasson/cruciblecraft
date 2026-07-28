package com.masson.cruciblecraft.air;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.machine.CheckpointDecisions;

import org.junit.jupiter.api.Test;

class TransferStateHelpersTest {
    @Test
    void bellowsHasOneSharedBudgetAndSimulationIsSideEffectFree() {
        PerTickAirLimiter limiter = new PerTickAirLimiter();

        assertEquals(16.0F, limiter.extract(10, 16.0F, 16.0F, true));
        assertEquals(16.0F, limiter.extract(10, 16.0F, 16.0F, true));
        assertEquals(10.0F, limiter.extract(10, 16.0F, 10.0F, false));
        assertEquals(6.0F, limiter.extract(10, 16.0F, 16.0F, false));
        assertEquals(0.0F, limiter.extract(10, 16.0F, 16.0F, false));
        assertEquals(16.0F, limiter.extract(11, 16.0F, 16.0F, false));
    }

    @Test
    void invalidColdOrClosedCrucibleRequestsNoAir() {
        assertEquals(0.0F, AirIntakeCoordinator.request(true, false, 100, 16));
        assertEquals(0.0F, AirIntakeCoordinator.request(false, true, 100, 16));
        assertEquals(0.0F, AirIntakeCoordinator.request(true, true, 0, 16));
        assertEquals(12.0F, AirIntakeCoordinator.request(true, true, 12, 16));
    }

    @Test
    void checkpointAndAuthoritativeSyncCadencesAreExplicit() {
        assertFalse(CheckpointDecisions.shouldCheckpoint(false, 20, 20));
        assertFalse(CheckpointDecisions.shouldCheckpoint(true, 19, 20));
        assertTrue(CheckpointDecisions.shouldCheckpoint(true, 20, 20));
        assertFalse(CheckpointDecisions.shouldSync(false, 100, 100));
        assertTrue(CheckpointDecisions.shouldSync(true, 100, 100));
    }

    @Test
    void clientInterpolationDoesNotAdvancePhysics() {
        var authoritative = new CrucibleThermalModel.StepResult(900, 37, 42);
        float display = CrucibleThermalModel.interpolateDisplay(800, authoritative.temperature(), 10);

        assertEquals(810, display);
        assertEquals(900, authoritative.temperature());
        assertEquals(37, authoritative.storedEnergy());
        assertEquals(42, authoritative.cooldownTicks());
    }

    @Test
    void boilingDecisionTriggersAtThresholdOnly() {
        assertFalse(CrucibleThermalModel.shouldBoil(999, 1_000));
        assertTrue(CrucibleThermalModel.shouldBoil(1_000, 1_000));
    }
}
