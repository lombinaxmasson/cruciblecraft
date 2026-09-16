package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.serialization.JsonOps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/** Codec round-trip for WorldgenStoneLayers pebble configuration. */
class StoneLayerRockFeatureConfigTest {
    private static final Path DECLARATION = Path.of(
            "src/main/resources/data/cruciblecraft/worldgen_catalog"
                    + "/stone_layer_rocks.json");
    private static final Path CONFIGURED = Path.of(
            "src/main/resources/data/cruciblecraft/worldgen"
                    + "/configured_feature/stone_layer_rocks.json");

    @Test
    void codecRoundTripsProbability() throws Exception {
        JsonObject declaration = JsonParser.parseString(
                Files.readString(DECLARATION))
                .getAsJsonObject()
                .getAsJsonObject("config");
        int probability = declaration.get("probability").getAsInt();
        StoneLayerRockConfiguration config = new StoneLayerRockConfiguration(
                probability);
        var encoded = StoneLayerRockConfiguration.CODEC
                .encodeStart(JsonOps.INSTANCE, config)
                .getOrThrow();
        StoneLayerRockConfiguration decoded = StoneLayerRockConfiguration.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(config, decoded);
        assertEquals(128, decoded.probability());
        JsonObject runtime = JsonParser.parseString(
                Files.readString(CONFIGURED))
                .getAsJsonObject()
                .getAsJsonObject("config");
        assertEquals(
                runtime.get("probability").getAsInt(),
                decoded.probability());
    }

    @Test
    void declarationConfigMustMatchConfiguredRuntimeResource() throws Exception {
        JsonObject declaration = JsonParser.parseString(
                Files.readString(DECLARATION))
                .getAsJsonObject();
        JsonObject runtime = JsonParser.parseString(
                Files.readString(CONFIGURED))
                .getAsJsonObject();
        assertEquals(
                declaration.get("feature_type").getAsString(),
                runtime.get("type").getAsString());
        assertEquals(
                declaration.getAsJsonObject("config").toString(),
                runtime.getAsJsonObject("config").toString());
        assertEquals(
                "cruciblecraft:gt_surface_rock",
                declaration.get("placer").getAsString());
        assertEquals(123, declaration.get("layer_count").getAsInt());
    }
}
