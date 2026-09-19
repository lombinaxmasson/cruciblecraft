package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CrucibleInteriorGeometryTest {
    @Test
    void displayedHeightMatchesGt6Scale() {
        assertEquals(0, CrucibleInteriorGeometry.displayedHeight(0, 2304));
        assertEquals(255, CrucibleInteriorGeometry.displayedHeight(2304, 2304));
        assertEquals(1, CrucibleInteriorGeometry.displayedHeight(1, 2304));
        assertEquals(
                1 + (144 * 254) / 2304,
                CrucibleInteriorGeometry.displayedHeight(144, 2304));
    }

    @Test
    void smallSurfaceUsesSmelteryPassFiveFormula() {
        assertEquals(
                0.125f + 255.0f / (2048.0f / 7.0f),
                CrucibleInteriorGeometry.smallSurfaceY(255),
                1.0e-6f);
        assertEquals(
                0.125f + 1.0f / (2048.0f / 7.0f),
                CrucibleInteriorGeometry.smallSurfaceY(1),
                1.0e-6f);
    }

    @Test
    void largeSurfaceUsesMultiblockPassFiveFormula() {
        assertEquals(
                1.125f + 255.0f / 150.0f,
                CrucibleInteriorGeometry.largeSurfaceY(255),
                1.0e-6f);
        assertEquals(
                1.125f + 1.0f / 150.0f,
                CrucibleInteriorGeometry.largeSurfaceY(1),
                1.0e-6f);
    }
}
