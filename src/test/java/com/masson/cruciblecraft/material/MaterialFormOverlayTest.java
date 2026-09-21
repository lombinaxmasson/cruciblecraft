package com.masson.cruciblecraft.material;

import java.nio.file.Path;

import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixTestFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialFormOverlayTest {
    @AfterEach
    void resetCatalog() {
        MaterialCatalog.resetForTests();
    }

    @Test
    void coreGateHasNoPlantFormsWithoutOverlay(@TempDir Path configDirectory) {
        MaterialPrefixTestFixture.bootstrapBuiltins();
        MaterialCatalog.bootstrap(configDirectory);
        var indigo = MaterialCatalog.require("indigo");
        assertFalse(MaterialCatalog.isFormRegistered(
                indigo, MaterialPrefixCatalog.require("plant_gt_blossom")));
        var iron = MaterialCatalog.require("iron");
        assertFalse(MaterialCatalog.isFormRegistered(
                iron, MaterialPrefixCatalog.require("plant_gt_blossom")));
        assertTrue(MaterialFormHosts.isSharedInventoryForm(
                indigo, MaterialPrefixCatalog.require("dust"))
                || MaterialCatalog.isFormRegistered(
                        indigo, MaterialPrefixCatalog.require("dust")));
    }

    @Test
    void namedPlantOverlayOpensSharedLongTailPair(@TempDir Path configDirectory) {
        MaterialPrefixTestFixture.bootstrapBuiltins();
        assertTrue(MaterialCatalog.addStartupForm("indigo", "plant_gt_blossom"));
        assertFalse(MaterialCatalog.addStartupForm("indigo", "plant_gt_blossom"));
        MaterialCatalog.bootstrap(configDirectory);
        var indigo = MaterialCatalog.require("indigo");
        var blossom = MaterialPrefixCatalog.require("plant_gt_blossom");
        assertTrue(MaterialCatalog.isFormRegistered(indigo, blossom));
        assertTrue(MaterialFormHosts.isSharedInventoryForm(indigo, blossom));
        assertFalse(MaterialFormHosts.isUniqueInventoryForm(indigo, blossom));
        assertFalse(MaterialCatalog.isFormRegistered(
                MaterialCatalog.require("iron"), blossom));
    }

    @Test
    void overlayRejectsPublicExchangeAndGateDuplicates(@TempDir Path configDirectory) {
        MaterialPrefixTestFixture.bootstrapBuiltins();
        assertTrue(MaterialCatalog.addStartupForm("iron", "ingot"));
        assertThrows(IllegalStateException.class, () -> MaterialCatalog.bootstrap(configDirectory));
        MaterialCatalog.resetForTests();
        MaterialPrefixTestFixture.bootstrapBuiltins();
        assertTrue(MaterialCatalog.addStartupForm("iron", "dust"));
        assertThrows(IllegalStateException.class, () -> MaterialCatalog.bootstrap(configDirectory));
        MaterialCatalog.resetForTests();
        MaterialPrefixTestFixture.bootstrapBuiltins();
        assertTrue(MaterialCatalog.addStartupForm("water", "plant_gt_twig"));
        assertThrows(IllegalStateException.class, () -> MaterialCatalog.bootstrap(configDirectory));
    }

    @Test
    void allNamedPlantPairsOpenSharedBackends(@TempDir Path configDirectory) {
        MaterialPrefixTestFixture.bootstrapBuiltins();
        var pairs = java.util.List.of(
                java.util.Map.entry("copper", "plant_gt_fiber"),
                java.util.Map.entry("emerald", "plant_gt_berry"),
                java.util.Map.entry("glowstone", "plant_gt_wart"),
                java.util.Map.entry("gold", "plant_gt_blossom"),
                java.util.Map.entry("indigo", "plant_gt_blossom"),
                java.util.Map.entry("iron", "plant_gt_blossom"),
                java.util.Map.entry("lead", "plant_gt_blossom"),
                java.util.Map.entry("milk", "plant_gt_wart"),
                java.util.Map.entry("mint", "plant_gt_blossom"),
                java.util.Map.entry("silver", "plant_gt_blossom"),
                java.util.Map.entry("tea", "plant_gt_blossom"),
                java.util.Map.entry("tin", "plant_gt_twig"));
        for (var pair : pairs) {
            assertTrue(
                    MaterialCatalog.addStartupForm(pair.getKey(), pair.getValue()),
                    pair.getKey() + "/" + pair.getValue());
        }
        MaterialCatalog.bootstrap(configDirectory);
        for (var pair : pairs) {
            var material = MaterialCatalog.require(pair.getKey());
            var prefix = MaterialPrefixCatalog.require(pair.getValue());
            assertTrue(MaterialCatalog.isFormRegistered(material, prefix), pair.toString());
            assertTrue(MaterialFormHosts.isSharedInventoryForm(material, prefix), pair.toString());
            assertFalse(MaterialFormHosts.isUniqueInventoryForm(material, prefix), pair.toString());
        }
    }
}
