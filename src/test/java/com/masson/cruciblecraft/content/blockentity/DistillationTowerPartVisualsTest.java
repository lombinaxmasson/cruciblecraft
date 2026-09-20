package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;

import org.junit.jupiter.api.Test;

class DistillationTowerPartVisualsTest {
    @Test
    void backHoleIsFarFaceCenterOfTowerLayers() {
        assertTrue(DistillationTowerPartVisuals.isBackHolePart(
                new Offset(0, 0, 2)));
        assertTrue(DistillationTowerPartVisuals.isBackHolePart(
                new Offset(0, 7, 2)));
        assertFalse(DistillationTowerPartVisuals.isBackHolePart(
                new Offset(0, -1, 2)));
        assertFalse(DistillationTowerPartVisuals.isBackHolePart(
                new Offset(1, 3, 2)));
        assertFalse(DistillationTowerPartVisuals.isBackHolePart(
                new Offset(0, 3, 1)));
        assertFalse(DistillationTowerPartVisuals.isBackHolePart(
                new Offset(0, 8, 2)));
    }
}
