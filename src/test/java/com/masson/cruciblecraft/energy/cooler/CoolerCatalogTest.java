package com.masson.cruciblecraft.energy.cooler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class CoolerCatalogTest {
    @Test
    void catalogLocksTenElectricAndFluxIdentities() {
        Map<String, CoolerProfile> profiles =
                CoolerCatalog.profiles().stream()
                        .collect(Collectors.toMap(
                                profile -> profile.id().toString(),
                                profile -> profile));
        assertEquals(10, profiles.size());
        CoolerProfile lv = profiles.get(
                "cruciblecraft:thermoelectric_cooler_lv");
        CoolerProfile iv = profiles.get(
                "cruciblecraft:thermoelectric_cooler_iv");
        CoolerProfile lead = profiles.get(
                "cruciblecraft:thermofluxic_cooler_lead");
        CoolerProfile enderium = profiles.get(
                "cruciblecraft:thermofluxic_cooler_enderium");
        assertNotNull(lv);
        assertNotNull(iv);
        assertNotNull(lead);
        assertNotNull(enderium);
        assertEquals(10161, lv.sourceId());
        assertEquals(32, lv.nbtInput());
        assertEquals(8, lv.nbtOutput());
        assertEquals("steel_galvanized", lv.material());
        assertTrue(lv.electric());
        assertTrue(lv.switchableMode());
        assertNull(lv.hostId());
        assertEquals(10165, iv.sourceId());
        assertEquals(8192, iv.nbtInput());
        assertEquals(11161, lead.sourceId());
        assertEquals(128, lead.nbtInput());
        assertTrue(lead.flux());
        assertEquals(lv.id(), lead.hostId());
        assertEquals(11165, enderium.sourceId());
        assertEquals(32768, enderium.nbtInput());
        assertEquals(
                Set.of(10161, 10162, 10163, 10164, 10165,
                        11161, 11162, 11163, 11164, 11165),
                profiles.values().stream()
                        .map(CoolerProfile::sourceId)
                        .collect(Collectors.toSet()));
        assertEquals(
                5,
                profiles.values().stream().filter(CoolerProfile::electric).count());
        assertEquals(
                5,
                profiles.values().stream().filter(CoolerProfile::flux).count());
    }

    @Test
    void convertUnitsMatchesGt6WasteAndModeCap() {
        assertEquals(8L, CoolerBlockEntity.convertUnits(32L, 32L, 8L, false));
        assertEquals(2L, CoolerBlockEntity.convertUnits(8L, 32L, 8L, false));
        assertEquals(3L, CoolerBlockEntity.convertUnits(16L, 16L, 3L, false));
        assertEquals(64L, CoolerBlockEntity.convertUnits(64L, 16L, 16L, true));
        assertEquals(8L, CoolerBlockEntity.convertUnits(128L, 128L, 8L, false));
    }

    @Test
    void catalogAndGameTestTemplateAreBundled() {
        assertNotNull(CoolerCatalogTest.class.getResource(
                "/data/cruciblecraft/coolers.json"));
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_cooler";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }
}
