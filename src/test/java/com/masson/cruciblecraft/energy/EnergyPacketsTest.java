package com.masson.cruciblecraft.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnergyPacketsTest {
    @Test
    void sizeTimesAmountDefinesTransferredMagnitude() {
        assertEquals(96L, EnergyPackets.units(24L, 4L));
        assertEquals(96L, EnergyPackets.units(-24L, 4L));
        assertEquals(4L, EnergyPackets.packetsForUnits(-24L, 100L));
    }

    @Test
    void invalidAndOverflowingValuesAreSafe() {
        assertEquals(0L, EnergyPackets.units(0L, 10L));
        assertEquals(0L, EnergyPackets.units(10L, -1L));
        assertEquals(Long.MAX_VALUE, EnergyPackets.units(Long.MAX_VALUE, 2L));
        assertEquals(Long.MAX_VALUE, EnergyPackets.units(Long.MIN_VALUE, 2L));
        assertEquals(Long.MAX_VALUE, EnergyPackets.add(Long.MAX_VALUE, 1L));
    }
}
