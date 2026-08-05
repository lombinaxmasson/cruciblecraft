package com.masson.cruciblecraft.client.color;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MaterialItemColorTest {
    @Test
    void missingMaterialFallsBackToWhite() {
        assertEquals(0xFFFFFF, MaterialItemColor.baseColor(null, true));
    }
}
