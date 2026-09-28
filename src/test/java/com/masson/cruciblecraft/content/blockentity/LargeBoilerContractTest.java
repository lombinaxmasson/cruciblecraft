package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LargeBoilerContractTest {
    private static final long STAINLESS_CAPACITY =
            LargeBoilerTier.STAINLESS_STEEL.capacity();

    @Test
    void barometerUsesGt6Scale() {
        assertEquals(0, LargeBoilerBlockEntity.barometerScale(0L, STAINLESS_CAPACITY));
        assertEquals(31, LargeBoilerBlockEntity.barometerScale(
                STAINLESS_CAPACITY, STAINLESS_CAPACITY));
        assertEquals(1, LargeBoilerBlockEntity.barometerScale(1L, STAINLESS_CAPACITY));
        assertEquals(16, LargeBoilerBlockEntity.barometerScale(
                STAINLESS_CAPACITY / 2L, STAINLESS_CAPACITY));
        assertEquals(4, LargeBoilerBlockEntity.barometerScale(
                10_922_666L, STAINLESS_CAPACITY));
        assertEquals(5, LargeBoilerBlockEntity.barometerScale(
                10_922_667L, STAINLESS_CAPACITY));
    }

    @Test
    void steamUsesGt6Units() {
        // gt6-source: MultiTileEntityLargeBoiler.java:187 units(conversions, 10000, efficiency * 160, false)
        assertEquals(480L, LargeBoilerBlockEntity.steamProduced(3L, 10_000));
        // gt6-source: MultiTileEntityLargeBoiler.java:187 units(conversions, 10000, efficiency * 160, false)
        assertEquals(240L, LargeBoilerBlockEntity.steamProduced(3L, 5_000));
        assertEquals(1_599L, LargeBoilerBlockEntity.steamProduced(10L, 9_999));
        // gt6-source: MultiTileEntityLargeBoiler.java:187 units(conversions, 10000, efficiency * 160, false)
        assertEquals(15_800L, LargeBoilerBlockEntity.steamProduced(100L, 9_875));
        assertEquals(0L, LargeBoilerBlockEntity.steamProduced(0L, 10_000));
    }

    @Test
    void pressurizedStructureRechecksImmediately() {
        assertTrue(LargeBoilerBlockEntity.recheckStructureNow(true, false, false));
        assertTrue(LargeBoilerBlockEntity.recheckStructureNow(false, true, false));
        assertTrue(LargeBoilerBlockEntity.recheckStructureNow(false, false, true));
        assertFalse(LargeBoilerBlockEntity.recheckStructureNow(false, false, false));
    }

    @Test
    void tierOutputsMatchLoaderRows() {
        assertEquals(8_192L, LargeBoilerTier.STAINLESS_STEEL.steamOutput());
        assertEquals(8_192L, LargeBoilerTier.INVAR.steamOutput());
        assertEquals(16_384L, LargeBoilerTier.TITANIUM.steamOutput());
        assertEquals(32_768L, LargeBoilerTier.TUNGSTENSTEEL.steamOutput());
        assertEquals(262_144L, LargeBoilerTier.ADAMANTIUM.steamOutput());
        assertEquals(4_096L, LargeBoilerTier.STAINLESS_STEEL.heatOutput());
        assertEquals(131_072L, LargeBoilerTier.ADAMANTIUM.heatOutput());
        assertEquals(81_920_000L, LargeBoilerTier.STAINLESS_STEEL.capacity());
        assertEquals(2_621_440_000L, LargeBoilerTier.ADAMANTIUM.capacity());
        assertEquals(128_000L, LargeBoilerTier.WATER_CAPACITY);
    }
}
