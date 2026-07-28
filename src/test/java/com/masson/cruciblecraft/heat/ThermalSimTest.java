package com.masson.cruciblecraft.heat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThermalSimTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void heatsTowardTargetAndStops() {
        assertEquals(22.5f, ThermalSim.step(20.0f, 1_400.0f, 2.5f), EPSILON);
        assertEquals(1_400.0f, ThermalSim.step(1_399.0f, 1_400.0f, 2.5f), EPSILON);
        assertEquals(1_400.0f, ThermalSim.step(1_400.0f, 1_400.0f, 2.5f), EPSILON);
    }

    @Test
    void coolsTowardTargetAndStops() {
        assertEquals(1_399.0f, ThermalSim.step(1_400.0f, 20.0f, 1.0f), EPSILON);
        assertEquals(20.0f, ThermalSim.step(20.5f, 20.0f, 1.0f), EPSILON);
        assertEquals(20.0f, ThermalSim.step(20.0f, 20.0f, 1.0f), EPSILON);
    }

    @Test
    void stepTicksScalesLinearlyAndClamps() {
        assertEquals(20.0f, ThermalSim.stepTicks(20.0f, 1_400.0f, 2.5f, 0), EPSILON);
        assertEquals(45.0f, ThermalSim.stepTicks(20.0f, 1_400.0f, 2.5f, 10), EPSILON);
        assertEquals(1_400.0f, ThermalSim.stepTicks(20.0f, 1_400.0f, 2.5f, 10_000), EPSILON);
        assertEquals(20.0f, ThermalSim.stepTicks(1_400.0f, 20.0f, 1.0f, 10_000), EPSILON);
    }

    @Test
    void nonPositiveRateDoesNotChangeTemperature() {
        assertEquals(500.0f, ThermalSim.step(500.0f, 1_400.0f, 0.0f), EPSILON);
        assertEquals(500.0f, ThermalSim.step(500.0f, 20.0f, -1.0f), EPSILON);
        assertEquals(500.0f, ThermalSim.stepTicks(500.0f, 1_400.0f, -1.0f, 100), EPSILON);
    }

    @Test
    void bulkStepMatchesRepeatedSingleSteps() {
        float repeated = 20.0f;
        for (int i = 0; i < 317; i++) {
            repeated = ThermalSim.step(repeated, 1_400.0f, 2.5f);
        }
        assertEquals(repeated, ThermalSim.stepTicks(20.0f, 1_400.0f, 2.5f, 317), EPSILON);
    }
}
