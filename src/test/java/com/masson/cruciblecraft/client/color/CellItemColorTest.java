package com.masson.cruciblecraft.client.color;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CellItemColorTest {
    @Test
    void emptyAndNonBaseLayersStayUntinted() {
        assertEquals(0xFFFFFFFF, CellItemColor.tint(1, false, 0xAABBCC));
        assertEquals(0xFFFFFFFF, CellItemColor.tint(0, true, 0xAABBCC));
        assertEquals(0xFFFFFFFF, CellItemColor.tint(0, false, null));
        assertEquals(0xFFAABBCC, CellItemColor.tint(0, false, 0xAABBCC));
    }
}
