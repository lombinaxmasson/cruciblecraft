package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.serialization.JsonOps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/** Codec round-trip for the surface rock scatter configuration. */
class SurfaceRockFeatureConfigTest {
    private static final Path SURFACE_SCATTER_DECLARATION = Path.of(
            "src/main/resources/data/cruciblecraft/worldgen_catalog"
                    + "/surface_scatter.json");
    private static final Path CONFIGURED_RUNTIME = Path.of(
            "src/main/resources/data/cruciblecraft/worldgen"
                    + "/configured_feature/surface_rock_scatter.json");

    @Test
    void codecRoundTripsAmountAndProbability() throws Exception {
        JsonObject declaration = JsonParser.parseString(
                Files.readString(SURFACE_SCATTER_DECLARATION))
                .getAsJsonObject()
                .getAsJsonObject("config");
        int amount = declaration.get("amount").getAsInt();
        int probability = declaration.get("probability").getAsInt();
        SurfaceRockConfiguration config = new SurfaceRockConfiguration(
                amount,
                probability);
        var encoded = SurfaceRockConfiguration.CODEC
                .encodeStart(JsonOps.INSTANCE, config)
                .getOrThrow();
        SurfaceRockConfiguration decoded = SurfaceRockConfiguration.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(config, decoded);
        assertEquals(amount, decoded.amount());
        assertEquals(probability, decoded.probability());
        JsonObject json = (JsonObject) encoded;
        assertEquals(amount, json.get("amount").getAsInt());

        JsonObject runtime = JsonParser.parseString(
                Files.readString(CONFIGURED_RUNTIME))
                .getAsJsonObject()
                .getAsJsonObject("config");
        assertEquals(
                runtime.get("amount").getAsInt(),
                decoded.amount());
        assertEquals(
                runtime.get("probability").getAsInt(),
                decoded.probability());
    }

    @Test
    void declarationConfigMustMatchConfiguredRuntimeResource() throws Exception {
        JsonObject declaration = JsonParser.parseString(
                Files.readString(SURFACE_SCATTER_DECLARATION))
                .getAsJsonObject();
        JsonObject runtime = JsonParser.parseString(
                Files.readString(CONFIGURED_RUNTIME))
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
    }
}
