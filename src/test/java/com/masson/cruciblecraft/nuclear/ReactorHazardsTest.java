package com.masson.cruciblecraft.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ReactorHazardsTest {
    @Test
    void areaUsesIntegerDivisionNotDivUp() {
        assertEquals(1, ReactorHazards.neutronCalc(256));
        assertEquals(1, ReactorHazards.neutronCalc(511));
        assertEquals(2, ReactorHazards.neutronCalc(512));
        assertEquals(0, ReactorHazards.areaStrength(255, 0));
        assertEquals(1, ReactorHazards.areaStrength(512, 1));
    }

    @Test
    void failBurstDoublesCalc() {
        assertEquals(2, ReactorHazards.areaStrength(512, 0));
        assertEquals(4, ReactorHazards.failStrength(512, 0));
        assertEquals(500, ReactorHazards.FAIL_RANGE);
        assertEquals(200, ReactorHazards.AREA_RANGE);
    }

    @Test
    void radioactivityLevelIsDivUpOfStrength() {
        assertEquals(0, ReactorHazards.radioactivityLevel(0));
        assertEquals(1, ReactorHazards.radioactivityLevel(1));
        assertEquals(2, ReactorHazards.radioactivityLevel(11));
    }

    @Test
    void bindIntClamps() {
        assertEquals(Integer.MAX_VALUE, ReactorHazards.bindInt(1L + Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, ReactorHazards.bindInt(-1L + Integer.MIN_VALUE));
        assertEquals(7, ReactorHazards.bindInt(7L));
    }

    @Test
    void heatDamageIsFive() {
        assertEquals(5.0F, ReactorHazards.HEAT_DAMAGE);
        assertFalse(ReactorHazards.applyHeatDamage(null, 0.0F));
    }
}
