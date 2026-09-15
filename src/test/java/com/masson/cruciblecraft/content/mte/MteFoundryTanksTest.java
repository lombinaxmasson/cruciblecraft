package com.masson.cruciblecraft.content.mte;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MteFoundryTanksTest {
    @Test
    void smelteryMatchesSixteenIngotMillibuckets() {
        assertEquals(2304, MteFoundryTanks.SMELTERY_MB);
        assertEquals(
                2304,
                MteFoundryTanks.capacityMb(
                        "MultiTileEntitySmeltery / Smelting Crucibles"));
    }

    @Test
    void moldBasinAndCrossingStayDistinctFromSmeltery() {
        assertEquals(
                144,
                MteFoundryTanks.capacityMb("MultiTileEntityMold / Molds"));
        assertEquals(
                1296,
                MteFoundryTanks.capacityMb("MultiTileEntityBasin / Molds"));
        assertEquals(
                0,
                MteFoundryTanks.capacityMb("MultiTileEntityCrossing / Molds"));
        assertEquals(0, MteFoundryTanks.capacityMb("MultiTileEntityChest / Chests"));
    }
}
