package com.masson.cruciblecraft.energy.flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class FluxCatalogTest {
    @Test
    void catalogLocksThirtyFluxIdentities() {
        Map<String, FluxProfile> profiles =
                FluxCatalog.profiles().stream()
                        .collect(Collectors.toMap(
                                profile -> profile.id().toString(),
                                profile -> profile));
        assertEquals(30, profiles.size());
        FluxProfile leadHeater = profiles.get(
                "cruciblecraft:flux_heater_lead");
        FluxProfile enderiumDynamo = profiles.get(
                "cruciblecraft:flux_dynamo_enderium");
        FluxProfile leadMagnet = profiles.get(
                "cruciblecraft:flux_magnet_lead");
        FluxProfile leadLaser = profiles.get(
                "cruciblecraft:flux_laser_lead");
        assertNotNull(leadHeater);
        assertNotNull(enderiumDynamo);
        assertNotNull(leadMagnet);
        assertNotNull(leadLaser);
        assertEquals(11001, leadHeater.sourceId());
        assertEquals(128, leadHeater.nbtInput());
        assertEquals(16, leadHeater.nbtOutput());
        assertEquals("lead", leadHeater.material());
        assertTrue(leadHeater.heater());
        assertTrue(leadHeater.fluxInput());
        assertTrue(leadHeater.recipeLive());
        assertEquals(11115, enderiumDynamo.sourceId());
        assertEquals(8192, enderiumDynamo.nbtInput());
        assertEquals(22528, enderiumDynamo.nbtOutput());
        assertTrue(enderiumDynamo.dynamo());
        assertTrue(enderiumDynamo.fluxOutput());
        assertFalse(leadMagnet.recipeLive());
        assertFalse(leadLaser.recipeLive());
        assertEquals(
                Set.of(
                        11001, 11002, 11003, 11004, 11005,
                        11011, 11012, 11013, 11014, 11015,
                        11021, 11022, 11023, 11024, 11025,
                        11031, 11032, 11033, 11034, 11035,
                        11101, 11102, 11103, 11104, 11105,
                        11111, 11112, 11113, 11114, 11115),
                profiles.values().stream()
                        .map(FluxProfile::sourceId)
                        .collect(Collectors.toSet()));
        assertEquals(
                20,
                profiles.values().stream()
                        .filter(FluxProfile::recipeLive)
                        .count());
    }

    @Test
    void convertUnitsMatchesGt6WasteAndModeCap() {
        assertEquals(16L, FluxMath.convertUnits(128L, 128L, 16L, false));
        assertEquals(1L, FluxMath.convertUnits(8L, 128L, 16L, false));
        assertEquals(88L, FluxMath.convertUnits(32L, 32L, 88L, false));
        assertEquals(256L, FluxMath.convertUnits(256L, 16L, 16L, true));
        assertEquals(3L, FluxMath.convertUnits(16L, 16L, 3L, false));
    }

    @Test
    void catalogAndGameTestTemplateAreBundled() {
        assertNotNull(FluxCatalogTest.class.getResource(
                "/data/cruciblecraft/flux_converters.json"));
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_flux_converters";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }
}
