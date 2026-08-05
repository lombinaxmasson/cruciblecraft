package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class FluidPipeSyncScheduleTest {
    @Test
    void fiveHundredLinearPipesHaveOneHundredPacketSlotsPerTick() {
        for (long tick = 0;
                tick < FluidPipeBlockEntity.CLIENT_SYNC_INTERVAL;
                tick++) {
            int due = 0;
            for (int pipe = 0; pipe < 500; pipe++) {
                if (FluidPipeBlockEntity.isClientSyncTick(
                        tick, new BlockPos(pipe, 64, 0))) {
                    due++;
                }
            }
            assertEquals(100, due);
        }
    }
}
