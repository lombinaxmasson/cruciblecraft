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
        assertEquals(131, declaration.get("layer_count").getAsInt());
        assertEquals(8, declaration.get("rock_ore_count").getAsInt());
        assertEquals(1, declaration.get("nether_rock_ore_count").getAsInt());
        assertEquals(
                "cruciblecraft:nether_netherquartz",
                declaration.getAsJsonObject("nether_quartz")
                        .get("feature_type")
                        .getAsString());
        boolean hasNetherQuartz = false;
        boolean netherInLayers = false;
        var dense = declaration.getAsJsonArray("rock_ores");
        for (int i = 0; i < dense.size(); i++) {
            if ("nether_quartz/dense_ore".equals(
                    dense.get(i).getAsJsonObject().get("registry_path").getAsString())) {
                hasNetherQuartz = true;
                break;
            }
        }
        var layers = declaration.getAsJsonArray("layers");
        for (int i = 0; i < layers.size(); i++) {
            if ("nether_quartz".equals(
                    layers.get(i).getAsJsonObject().get("material").getAsString())) {
                netherInLayers = true;
                break;
            }
        }
        assertEquals(true, hasNetherQuartz);
        assertEquals(false, netherInLayers);
        assertEquals(45, declaration.get("stone_block_count").getAsInt());
        assertEquals(17, declaration.get("village_brick_count").getAsInt());
        assertEquals(648648000, declaration.get("unit").getAsInt());
        assertEquals(
                true,
                declaration.getAsJsonObject("deepslate_layer")
                        .getAsJsonArray("ores")
                        .size()
                        > 0);
        boolean hasBlackGranite = false;
        var cubes = declaration.getAsJsonArray("stone_blocks");
        for (int i = 0; i < cubes.size(); i++) {
            if ("granite_black/stone".equals(
                    cubes.get(i).getAsJsonObject().get("registry_path").getAsString())) {
                hasBlackGranite = true;
                break;
            }
        }
        assertEquals(true, hasBlackGranite);
    }
}
