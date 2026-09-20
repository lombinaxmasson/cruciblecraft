package com.masson.cruciblecraft.client.color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import org.junit.jupiter.api.Test;

class MaterialItemColorTest {
    @Test
    void missingMaterialFallsBackToWhite() {
        assertEquals(0xFFFFFF, MaterialItemColor.baseColor(null, true));
        assertEquals(
                0x664F2F,
                MaterialItemColor.layerTint(
                        ToolKind.PICKAXE, 2, 0x888888, 0x664F2F));
        assertEquals(
                0x888888,
                MaterialItemColor.layerTint(
                        ToolKind.PICKAXE, 0, 0x888888, 0x664F2F));
        assertEquals(
                0xFFFFFF,
                MaterialItemColor.layerTint(
                        ToolKind.WRENCH, 2, 0x888888, 0x664F2F));
        assertEquals(
                0x888888,
                MaterialItemColor.layerTint(
                        ToolKind.UNIVERSAL_SPADE, 2, 0x888888, 0x664F2F));
        assertEquals(
                0xFF7F00,
                MaterialItemColor.layerTint(
                        ToolKind.MINING_DRILL_LV, 2, 0x888888, 0x664F2F));
        assertEquals(
                0x888888,
                MaterialItemColor.layerTint(
                        ToolKind.MINING_DRILL_LV, 0, 0x888888, 0x664F2F));
        assertTrue(MaterialItemColor.isWoodHandleKind(ToolKind.PICKAXE));
        assertFalse(MaterialItemColor.isWoodHandleKind(ToolKind.WRENCH));
    }
}
