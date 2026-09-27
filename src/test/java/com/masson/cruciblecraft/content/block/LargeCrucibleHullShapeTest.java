package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LargeCrucibleHullShapeTest {
    @Test
    void upperSideWallOutlineIsTheOuterHalfBlock() {
        var boxes = LargeCrucibleHullShape.localBoxes(-1, 2, 0);
        assertEquals(1, boxes.size());
        assertBox(boxes.get(0), 0.0, 0.0, 0.0, 0.5, 1.0, 1.0);
    }

    @Test
    void upperCornerOutlineIsTheOuterQuarter() {
        var boxes = LargeCrucibleHullShape.localBoxes(1, 2, 1);
        assertEquals(2, boxes.size());
        assertTrue(boxes.stream().anyMatch(box -> near(box, 0.5, 0.0, 0.0, 1.0, 1.0, 1.0)));
        assertTrue(boxes.stream().anyMatch(box -> near(box, 0.0, 0.0, 0.5, 1.0, 1.0, 1.0)));
    }

    @Test
    void middleSideWallKeepsTheFloorLip() {
        var boxes = LargeCrucibleHullShape.localBoxes(0, 1, -1);
        assertTrue(boxes.stream().anyMatch(box -> near(box, 0.0, 0.0, 0.0, 1.0, 1.0, 0.5)));
        assertTrue(boxes.stream().anyMatch(box -> near(box, 0.0, 0.0, 0.0, 1.0, 0.125, 1.0)));
    }

    private static void assertBox(
            LargeCrucibleHullShape.Box box,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1) {
        assertTrue(near(box, x0, y0, z0, x1, y1, z1));
    }

    private static boolean near(
            LargeCrucibleHullShape.Box box,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1) {
        return close(box.x0(), x0)
                && close(box.y0(), y0)
                && close(box.z0(), z0)
                && close(box.x1(), x1)
                && close(box.y1(), y1)
                && close(box.z1(), z1);
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0e-4;
    }
}
