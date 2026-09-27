package com.masson.cruciblecraft.steam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SteamEngineKuCurveTest {
    @Test
    void bronzeNominalTwelveSpansInclusiveSixToTwentyFour() {
        assertEquals(0L, SteamEngineKuCurve.outputKu(12L, 0));
        assertEquals(6L, SteamEngineKuCurve.outputKu(12L, 7));
        assertEquals(12L, SteamEngineKuCurve.outputKu(12L, 15));
        assertEquals(24L, SteamEngineKuCurve.outputKu(12L, 31));
        assertEquals(24L, SteamEngineKuCurve.maximumKu(12L));
        assertFalse(SteamEngineKuCurve.activelyEmitting(7L, 6L, 12L));
        assertTrue(SteamEngineKuCurve.activelyEmitting(8L, 7L, 12L));
        assertFalse(SteamEngineKuCurve.activelyEmitting(6L, 6L, 12L));
        assertFalse(SteamEngineKuCurve.activelyEmitting(5L, 1L, 12L));
        assertTrue(SteamEngineKuCurve.activelyEmitting(25L, 24L, 12L));
    }

    @Test
    void visualStateTracksStoredOverCapacity() {
        assertEquals(0, SteamEngineKuCurve.visualState(0L, 1_024L));
        assertEquals(1, SteamEngineKuCurve.visualState(1L, 1_024L));
        assertEquals(7, SteamEngineKuCurve.visualState(224L, 1_024L));
        assertEquals(15, SteamEngineKuCurve.visualState(480L, 1_024L));
        assertEquals(31, SteamEngineKuCurve.visualState(1_024L, 1_024L));
        assertEquals(31, SteamEngineKuCurve.visualState(23_999L, 24_000L));
    }

    @Test
    void fullBufferVentsOnlyAfterTheLatchedStateIsAlreadyHot() {
        assertFalse(SteamEngineKuCurve.ventsWhenFull(26));
        assertFalse(SteamEngineKuCurve.ventsWhenFull(30));
        assertTrue(SteamEngineKuCurve.ventsWhenFull(31));
    }
}
