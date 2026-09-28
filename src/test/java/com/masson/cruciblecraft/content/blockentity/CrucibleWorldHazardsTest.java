package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CrucibleWorldHazardsTest {
    @Test
    void nearMeltdownMatchesGt6Slack() {
        assertFalse(CrucibleWorldHazards.nearMeltdown(1_000.0F, 1_200.0F));
        assertFalse(CrucibleWorldHazards.nearMeltdown(1_100.0F, 1_200.0F));
        assertTrue(CrucibleWorldHazards.nearMeltdown(1_100.1F, 1_200.0F));
        assertTrue(CrucibleWorldHazards.nearMeltdown(1_200.0F, 1_200.0F));
    }

    @Test
    void meltDownTintBoostsRedAndGreenAndHalvesBlue() {
        assertEquals(0xFFB2B252, CrucibleWorldHazards.meltDownTint(0xFF404040));
        assertEquals(0xFFFFB232, CrucibleWorldHazards.meltDownTint(0xFF804000));
    }
}
