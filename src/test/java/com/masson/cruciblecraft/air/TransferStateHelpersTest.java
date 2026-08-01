package com.masson.cruciblecraft.air;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.machine.CheckpointDecisions;

import org.junit.jupiter.api.Test;

class TransferStateHelpersTest {
    @Test
    void checkpointAndSyncCadencesArePositionPhased() {
        assertFalse(CheckpointDecisions.shouldCheckpoint(false, 7, 7, 20));
        assertFalse(CheckpointDecisions.shouldCheckpoint(true, 6, 7, 20));
        assertTrue(CheckpointDecisions.shouldCheckpoint(true, 7, 7, 20));
        assertTrue(CheckpointDecisions.shouldCheckpoint(true, 27, 7, 20));
        assertTrue(CheckpointDecisions.shouldCheckpoint(true, -13, 7, 20));
        assertTrue(CheckpointDecisions.shouldCheckpoint(true, 17, -3, 20));
        assertFalse(CheckpointDecisions.shouldSync(false, 7, 7, 20));
        assertTrue(CheckpointDecisions.shouldSync(true, 7, 7, 20));

        int checkpointsAtTick = 0;
        for (long positionKey = 0; positionKey < 20; positionKey++) {
            if (CheckpointDecisions.shouldCheckpoint(true, 5, positionKey, 20)) {
                checkpointsAtTick++;
            }
        }
        assertEquals(1, checkpointsAtTick);
    }

    @Test
    void coordinatePhaseKeyUsesAllTwentyPhasesOnOneLayer() {
        boolean[] phases = new boolean[20];
        for (int z = 0; z < 20; z++) {
            long key = CheckpointDecisions.phaseKey(0, 64, z);
            phases[Math.floorMod(key, phases.length)] = true;
        }
        for (boolean phase : phases) {
            assertTrue(phase);
        }
        assertEquals((64L + 7L * 31L) * 31L + 3L, CheckpointDecisions.phaseKey(3, 64, 7));
    }

    @Test
    void clientInterpolationDoesNotAdvancePhysics() {
        var authoritative = new CrucibleThermalModel.StepResult(900, 37, 42);
        float display = CrucibleThermalModel.interpolateDisplay(800, authoritative.temperature(), 20);

        assertEquals(805, display);
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
