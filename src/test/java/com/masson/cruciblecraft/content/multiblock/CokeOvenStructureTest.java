package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.masson.cruciblecraft.content.multiblock.CokeOvenStructureLayout.Offset;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class CokeOvenStructureTest {
    private static final int[][] FACINGS = {
        {0, -1},
        {1, 0},
        {0, 1},
        {-1, 0}
    };

    @Test
    void eachHorizontalFacingRequiresTwentyFiveUniqueFirebricks() {
        for (int[] facing : FACINGS) {
            var offsets = CokeOvenStructureLayout.firebrickOffsets(facing[0], facing[1]);
            assertEquals(25, offsets.size());
            assertEquals(25, new HashSet<>(offsets).size());
            assertFalse(offsets.contains(Offset.ZERO));
            assertFalse(offsets.contains(
                    CokeOvenStructureLayout.center(facing[0], facing[1])));
        }
    }

    @Test
    void controllerOccupiesTheOutwardMiddleOfTheShell() {
        for (int[] facing : FACINGS) {
            Offset center = CokeOvenStructureLayout.center(facing[0], facing[1]);
            assertEquals(0, center.x() + facing[0]);
            assertEquals(0, center.z() + facing[1]);
        }
    }

    @Test
    void heatSourceIsBelowTheCenterOfTheBottomLayer() {
        for (int[] facing : FACINGS) {
            Offset center = CokeOvenStructureLayout.center(facing[0], facing[1]);
            assertEquals(
                    new Offset(center.x(), -2, center.z()),
                    CokeOvenStructureLayout.heatSource(facing[0], facing[1]));
        }
    }
}
