package com.masson.cruciblecraft.energy.vondagraagg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VonDaGraaggLogicTest {
    @Test
    void bind8Clamps4096Over16To255() {
        assertEquals(0, VonDaGraaggLogic.range(0L, true));
        assertEquals(0, VonDaGraaggLogic.range(4096L, false));
        assertEquals(1, VonDaGraaggLogic.range(16L, true));
        assertEquals(255, VonDaGraaggLogic.range(4096L, true));
        assertEquals(255, VonDaGraaggLogic.range(10_000L, true));
        assertEquals(255, VonDaGraaggLogic.MAX_RANGE);
        assertEquals(255, VonDaGraaggLogic.range(8192L, true));
    }

    @Test
    void drainConsumesTheFullCapacitor() {
        assertEquals(0L, VonDaGraaggLogic.afterDrain(4096L));
        assertEquals(0L, VonDaGraaggLogic.afterDrain(1L));
        assertEquals(4096L, VonDaGraaggLogic.afterDrain(8192L));
    }
}
