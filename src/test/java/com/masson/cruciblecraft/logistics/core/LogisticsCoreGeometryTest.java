package com.masson.cruciblecraft.logistics.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LogisticsCoreGeometryTest {
    @Test
    void cubeCountsMatchGt6Slots() {
        int inner = 0;
        int vent = 0;
        int wall = 0;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                for (int k = -2; k <= 2; k++) {
                    switch (LogisticsCoreGeometry.cell(i, j, k)) {
                        case INNER -> inner++;
                        case VENT -> vent++;
                        case WALL -> wall++;
                    }
                }
            }
        }
        assertEquals(27, inner);
        assertEquals(54, vent);
        assertEquals(44, wall);
        assertEquals(125, inner + vent + wall);
        assertEquals(192L, LogisticsCoreGeometry.operateThreshold(1, 1));
        assertEquals(384L, LogisticsCoreGeometry.capacity(1, 1));
        assertEquals(24L, LogisticsCoreGeometry.idleDrain(1, 1, 1, 1));
        assertTrue(LogisticsCorePart.WALL.innerAllowed());
        assertTrue(!LogisticsCorePart.VENT.innerAllowed());
        assertTrue(!LogisticsCorePart.CONTROLLER.innerAllowed());
    }
}
