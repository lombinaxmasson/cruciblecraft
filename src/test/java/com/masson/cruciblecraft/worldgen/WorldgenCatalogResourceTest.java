package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class WorldgenCatalogResourceTest {
    private static final Path GENERATED =
            Path.of("src/worldgen_catalog_generated/resources");
    private static final Path READINESS =
            Path.of("tools/worldgen_catalog_readiness.json");

    @Test
    void readinessAndGeneratedResourcesCloseAllT2cVeinRows() throws Exception {
        var readiness = JsonParser.parseString(Files.readString(READINESS))
                .getAsJsonObject();
        var counts = readiness.getAsJsonObject("counts");
        assertEquals(129, counts.get("closure_vein_classifications").getAsInt());
        assertEquals(129, counts.get("t20_profile_v2_veins").getAsInt());
        assertEquals(129, counts.get("closure_configured_ore_features").getAsInt());
        assertEquals(129, counts.get("closure_placed_ore_features").getAsInt());
        assertEquals(137, counts.get("registered_ore_materials").getAsInt());
        assertEquals(274, counts.get("registered_ore_blocks").getAsInt());
        assertEquals(0, counts.get("unclassified").getAsInt());

        Set<String> ids = new HashSet<>();
        for (var element : readiness.getAsJsonArray("closure_vein_feature_ids")) {
            String id = element.getAsString();
            assertTrue(ids.add(id), id);
            Path configured = GENERATED.resolve(
                    "data/cruciblecraft/worldgen/configured_feature/"
                            + id + ".json");
            Path placed = GENERATED.resolve(
                    "data/cruciblecraft/worldgen/placed_feature/"
                            + id + ".json");
            assertEquals(
                    "cruciblecraft:large_vein",
                    JsonParser.parseString(Files.readString(configured))
                            .getAsJsonObject()
                            .get("type")
                            .getAsString(),
                    id);
            var config = JsonParser.parseString(Files.readString(configured))
                    .getAsJsonObject()
                    .getAsJsonObject("config");
            assertEquals(2, config.get("profile_version").getAsInt(), id);
            assertEquals(
                    "cruciblecraft:" + id,
                    config.get("profile_id").getAsString(),
                    id);
            assertEquals(
                    "cruciblecraft:" + id,
                    JsonParser.parseString(Files.readString(placed))
                            .getAsJsonObject()
                            .get("feature")
                            .getAsString(),
                    id);
        }
        assertEquals(129, ids.size());
        var geometry = readiness.getAsJsonObject("geometry_policy");
        assertEquals("T20_CLASSIFIED", geometry.get("status").getAsString());
        assertEquals(0, geometry.get("placeholder").getAsInt());
        assertEquals(0, geometry.get("unverified").getAsInt());
    }

    @Test
    void oilAndGasDepositsCompileToFiniteConfiguredFeatures()
            throws Exception {
        var readiness = JsonParser.parseString(Files.readString(READINESS))
                .getAsJsonObject();
        var deposits = readiness.getAsJsonArray("fluid_deposits");
        assertEquals(2, deposits.size());
        Set<String> states = new HashSet<>();
        for (var element : deposits) {
            var deposit = element.getAsJsonObject();
            String id = deposit.get("id").getAsString();
            states.add(deposit.get("material_state").getAsString());
            assertTrue(
                    deposit.get("min_amount_mb").getAsLong() > 0L,
                    id);
            assertTrue(
                    deposit.get("max_amount_mb").getAsLong()
                            >= deposit.get("min_amount_mb").getAsLong(),
                    id);
            Path configured = GENERATED.resolve(
                    "data/cruciblecraft/worldgen/configured_feature/"
                            + id + ".json");
            assertEquals(
                    "cruciblecraft:subsurface_fluid_deposit",
                    JsonParser.parseString(Files.readString(configured))
                            .getAsJsonObject()
                            .get("type")
                            .getAsString(),
                    id);
        }
        assertEquals(Set.of("liquid", "gas"), states);
    }
}
