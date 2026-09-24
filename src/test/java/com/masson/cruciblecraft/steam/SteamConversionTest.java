package com.masson.cruciblecraft.steam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;

import org.junit.jupiter.api.Test;

class SteamConversionTest {
    @Test void boilerConversionUsesExactIntegerBatches() {
        assertEquals(0, SteamConversion.boilerBatches(1, 159, 80));
        assertEquals(0, SteamConversion.boilerBatches(1, 160, 79));
        assertEquals(1, SteamConversion.boilerBatches(1, 160, 80));
        assertEquals(3, SteamConversion.boilerBatches(5, 500, 400));
    }
    @Test void engineConversionUsesExactSourceEfficiencyBatches() {
        assertEquals(0, SteamConversion.engineBatches(199));
        assertEquals(1, SteamConversion.engineBatches(200));
        assertEquals(1, SteamConversion.engineBatches(399));
        assertEquals(2, SteamConversion.engineBatches(400));
        assertEquals(200, SteamConversion.ENGINE_STEAM_PER_BATCH);
        assertEquals(50, SteamConversion.KU_PER_ENGINE_BATCH);
        assertEquals(
                4,
                SteamConversion.ENGINE_STEAM_PER_BATCH
                        / SteamConversion.KU_PER_ENGINE_BATCH);
        assertEquals(1, SteamConversion.EXHAUST_WATER_PER_BATCH);
        assertEquals("water_distilled", SteamConversion.DISTILLED_WATER_ID);
    }

    @Test void engineConversionUsesEachTierEfficiency() {
        assertEquals(
                30,
                SteamConversion.engineKuPerBatch(
                        EnergyConverterCatalog.require(
                                "cruciblecraft:lead_steam_engine")));
        assertEquals(
                64,
                SteamConversion.engineKuPerBatch(
                        EnergyConverterCatalog.require(
                                "cruciblecraft:invar_steam_engine")));
        assertEquals(
                63,
                SteamConversion.engineKuPerBatch(
                        EnergyConverterCatalog.require(
                                "cruciblecraft:chromium_strong_steam_engine")));
        assertEquals(
                120,
                SteamConversion.engineKuForBatches(
                        EnergyConverterCatalog.require(
                                "cruciblecraft:lead_steam_engine"),
                        4));
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
        assertEquals(8, buffer.discard(8));
        assertEquals(0, buffer.stored());
        assertEquals(1, buffer.strokeSign());
        buffer.addConverted(80);
        assertEquals(80, buffer.stored());
        assertEquals(0, buffer.room());
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
