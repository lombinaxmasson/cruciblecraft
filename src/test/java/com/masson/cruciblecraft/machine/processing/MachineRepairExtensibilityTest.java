package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

class MachineRepairExtensibilityTest {
    private static final String FIXTURE_VARIANT = "cruciblecraft:invar_lathe";
    private static final String FIXTURE_CASING = "cruciblecraft:iron_machine_casing";
    private static final Path OVERLAY_ROOT = Path.of(
            "src/test/resources/data/cruciblecraft/repair_overlay");

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void overlayProjectsL1LatheWithoutMutatingProductionCatalog() {
        int productionSize = MachineTierCatalog.entries().size();
        CatalogTestSupport.Loaded production = CatalogTestSupport.loadProduction();
        CatalogTestSupport.Loaded overlay = CatalogTestSupport.loadOverlay();

        assertEquals(productionSize, MachineTierCatalog.entries().size());
        assertFalse(production.containsVariant(FIXTURE_VARIANT));
        assertTrue(overlay.containsVariant(FIXTURE_VARIANT));
        assertEquals(production.tiers().entries().size() + 1,
                overlay.tiers().entries().size());

        MachineAcquisition.Resolved resolved = MachineAcquisition.resolve(
                ResourceLocation.parse(FIXTURE_VARIANT),
                ResourceLocation.parse("cruciblecraft:lathe"),
                "cruciblecraft:invar",
                EnergyType.KINETIC_ROTATION,
                overlay.kinds(),
                overlay.casings(),
                overlay.acquisition());
        assertEquals("kinetic", resolved.template());
        assertEquals(
                ResourceLocation.parse("cruciblecraft:invar/machine_casing_double"),
                resolved.casingItem());
        assertFalse(MachineTierCatalog.entries().stream().anyMatch(entry ->
                FIXTURE_VARIANT.equals(entry.variantId().toString())));
    }

    @Test
    void overlayProjectsL2IronElectrolyzerCableWithoutProductionRow() {
        CatalogTestSupport.Loaded overlay = CatalogTestSupport.loadOverlay();
        assertFalse(MachineCasingCatalog.bundled().hasElectrolyzerCable("iron"));
        IllegalStateException missing = assertThrows(
                IllegalStateException.class,
                () -> MachineCasingCatalog.bundled()
                        .electrolyzerCableMaterial("iron"));
        assertTrue(missing.getMessage().contains("No electrolyzer cable for"));
        assertTrue(overlay.casings().hasElectrolyzerCable("iron"));
        assertEquals(
                "copper",
                overlay.casings().electrolyzerCableMaterial("iron"));
    }

    @Test
    void overlayProjectsDeviceIronCrucibleFromCatalog() {
        CatalogTestSupport.Loaded overlay = CatalogTestSupport.loadOverlay();
        assertFalse(MachineMaterialRules.isAllowed(Device.CRUCIBLE, "iron"));
        assertTrue(overlay.devices().requireDevice(Device.CRUCIBLE).isAllowed("iron"));
        assertEquals(
                DeviceMaterialCatalog.require(Device.CRUCIBLE).creativeVisible().size() + 1,
                overlay.devices().requireDevice(Device.CRUCIBLE).creativeVisible().size());
        // Iron melting point is the MaterialCatalog value; overlay only adds
        // allowlisting. Temperature still uses the GT6 1.25 floor formula.
        assertEquals(
                (float) Math.floor(
                        (1538.0 + MachineMaterialRules.KELVIN_OFFSET)
                                * MachineMaterialRules.CRUCIBLE_TEMPERATURE_FACTOR
                                - MachineMaterialRules.KELVIN_OFFSET),
                MachineMaterialRules.maxTemperature(1538.0),
                0.001f);
    }

    @Test
    void fixtureIdsStayOutOfProductionDatapackAndJava() throws Exception {
        String tiers = Files.readString(Path.of(
                "src/main/resources/data/cruciblecraft/machine_tiers.json"));
        String casings = Files.readString(Path.of(
                "src/main/resources/data/cruciblecraft/machine_casings.json"));
        String devices = Files.readString(Path.of(
                "src/main/resources/data/cruciblecraft/device_materials.json"));
        assertFalse(tiers.contains("invar_lathe"));
        assertFalse(casings.contains("iron_machine_casing"));
        assertFalse(crucibleSection(devices).contains("\"material_id\": \"iron\""));
        assertEquals(127, JsonParser.parseString(tiers)
                .getAsJsonObject()
                .getAsJsonArray("variants")
                .size());
        assertFalse(Files.exists(Path.of(
                "src/main/resources/data/cruciblecraft/repair_overlay")));
        assertTrue(Files.isRegularFile(OVERLAY_ROOT.resolve("machine_tiers.json")));
        for (String relative : List.of(
                "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java",
                "src/main/java/com/masson/cruciblecraft/registry/ModItems.java",
                "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java",
                "src/main/java/com/masson/cruciblecraft/registry/ModMachineVariants.java")) {
            String source = Files.readString(Path.of(relative));
            assertFalse(source.contains("invar_lathe"), relative);
            assertFalse(source.contains("iron_machine_casing"), relative);
            assertFalse(source.contains("Set.of(\"" + FIXTURE_VARIANT), relative);
        }
        assertNotEquals(
                MachineTierCatalog.entries().size(),
                CatalogTestSupport.loadOverlay().tiers().entries().size());
    }

    private static String crucibleSection(String devices) {
        int start = devices.indexOf("\"crucible\"");
        int end = devices.indexOf("\"anvil\"");
        return devices.substring(start, end);
    }
}
