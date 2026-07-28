package com.masson.cruciblecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SteelmakingProcessTest {
    @Test
    void acceptsOnlyExactThreeToOneHighCarbonCharges() {
        var batch = SteelmakingProcess.begin(Map.of("iron", 432, "carbon", 144));

        assertTrue(batch.isPresent());
        assertEquals(3, batch.orElseThrow().minimumSteelCarbonUnits());
        assertEquals(6, batch.orElseThrow().maximumSteelCarbonUnits());
        assertFalse(SteelmakingProcess.begin(Map.of("iron", 432, "carbon", 108)).isPresent());
        assertFalse(SteelmakingProcess.begin(Map.of("iron", 144, "carbon", 48)).isPresent());
        assertFalse(SteelmakingProcess.begin(
                Map.of("iron", 432, "carbon", 144, "tin", 1)).isPresent());
    }

    @Test
    void consumesCarbonUntilTheSteelRangeIsReached() {
        var batch = SteelmakingProcess.begin(Map.of("iron", 432, "carbon", 144))
                .orElseThrow();
        int carbon = 144;
        int cycles = 0;
        SteelmakingProcess.CarbonStep step;
        do {
            step = SteelmakingProcess.consumeCarbon(batch, carbon);
            carbon = step.remainingCarbonUnits();
            cycles++;
        } while (!step.steelRangeReached());

        assertEquals(6, carbon);
        assertEquals(14, cycles);
    }

    @Test
    void oneBellowsBurstCanProcessTheLargestValidCrucibleBatch() {
        var batch = SteelmakingProcess.begin(Map.of("iron", 864, "carbon", 288))
                .orElseThrow();
        int carbon = 288;
        int cycles = 0;
        while (carbon > batch.maximumSteelCarbonUnits()) {
            carbon = SteelmakingProcess.consumeCarbon(batch, carbon).remainingCarbonUnits();
            cycles++;
        }

        assertEquals(12, carbon);
        assertTrue(
                cycles * SteelmakingProcess.REACTION_INTERVAL_TICKS
                        <= com.masson.cruciblecraft.air.AirOutputModel.bellowsStrokeTotal());
    }
}
