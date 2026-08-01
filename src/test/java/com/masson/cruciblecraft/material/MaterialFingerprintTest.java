package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixDefinition;

import org.junit.jupiter.api.Test;

class MaterialFingerprintTest {
    @Test
    void prefixChangesOnlyChangePrefixHandshakeEntries() {
        MaterialDefinition copper = definition("copper", Map.of());
        List<MaterialPrefixDefinition> changed =
                new ArrayList<>(MaterialPrefixCatalog.definitions());
        MaterialPrefixDefinition ingot =
                MaterialPrefixCatalog.definition(MaterialPrefixes.INGOT);
        int index = changed.indexOf(ingot);
        changed.set(index, new MaterialPrefixDefinition(
                ingot.prefix(),
                ingot.serializedPath(),
                ingot.units() + 1,
                ingot.generationFlag(),
                ingot.tagDirectory(),
                ingot.tagNamespace(),
                ingot.modelTemplate(),
                ingot.modelTexture(),
                ingot.aliases(),
                ingot.capabilities()));

        Map<String, String> base =
                MaterialFingerprint.structureEntries(List.of(copper));
        Map<String, String> modified =
                MaterialFingerprint.structureEntries(List.of(copper), changed);

        assertEquals(base.get("copper"), modified.get("copper"));
        assertNotEquals(
                base.get("@prefix/cruciblecraft:ingot"),
                modified.get("@prefix/cruciblecraft:ingot"));
    }

    @Test
    void impliedPrefixesAreCanonicalStructuralFingerprintInput() {
        List<MaterialPrefixDefinition> changed =
                new ArrayList<>(MaterialPrefixCatalog.definitions());
        MaterialPrefixDefinition ore =
                MaterialPrefixCatalog.definition(MaterialPrefixes.ORE);
        int index = changed.indexOf(ore);
        changed.set(index, new MaterialPrefixDefinition(
                ore.prefix(),
                ore.serializedPath(),
                ore.units(),
                ore.generationFlag(),
                ore.tagDirectory(),
                ore.tagNamespace(),
                ore.modelTemplate(),
                ore.modelTexture(),
                ore.aliases(),
                List.of(),
                ore.capabilities()));

        assertNotEquals(
                MaterialFingerprint.structure(List.of()),
                MaterialFingerprint.structure(List.of(), changed));
    }

    @Test
    void isOrderIndependentAndIncludesFormOverrides() {
        MaterialDefinition copper = definition(
                "copper",
                Map.of(MaterialPrefixes.INGOT, "minecraft:copper_ingot"));
        MaterialDefinition tin = definition("tin", Map.of());

        assertEquals(
                MaterialFingerprint.compute(List.of(copper, tin)),
                MaterialFingerprint.compute(List.of(tin, copper)));
        assertNotEquals(
                MaterialFingerprint.compute(List.of(copper, tin)),
                MaterialFingerprint.compute(List.of(definition("copper", Map.of()), tin)));
    }

    @Test
    void tuningChangesDoNotAlterSaveStructureFingerprint() {
        MaterialDefinition base = definition("copper", Map.of());
        MaterialDefinition tuned = new MaterialDefinition(
                base.id(),
                base.tagName(),
                base.nameKey(),
                4,
                "#AABBCC",
                base.tintStyle(),
                base.forms(),
                base.formItems(),
                new ThermalProperties(900, 2_500, 9.0),
                base.moltenFluid(),
                base.composition(),
                base.noDecompose());

        assertEquals(
                MaterialFingerprint.structure(List.of(base)),
                MaterialFingerprint.structure(List.of(tuned)));
        assertNotEquals(
                MaterialFingerprint.tuning(List.of(base)),
                MaterialFingerprint.tuning(List.of(tuned)));
    }

    private static MaterialDefinition definition(
            String id,
            Map<MaterialPrefix, String> overrides) {
        return new MaterialDefinition(
                id,
                id,
                Optional.empty(),
                1,
                "#FFFFFF",
                "metallic",
                List.of(MaterialPrefixes.INGOT),
                overrides,
                new ThermalProperties(1_000, 2_000, 8),
                false,
                Map.of(),
                false);
    }
}
