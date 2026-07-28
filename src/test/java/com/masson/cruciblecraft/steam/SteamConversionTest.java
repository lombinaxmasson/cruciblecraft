package com.masson.cruciblecraft.steam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SteamConversionTest {
    @Test void boilerConversionUsesExactIntegerBatches() {
        assertEquals(0, SteamConversion.boilerBatches(1, 159, 80));
        assertEquals(0, SteamConversion.boilerBatches(1, 160, 79));
        assertEquals(1, SteamConversion.boilerBatches(1, 160, 80));
        assertEquals(3, SteamConversion.boilerBatches(5, 500, 400));
    }
    @Test void engineConversionKeepsOddSteamBuffered() {
        assertEquals(0, SteamConversion.kineticFromSteam(1, 16));
        assertEquals(16, SteamConversion.kineticFromSteam(33, 16));
    }
    @Test void extractionSimulationDoesNotMutateAndRateLimits() {
        KineticBuffer buffer = new KineticBuffer(64, 16);
        assertEquals(40, buffer.insert(40));
        assertEquals(1, buffer.strokeSign());
        assertEquals(16, buffer.extract(32, true));
        assertEquals(40, buffer.stored());
        assertEquals(1, buffer.strokeSign());
        assertEquals(16, buffer.extract(32, false));
        assertEquals(24, buffer.stored());
        assertEquals(-1, buffer.strokeSign());
        assertEquals(16, buffer.extract(32, false));
        assertEquals(1, buffer.strokeSign());
    }

    @Test void eightHuSourcesAccumulateWithoutFractionalLoss() {
        int accumulated = 0;
        for (int tick = 0; tick < 9; tick++) {
            accumulated += 8;
            assertEquals(0, SteamConversion.boilerBatches(1, 160, accumulated));
        }
        accumulated += 8;
        assertEquals(1, SteamConversion.boilerBatches(1, 160, accumulated));
        assertEquals(0, accumulated - SteamConversion.HU_PER_BATCH);
    }
}
