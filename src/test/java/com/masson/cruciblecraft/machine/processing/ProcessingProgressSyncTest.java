package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProcessingProgressSyncTest {
    @Test
    void sixtyFourTickRecipeAdvancesBetweenTwentyTickCheckpoints() {
        ProcessingProgressSync sync = new ProcessingProgressSync();
        int previous = 0;
        int[] observed = new int[65];

        for (int tick = 1; tick <= 64; tick++) {
            int current = sync.update("test:short", tick, 64L, false);
            observed[tick] = current;
            assertEquals(tick * 1_000 / 64, current);
            assertTrue(current > previous, "progress stalled at tick " + tick);
            previous = current;
        }

        assertTrue(observed[21] > observed[20]);
        assertTrue(observed[41] > observed[40]);
        assertTrue(observed[61] > observed[60]);
        assertEquals(1_000, observed[64]);
    }

    @Test
    void pauseIsStableAndRecipeOrWorkCycleChangesCanReset() {
        ProcessingProgressSync sync = new ProcessingProgressSync();

        assertEquals(250, sync.update("test:first", 16L, 64L, false));
        assertEquals(250, sync.update("test:first", 16L, 64L, false));
        assertEquals(375, sync.update("test:first", 24L, 64L, false));
        assertEquals(125, sync.update("test:second", 8L, 64L, false));
        assertEquals(0, sync.update("test:second", 0L, 64L, false));
        assertEquals(0, sync.update("test:invalid", 10L, 0L, false));
        assertEquals(0, sync.update("", 0L, 0L, false));
    }

    @Test
    void completionIsExactThenTheExistingLifecycleResets() {
        ProcessingProgressSync sync = new ProcessingProgressSync();

        assertEquals(984, sync.update("test:short", 63L, 64L, false));
        assertEquals(1_000, sync.update("test:short", 64L, 64L, true));
        assertEquals(0, sync.update("", 0L, 0L, false));
        assertEquals(15, sync.update("test:short", 1L, 64L, false));
    }

    @Test
    void longWorkAndInvalidDurationsStayBoundedWithoutOverflow() {
        long required = 4L * Integer.MAX_VALUE;

        assertEquals(0, ProcessingProgressSync.permille(1L, 0L));
        assertEquals(0, ProcessingProgressSync.permille(-1L, required));
        assertEquals(500, ProcessingProgressSync.permille(required / 2L, required));
        assertEquals(
                999,
                ProcessingProgressSync.permille(
                        Long.MAX_VALUE - 1L, Long.MAX_VALUE));
        assertEquals(
                1_000,
                ProcessingProgressSync.permille(
                        Long.MAX_VALUE, Long.MAX_VALUE));
    }
}
