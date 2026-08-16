package com.masson.cruciblecraft.air;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AirOutputModelTest {
    @Test
    void bellowsStrokePreservesThePreviousManualBurst() {
        assertEquals(16L, AirOutputModel.BELLOWS_AIR_PER_TICK);
        assertEquals(40, AirOutputModel.BELLOWS_STROKE_TICKS);
        assertEquals(640L, AirOutputModel.bellowsStrokeTotal());
    }

    @Test
    void steelSteamEngineUsesTheGt6DynamicOutputCurve() {
        float nominal = AirOutputModel.STEEL_STEAM_ENGINE_NOMINAL;
        assertEquals(1.0F, AirOutputModel.gt6SteamEngineOutput(nominal, 0));
        assertEquals(8.0F, AirOutputModel.gt6SteamEngineOutput(nominal, 7));
        assertEquals(16.0F, AirOutputModel.gt6SteamEngineOutput(nominal, 15));
        assertEquals(32.0F, AirOutputModel.gt6SteamEngineOutput(nominal, 31));
    }

    @Test
    void airBufferCapsStoredAndIncomingValues() {
        assertEquals(640L, AirOutputModel.addToBuffer(0L, 640L));
        assertEquals(1_200L, AirOutputModel.addToBuffer(1_190L, 16L));
        assertEquals(0L, AirOutputModel.clampStoredAir(-1L));
        assertEquals(1_200L, AirOutputModel.clampStoredAir(2_000L));
        assertEquals(639L, AirOutputModel.consumeProcessingTick(640L));
    }
}
