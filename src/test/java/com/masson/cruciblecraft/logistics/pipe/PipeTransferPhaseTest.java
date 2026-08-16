package com.masson.cruciblecraft.logistics.pipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class PipeTransferPhaseTest {
    @Test
    void fiveHundredPipesScheduleOnlyOneHundredCoverScansPerTick() {
        for (long tick = 0; tick < PipeTransferPhase.INTERVAL; tick++) {
            int due = 0;
            for (int pipe = 0; pipe < 500; pipe++) {
                if (PipeTransferPhase.isDue(
                        tick, new BlockPos(pipe, 64, 0))) {
                    due++;
                }
            }
            assertEquals(100, due);
        }
    }
}
