package com.masson.cruciblecraft.logistics.machinecover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MachineCoverVisualsTest {
    @Test
    void displayBitsFollowGt6VisualLayout() {
        int visual = MachineCoverVisuals.encodeDisplay(
                true, false, false, true, 0);
        assertEquals("bottom", MachineCoverVisuals.displaySkin(visual));
        assertTrue(MachineCoverVisuals.displayLightOn(visual, 0));
        assertFalse(MachineCoverVisuals.displayLightOn(visual, 1));
        assertTrue(MachineCoverVisuals.displayLightOn(visual, 3));
        assertTrue(MachineCoverVisuals.displayLightPresent(visual, 0));
        assertTrue(MachineCoverVisuals.displayLightPresent(visual, 3));
        assertEquals(1 | 8 | 32 | 64 | 128 | 256, visual);
        int chiseled = MachineCoverVisuals.cycleDisplayStyle(visual);
        assertEquals("top", MachineCoverVisuals.displaySkin(chiseled));
        assertTrue(MachineCoverVisuals.displayLightOn(chiseled, 0));
        assertTrue(MachineCoverVisuals.displaySwitchHotspot(0, 0.7, 0.8));
        assertFalse(MachineCoverVisuals.displaySwitchHotspot(0, 0.7, 0.2));
        assertTrue(MachineCoverVisuals.displaySwitchHotspot(1, 0.7, 0.2));
    }

    @Test
    void buttonUnderlaysAndVentFacesStayExact() {
        assertEquals("underlay", MachineCoverVisuals.buttonUnderlay(0));
        assertEquals(
                "underlay_0_to_f",
                MachineCoverVisuals.buttonUnderlay(2 << 4));
        assertEquals(
                "underlay_bits",
                MachineCoverVisuals.buttonUnderlay(7 << 4));
        int next = MachineCoverVisuals.cycleButtonStyle(3);
        assertEquals(3, MachineCoverVisuals.buttonMode(next));
        assertEquals(1, MachineCoverVisuals.buttonStyle(next));
        assertEquals("front", MachineCoverVisuals.ventFront());
        assertEquals("back", MachineCoverVisuals.ventBack());
        assertEquals("sides", MachineCoverVisuals.ventSides());
        assertEquals(14, MachineCoverVisuals.emitterClick(15, 2.5 / 16, 2.5 / 16));
        assertEquals(8, MachineCoverVisuals.emitterClick(0, 3.5 / 16, 10.5 / 16));
        assertEquals(-1, MachineCoverVisuals.emitterClick(3, 0.5, 0.5));
    }
}
