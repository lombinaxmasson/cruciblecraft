package com.masson.cruciblecraft.air;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AirOutputModelTest {
    @Test
    void bellowsStrokePreservesThePreviousManualBurst() {
        assertEquals(16.0F, AirOutputModel.BELLOWS_AIR_PER_TICK);
        assertEquals(40, AirOutputModel.BELLOWS_STROKE_TICKS);
        assertEquals(640.0F, AirOutputModel.bellowsStrokeTotal());
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
    void airBufferCapsMigratedAndIncomingValues() {
        assertEquals(640.0F, AirOutputModel.addToBuffer(0.0F, 640.0F));
        assertEquals(1_200.0F, AirOutputModel.addToBuffer(1_190.0F, 16.0F));
        assertEquals(0.0F, AirOutputModel.clampStoredAir(Float.NaN));
        assertEquals(1_200.0F, AirOutputModel.clampStoredAir(2_000.0F));
        assertEquals(639.0F, AirOutputModel.consumeProcessingTick(640.0F));
    }
}
