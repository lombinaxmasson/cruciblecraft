package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class LargeBoilerTierTest {
    @Test
    void exposesAllSourceBackedControllerRows() {
        assertEquals(5, LargeBoilerTier.values().length);
        assertEquals(8_192L, LargeBoilerTier.STAINLESS_STEEL.steamOutput());
        assertEquals(8_192L, LargeBoilerTier.INVAR.steamOutput());
        assertEquals(16_384L, LargeBoilerTier.TITANIUM.steamOutput());
        assertEquals(32_768L, LargeBoilerTier.TUNGSTENSTEEL.steamOutput());
        assertEquals(262_144L, LargeBoilerTier.ADAMANTIUM.steamOutput());
        assertEquals(
                LargeBoilerTier.STAINLESS_STEEL,
                LargeBoilerTier.byControllerId(
                        LargeBoilerTier.STAINLESS_STEEL.controllerId())
                        .orElseThrow());
        assertEquals(
                LargeBoilerTier.ADAMANTIUM.capacity(),
                2_621_440_000L);
        assertNotEquals(
                LargeBoilerTier.STAINLESS_STEEL.wallId(),
                LargeBoilerTier.INVAR.wallId());
    }

    @Test
    void keepsWaterCapacityIndependentFromSteamTier() {
        for (LargeBoilerTier tier : LargeBoilerTier.values()) {
            assertEquals(128_000L, LargeBoilerTier.WATER_CAPACITY);
            assertEquals(
                    tier.steamOutput() * 10_000L,
                    tier.capacity());
        }
    }
}
