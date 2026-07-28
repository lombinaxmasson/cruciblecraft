package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;

import org.junit.jupiter.api.Test;

class MaterialFingerprintTest {
    @Test
    void isOrderIndependentAndIncludesFormOverrides() {
        MaterialDefinition copper = definition(
                "copper",
                Map.of(MaterialForm.INGOT, "minecraft:copper_ingot"));
        MaterialDefinition tin = definition("tin", Map.of());

        assertEquals(
                MaterialFingerprint.compute(List.of(copper, tin)),
                MaterialFingerprint.compute(List.of(tin, copper)));
        assertNotEquals(
                MaterialFingerprint.compute(List.of(copper, tin)),
                MaterialFingerprint.compute(List.of(definition("copper", Map.of()), tin)));
    }

    private static MaterialDefinition definition(
            String id,
            Map<MaterialForm, String> overrides) {
        return new MaterialDefinition(
                id,
                id,
                Optional.empty(),
                1,
                "#FFFFFF",
                "metallic",
                List.of(MaterialForm.INGOT),
                overrides,
                new ThermalProperties(1_000, 2_000, 8),
                false,
                Map.of(),
                false);
    }
}
