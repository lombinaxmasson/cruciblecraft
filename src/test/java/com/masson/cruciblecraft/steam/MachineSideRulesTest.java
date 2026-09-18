package com.masson.cruciblecraft.steam;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.steam.CapabilitySideRules.Face;

class MachineSideRulesTest {
    @Test void boilerSeparatesWaterInputFromSteamOutput() {
        assertTrue(CapabilitySideRules.boilerAcceptsWater(Face.DOWN));
        assertTrue(CapabilitySideRules.boilerAcceptsWater(Face.NORTH));
        assertFalse(CapabilitySideRules.boilerAcceptsWater(Face.UP));
        assertTrue(CapabilitySideRules.boilerExposesSteam(Face.UP));
        assertFalse(CapabilitySideRules.boilerExposesSteam(Face.DOWN));
    }

    @Test void engineAndCrusherReserveTheirFrontForOutput() {
        assertTrue(CapabilitySideRules.engineExposesKinetic(Face.EAST, Face.EAST));
        assertFalse(CapabilitySideRules.engineExposesKinetic(Face.EAST, Face.WEST));
        assertFalse(CapabilitySideRules.engineAcceptsSteam(Face.EAST, Face.EAST));
        assertTrue(CapabilitySideRules.engineAcceptsSteam(Face.EAST, Face.WEST));
        assertFalse(CapabilitySideRules.engineAcceptsSteam(Face.EAST, Face.NORTH));
        assertFalse(CapabilitySideRules.engineAcceptsSteam(Face.EAST, Face.UP));
        assertTrue(CapabilitySideRules.engineExposesExhaust(Face.EAST, Face.NORTH));
        assertTrue(CapabilitySideRules.engineExposesExhaust(Face.EAST, Face.UP));
        assertFalse(CapabilitySideRules.engineExposesExhaust(Face.EAST, Face.EAST));
        assertFalse(CapabilitySideRules.engineExposesExhaust(Face.EAST, Face.WEST));
        assertTrue(CapabilitySideRules.crusherExtractsItems(Face.NORTH, Face.NORTH));
        assertFalse(CapabilitySideRules.crusherExtractsItems(Face.NORTH, Face.SOUTH));
        assertTrue(CapabilitySideRules.crusherAcceptsKinetic(Face.NORTH, Face.SOUTH));
        assertFalse(CapabilitySideRules.crusherAcceptsKinetic(Face.NORTH, Face.NORTH));
    }
}
