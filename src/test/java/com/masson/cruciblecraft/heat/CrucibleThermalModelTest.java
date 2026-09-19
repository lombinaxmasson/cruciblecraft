package com.masson.cruciblecraft.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.content.sensor.ItemMass;
import com.masson.cruciblecraft.machine.CheckpointDecisions;

import org.junit.jupiter.api.Test;

class CrucibleThermalModelTest {
    private static final double CLAY_CASING_WEIGHT = 1.9 * 7.0 * ItemMass.CM3_PER_INGOT;
    private static final double IRON_INGOT_WEIGHT = 7.874 * ItemMass.CM3_PER_INGOT;

    @Test
    void fullerChargeNeedsMoreHuPerKelvin() {
        assertEquals(15L, CrucibleThermalModel.requiredEnergyPerDegree(CLAY_CASING_WEIGHT));
        assertEquals(
                155L,
                CrucibleThermalModel.requiredEnergyPerDegree(
                        CLAY_CASING_WEIGHT + 16.0 * IRON_INGOT_WEIGHT));
        float empty = heat(CLAY_CASING_WEIGHT, 16L, 200);
        float full = heat(CLAY_CASING_WEIGHT + 16.0 * IRON_INGOT_WEIGHT, 16L, 200);
        assertTrue(full < empty - 50.0F);
    }

    @Test
    void retainsEnergyRemaindersAndUsesGt6CooldownCadence() {
        var first = CrucibleThermalModel.step(20, 0, 100, 5, 350.0, 20);
        var second = CrucibleThermalModel.step(
                first.temperature(),
                first.storedEnergy(),
                first.cooldownTicks(),
                5,
                350.0,
                20);
        assertEquals(21, first.temperature());
        assertEquals(1, first.storedEnergy());
        assertEquals(22, second.temperature());
        assertEquals(2, second.storedEnergy());

        var cooling = new CrucibleThermalModel.StepResult(1_000, 0, 100);
        for (int tick = 0; tick < 100; tick++) {
            cooling = CrucibleThermalModel.step(
                    cooling.temperature(),
                    cooling.storedEnergy(),
                    cooling.cooldownTicks(),
                    0,
                    CLAY_CASING_WEIGHT,
                    20);
        }
        assertEquals(999, cooling.temperature());
        assertEquals(10, cooling.cooldownTicks());
    }

    @Test
    void stoppedHeatClearsRemainderAndEventuallyBecomesQuiescent() {
        float ambient = 20.25F;
        var state = CrucibleThermalModel.step(
                ambient,
                0,
                0,
                3,
                100.0,
                ambient);
        assertTrue(state.temperature() > ambient);
        assertTrue(state.storedEnergy() > 0.0);

        state = CrucibleThermalModel.step(
                state.temperature(),
                state.storedEnergy(),
                state.cooldownTicks(),
                0,
                100.0,
                ambient);
        assertEquals(0.0, state.storedEnergy());

        for (int tick = 0; tick < 2_000; tick++) {
            state = CrucibleThermalModel.step(
                    state.temperature(),
                    state.storedEnergy(),
                    state.cooldownTicks(),
                    0,
                    100.0,
                    ambient);
        }

        assertEquals(ambient, state.temperature());
        assertEquals(0.0, state.storedEnergy());
        assertEquals(0, state.cooldownTicks());
        boolean quiescent = CrucibleThermalModel.isQuiescent(
                state.temperature(),
                state.storedEnergy(),
                state.cooldownTicks(),
                0,
                ambient);
        assertTrue(quiescent);
        assertFalse(CheckpointDecisions.shouldSync(!quiescent, 40, 0, 20));
        assertFalse(CheckpointDecisions.shouldCheckpoint(false, 40, 0, 20));
    }

    @Test
    void mixesInsertedMaterialAtItsActualTemperature() {
        assertEquals(
                600,
                CrucibleThermalModel.mixTemperature(1_000, 3, 200, 3));
    }

    @Test
    void coldIngotCoolsAHotChargeByGt6MassMix() {
        float mixed = CrucibleThermalModel.mixTemperature(
                1_000.0F,
                CLAY_CASING_WEIGHT,
                20.0F,
                IRON_INGOT_WEIGHT);
        assertEquals(635.0F, mixed);
        assertTrue(mixed < 1_000.0F - 300.0F);
    }

    @Test
    void gt6AmbientFloorIsCappedAtTwoHundred() {
        var result = CrucibleThermalModel.step(20, 0, 100, 0, CLAY_CASING_WEIGHT, 500);
        assertEquals(200, result.temperature());
    }

    private static float heat(double weight, long incoming, int ticks) {
        var state = new CrucibleThermalModel.StepResult(20, 0, 100);
        for (int tick = 0; tick < ticks; tick++) {
            state = CrucibleThermalModel.step(
                    state.temperature(),
                    state.storedEnergy(),
                    state.cooldownTicks(),
                    incoming,
                    weight,
                    20);
        }
        return state.temperature();
    }
}
