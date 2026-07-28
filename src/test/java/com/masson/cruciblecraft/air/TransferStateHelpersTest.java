package com.masson.cruciblecraft.air;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

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
    void requestIsLimitedOnlyByFiniteRoomAndSourceRate() {
        assertEquals(0.0F, AirIntakeCoordinator.request(0, 16));
        assertEquals(0.0F, AirIntakeCoordinator.request(100, Float.NaN));
        assertEquals(12.0F, AirIntakeCoordinator.request(12, 16));
        assertEquals(8.0F, AirIntakeCoordinator.request(12, 8));
    }

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

    @Test
    void crucibleExecutesEachAdjacentAirRequestOnlyOnce() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/CrucibleBlockEntity.java"));
        String method = source.substring(
                source.indexOf("private void pullAdjacentAir"),
                source.indexOf("public AirInjectionResult injectAir"));

        assertEquals(1, occurrences(method, "extractAir("));
        assertTrue(method.contains("extractAir(request, false)"));
        assertFalse(method.contains("extractAir(request, true)"));
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
