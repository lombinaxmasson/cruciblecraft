package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import org.junit.jupiter.api.Test;

class MachineCasingTest {
    @Test
    void sanitizesMaterialIdsAndTracksActualChanges() {
        MachineCasing casing = new MachineCasing(Device.CRUCIBLE, 800.0);
        assertEquals("ceramic", casing.materialId());
        assertFalse(casing.setMaterialId("not_a_material"));
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
