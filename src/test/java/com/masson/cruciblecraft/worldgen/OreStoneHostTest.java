package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.block.OreStoneHost;

class OreStoneHostTest {
    private static final Path LAYER_CUBES = Path.of(
            "src/main/resources/data/cruciblecraft/worldgen_catalog"
                    + "/stone_layer_rocks.json");
    private static final Path HOSTED_STATE = Path.of(
            "src/main/resources/assets/cruciblecraft/blockstates/gt_hosted_ore.json");
    private static final Path SMALL_STATE = Path.of(
            "src/main/resources/assets/cruciblecraft/blockstates/gt_small_ore.json");
    private static final Path BROKEN_STATE = Path.of(
            "src/main/resources/assets/cruciblecraft/blockstates/gt_broken_ore.json");
    private static final Path FLOWER_ITEM = Path.of(
            "src/main/resources/assets/cruciblecraft/models/item/gt_indicator_flower.json");

    @Test
    void everyLayerCubeTextureIsAnOreHost() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(LAYER_CUBES))
                .getAsJsonObject();
        for (String key : java.util.List.of("stone_blocks", "rock_ores")) {
            JsonArray cubes = root.getAsJsonArray(key);
            for (int i = 0; i < cubes.size(); i++) {
                JsonObject cube = cubes.get(i).getAsJsonObject();
                String material = cube.get("material").getAsString();
                String texture = cube.get("texture").getAsString();
                String role = cube.get("role").getAsString();
                OreStoneHost host = OreStoneHost.ofLayer(material);
                assertTrue(
                        host != OreStoneHost.STONE || "stone".equals(material),
                        material + " must map to a dedicated OreStoneHost");
                if ("stone".equals(role)) {
                    assertEquals(texture, host.stoneTexture(), material);
                } else if ("cobble".equals(role)) {
                    assertEquals(texture, host.cobbleTexture(), material);
                }
            }
        }
        assertEquals(OreStoneHost.STONE, OreStoneHost.ofLayer("chert"));
        assertEquals(OreStoneHost.DEEPSLATE, OreStoneHost.ofLayer("deepslate"));
    }

    @Test
    void hostedSmallAndBrokenBlockstatesCoverEveryHost() throws Exception {
        Set<String> expected = new HashSet<>();
        for (OreStoneHost host : OreStoneHost.values()) {
            expected.add("host=" + host.getSerializedName());
            Path stone = Path.of(
                    "src/main/resources/assets/cruciblecraft/models/block/ore_host/"
                            + host.getSerializedName()
                            + ".json");
            Path cobble = Path.of(
                    "src/main/resources/assets/cruciblecraft/models/block/ore_host/"
                            + host.getSerializedName()
                            + "_cobble.json");
            assertTrue(Files.exists(stone), stone.toString());
            assertTrue(Files.exists(cobble), cobble.toString());
        }
        for (Path state : java.util.List.of(HOSTED_STATE, SMALL_STATE, BROKEN_STATE)) {
            JsonObject variants = JsonParser.parseString(Files.readString(state))
                    .getAsJsonObject()
                    .getAsJsonObject("variants");
            assertEquals(expected, variants.keySet(), state.toString());
        }
    }

    @Test
    void indicatorFlowerItemModelOverridesEveryVariant() throws Exception {
        JsonObject item = JsonParser.parseString(Files.readString(FLOWER_ITEM))
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:block/gt_indicator_flower/"
                        + IndicatorFlower.values()[0].getSerializedName(),
                item.get("parent").getAsString());
        var overrides = item.getAsJsonArray("overrides");
        assertEquals(IndicatorFlower.values().length - 1, overrides.size());
        for (int i = 1; i < IndicatorFlower.values().length; i++) {
            JsonObject override = overrides.get(i - 1).getAsJsonObject();
            assertEquals(
                    i - 0.5,
                    override.getAsJsonObject("predicate")
                            .get("cruciblecraft:flower")
                            .getAsDouble(),
                    0.001);
            assertEquals(
                    "cruciblecraft:block/gt_indicator_flower/"
                            + IndicatorFlower.values()[i].getSerializedName(),
                    override.get("model").getAsString());
        }
    }
}
