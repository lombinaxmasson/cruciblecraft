package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import org.junit.jupiter.api.Test;

class MachineCasingTest {
    @Test
    void rejectsInvalidMaterialIdsAndTracksActualChanges() {
        MachineCasing casing = new MachineCasing(Device.CRUCIBLE, 800.0);
        assertEquals("ceramic", casing.materialId());
        assertThrows(
                IllegalArgumentException.class,
                () -> casing.setMaterialId("not_a_material"));
        assertEquals("ceramic", casing.materialId());
        assertTrue(casing.restoreMaterialId("gold"));
        assertTrue(casing.quarantined());
        assertEquals("ceramic", casing.materialId());
        assertEquals("gold", casing.persistedMaterialId());
        assertTrue(casing.setMaterialId("ceramic"));
        assertFalse(casing.quarantined());
        assertFalse(casing.setMaterialId("ceramic"));
        assertTrue(casing.setMaterialId("bronze"));
        assertEquals("bronze", casing.materialId());
    }

    @Test
    void rejectsInvalidPhysicalVolume() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineCasing(Device.CRUCIBLE, Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineCasing(Device.CRUCIBLE, -1.0));
    }
}
