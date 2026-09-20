package com.masson.cruciblecraft.content.item.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

class ElectricToolCatalogTest {
    @Test
    void loaderToolsFamilyHasTwentyIdentities() {
        assertEquals(19, ElectricToolCatalog.values().length);
        assertEquals(
                ToolKind.MINING_DRILL_LV,
                ElectricToolCatalog.MINING_DRILL_LV.kind());
        assertEquals(32L, ElectricToolCatalog.Voltage.LV.packet());
        assertEquals(128L, ElectricToolCatalog.Voltage.MV.packet());
        assertEquals(512L, ElectricToolCatalog.Voltage.HV.packet());
        assertEquals(
                "steel_galvanized",
                ElectricToolCatalog.Voltage.LV.hullMaterial());
        assertTrue(ElectricToolCatalog.WRENCH_LV.switchPartner()
                .map(partner -> partner == ElectricToolCatalog.MONKEY_WRENCH_LV)
                .orElse(false));
        assertTrue(ElectricToolCatalog.JACKHAMMER_HV.checkSwitchTarget());
        assertEquals(
                "material_mining_drill_lv",
                ElectricToolCatalog.MINING_DRILL_LV.itemPath());
    }
}
