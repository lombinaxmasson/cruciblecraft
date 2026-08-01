package com.masson.cruciblecraft.energy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class EnergyAllocationTest {
    @Test
    void conservesPacketsAndNeverExceedsDemand() {
        Random random = new Random(0x475436L);
        for (int sample = 0; sample < 2_000; sample++) {
            long amount = random.nextInt(2_000);
            long[] demands = new long[random.nextInt(12)];
            long totalDemand = 0L;
            for (int index = 0; index < demands.length; index++) {
                demands[index] = random.nextInt(500);
                totalDemand += demands[index];
            }

            long[] result = EnergyAllocation.distribute(amount, demands);
            long allocated = 0L;
            for (int index = 0; index < result.length; index++) {
                assertTrue(result[index] >= 0L);
                assertTrue(result[index] <= demands[index]);
                allocated += result[index];
            }
            assertEquals(Math.min(amount, totalDemand), allocated);
        }
    }

    @Test
    void isDeterministicAndDoesNotMutateDemands() {
        long[] demands = {7, 2, 11, 0};
        long[] unchanged = demands.clone();

        assertArrayEquals(
                EnergyAllocation.distribute(13, demands),
                EnergyAllocation.distribute(13, demands));
        assertArrayEquals(unchanged, demands);
    }

    @Test
    void equalDemandsDifferByAtMostOnePacket() {
        long[] result = EnergyAllocation.distribute(103, new long[] {100, 100, 100, 100, 100});
        long minimum = Long.MAX_VALUE;
        long maximum = Long.MIN_VALUE;
        for (long allocation : result) {
            minimum = Math.min(minimum, allocation);
            maximum = Math.max(maximum, allocation);
        }
        assertTrue(maximum - minimum <= 1L);
    }

    @Test
    void ignoresNegativeBudgetsAndDemands() {
        assertArrayEquals(
                new long[] {0, 0, 0},
                EnergyAllocation.distribute(-1, new long[] {10, -5, 3}));
        assertArrayEquals(
                new long[] {2, 0, 2},
                EnergyAllocation.distribute(4, new long[] {10, -5, 10}));
    }
}
