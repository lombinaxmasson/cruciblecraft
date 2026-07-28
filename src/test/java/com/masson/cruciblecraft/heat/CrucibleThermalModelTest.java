package com.masson.cruciblecraft.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.machine.CheckpointDecisions;

import org.junit.jupiter.api.Test;

class CrucibleThermalModelTest {
    private static final double CLAY_CASING_WEIGHT = 1.9 * 800.0;

    @Test
    void charcoalMeltsBronzeButNotIronOrClayCasing() {
        float emptyTemperature = burnCharcoal(CLAY_CASING_WEIGHT);
        double bronzeWeight = CLAY_CASING_WEIGHT
                + 432 * 0.15 * 8.96
                + 144 * 0.15 * 7.31;
        float bronzeTemperature = burnCharcoal(bronzeWeight);
        double ironWeight = CLAY_CASING_WEIGHT + 144 * 0.15 * 7.87;
        float ironTemperature = burnCharcoal(ironWeight);

        assertTrue(emptyTemperature < 1727 * 1.10);
        assertTrue(bronzeTemperature >= 950);
        assertTrue(ironTemperature < 1538);
    }

    @Test
    void retainsEnergyRemaindersAndUsesGt6CooldownCadence() {
        var first = CrucibleThermalModel.step(20, 0, 100, 5, CLAY_CASING_WEIGHT, 20);
        var second = CrucibleThermalModel.step(
                first.temperature(),
                first.storedEnergy(),
                first.cooldownTicks(),
                5,
                CLAY_CASING_WEIGHT,
                20);
        assertEquals(20, first.temperature());
        assertEquals(5, first.storedEnergy());
        assertEquals(21, second.temperature());
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
                10,
                CLAY_CASING_WEIGHT,
                ambient);
        assertTrue(state.temperature() > ambient);
        assertTrue(state.storedEnergy() > 0.0);

        state = CrucibleThermalModel.step(
                state.temperature(),
                state.storedEnergy(),
                state.cooldownTicks(),
                0,
                CLAY_CASING_WEIGHT,
                ambient);
        assertEquals(0.0, state.storedEnergy());

        for (int tick = 0; tick < 2_000; tick++) {
            state = CrucibleThermalModel.step(
                    state.temperature(),
                    state.storedEnergy(),
                    state.cooldownTicks(),
                    0,
                    CLAY_CASING_WEIGHT,
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
    void gt6AmbientFloorIsCappedAtTwoHundred() {
        var result = CrucibleThermalModel.step(20, 0, 100, 0, CLAY_CASING_WEIGHT, 500);
        assertEquals(200, result.temperature());
    }

    @Test
    void coalCokeKeepsFireboxThroughputAndDoublesFuelEnergy() {
        assertEquals(
                FuelDefinition.CHARCOAL.energyPerTick(),
                FuelDefinition.COAL_COKE.energyPerTick());
        assertEquals(
                FuelDefinition.CHARCOAL.totalEnergy() * 2.0,
                FuelDefinition.COAL_COKE.totalEnergy());
    }

    private static float burnCharcoal(double weight) {
        var state = new CrucibleThermalModel.StepResult(20, 0, 100);
        for (int tick = 0; tick < FuelDefinition.CHARCOAL.burnTicks(); tick++) {
            state = CrucibleThermalModel.step(
                    state.temperature(),
                    state.storedEnergy(),
                    state.cooldownTicks(),
                    FuelDefinition.CHARCOAL.energyPerTick(),
                    weight,
                    20);
        }
        return state.temperature();
    }
}
