package com.masson.cruciblecraft.content.redstonewire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverVisuals;

import net.minecraft.resources.ResourceLocation;

class RedstoneWireCoversTest {
    @Test
    void torchIsNotAndRepeaterIsBuffer() {
        assertEquals(0, RedstoneWireCovers.visual(true, false));
        assertEquals(15, RedstoneWireCovers.output(0));
        assertEquals(1, RedstoneWireCovers.visual(true, true));
        assertEquals(0, RedstoneWireCovers.output(1));
        assertEquals(1, RedstoneWireCovers.visual(false, false));
        assertEquals(0, RedstoneWireCovers.visual(false, true));
        assertEquals(15, RedstoneWireCovers.output(
                RedstoneWireCovers.visual(false, true)));
    }

    @Test
    void selectorModeFloorsVisualLikeGt6() {
        assertEquals(
                2,
                RedstoneWireNetwork.visual(
                        2L * RedstoneWireNetwork.MAX_RANGE - 1L));
        assertEquals(
                15,
                RedstoneWireNetwork.visual(
                        15L * RedstoneWireNetwork.MAX_RANGE - 1L));
    }

    @Test
    void wireAcceptsGt6AttachableCoversOnly() {
        assertTrue(attaches("cover_blank"));
        assertTrue(attaches("redstone_torch"));
        assertTrue(attaches("selector_redstone"));
        assertTrue(attaches("selector_tag"));
        assertTrue(attaches("selector_button_panel"));
        assertTrue(attaches("redstone_emitter"));
        assertTrue(attaches("redstone_conductor_in"));
        assertTrue(attaches("redstone_conductor_out"));
        assertTrue(attaches("scale_progress"));
        assertTrue(attaches("cover_plate"));
        assertFalse(attaches("controller_auto"));
        assertFalse(attaches("scale_energy"));
        assertFalse(attaches("vent"));
        assertFalse(attaches("detector_running_possible"));
        assertFalse(attaches("display_energy"));
    }

    @Test
    void scaleProgressUsesGt6FillCurve() {
        assertEquals(0, MachineCoverBehaviors.scaledRedstone(0, 0L, 16000L));
        assertEquals(
                15, MachineCoverBehaviors.scaledRedstone(0, 16000L, 16000L));
        assertEquals(
                14,
                MachineCoverBehaviors.scaledRedstone(0, 15000L, 16000L));
        assertEquals(
                1,
                MachineCoverBehaviors.scaledRedstone(2, 15000L, 16000L));
    }

    @Test
    void emitterHotspotsMatchGt6CornersAndBits() {
        assertEquals(14, MachineCoverVisuals.emitterClick(15, 2.5 / 16, 2.5 / 16));
        assertEquals(1, MachineCoverVisuals.emitterClick(0, 13.5 / 16, 2.5 / 16));
        assertEquals(8, MachineCoverVisuals.emitterClick(0, 3.5 / 16, 10.5 / 16));
        assertEquals(4, MachineCoverVisuals.emitterClick(0, 6.5 / 16, 10.5 / 16));
        assertEquals(-1, MachineCoverVisuals.emitterClick(5, 0.5, 0.5));
    }

    private static boolean attaches(String path) {
        return MachineCoverKinds.attachesToRedstoneWire(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }
}
