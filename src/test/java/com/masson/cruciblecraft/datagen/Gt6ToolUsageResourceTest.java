package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class Gt6ToolUsageResourceTest {
    @Test
    void clayCeramicsUseKnifeAndRollingPin() throws Exception {
        assertClay(
                "raw_ceramic_crucible.json",
                List.of("CkC", "CRC", "CCC"),
                "cruciblecraft:raw_ceramic_crucible");
        assertClay(
                "raw_ceramic_mold.json",
                List.of("C C", "CCC", "k R"),
                "cruciblecraft:raw_ceramic_mold");
        assertClay(
                "raw_ceramic_bowl.json",
                List.of("k R", "C C", "CCC"),
                "cruciblecraft:raw_ceramic_bowl");
        JsonObject unshape = object(Path.of(
                "src/main/resources/data/cruciblecraft/recipe/"
                        + "raw_ceramic_bowl_unshape.json"));
        assertEquals(
                "minecraft:crafting_shapeless",
                unshape.get("type").getAsString());
        assertEquals(
                5,
                unshape.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(
                "minecraft:clay_ball",
                unshape.getAsJsonObject("result").get("id").getAsString());
        JsonObject firing = object(Path.of(
                "src/main/resources/data/cruciblecraft/recipe/"
                        + "mixing_bowl_firing.json"));
        assertEquals("minecraft:smelting", firing.get("type").getAsString());
        assertEquals(
                "cruciblecraft:raw_ceramic_bowl",
                firing.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals(
                "cruciblecraft:mixing_bowl",
                firing.getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    void brickBurningBoxUsesVanillaBrickAndFirestarter() throws Exception {
        JsonObject recipe = object(Path.of(
                "src/generated/resources/data/cruciblecraft/recipe/"
                        + "clay_brick_burning_box_brick.json"));
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(
                List.of("BBB", "BBB", "BFB"),
                recipe.getAsJsonArray("pattern").asList().stream()
                        .map(value -> value.getAsString())
                        .toList());
        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals(
                "minecraft:brick",
                key.getAsJsonObject("B").get("item").getAsString());
        assertEquals(
                "cruciblecraft:crafting_firestarter",
                key.getAsJsonObject("F").get("tag").getAsString());
        assertEquals(
                "cruciblecraft:clay_brick_burning_box_brick",
                recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    void brickBurningBoxNameIsNotDoubled() throws Exception {
        JsonObject english = object(Path.of(
                "src/generated/resources/assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = object(Path.of(
                "src/generated/resources/assets/cruciblecraft/lang/zh_cn.json"));
        assertEquals(
                "Brick Burning Box",
                english.get("block.cruciblecraft.clay_brick_burning_box_brick")
                        .getAsString());
        assertEquals(
                "Brick Burning Box",
                english.get("item.cruciblecraft.clay_brick_burning_box_brick")
                        .getAsString());
        assertEquals(
                "砖燃烧室",
                chinese.get("block.cruciblecraft.clay_brick_burning_box_brick")
                        .getAsString());
        assertEquals(
                "砖燃烧室",
                chinese.get("item.cruciblecraft.clay_brick_burning_box_brick")
                        .getAsString());
    }

    @Test
    void wrenchTagIncludesMonkeyWrench() throws Exception {
        JsonObject wrench = object(Path.of(
                "src/main/resources/data/cruciblecraft/tags/item/"
                        + "crafting_tools/wrench.json"));
        JsonObject knife = object(Path.of(
                "src/main/resources/data/cruciblecraft/tags/item/"
                        + "crafting_tools/knife.json"));
        assertTrue(wrench.getAsJsonArray("values").toString().contains(
                "cruciblecraft:material_wrench"));
        assertTrue(wrench.getAsJsonArray("values").toString().contains(
                "cruciblecraft:material_monkey_wrench"));
        assertTrue(knife.getAsJsonArray("values").toString().contains(
                "cruciblecraft:material_knife"));
        assertFalse(knife.getAsJsonArray("values").toString().contains(
                "cruciblecraft:flint_knife"));
    }

    private static void assertClay(
            String file,
            List<String> pattern,
            String resultId) throws Exception {
        JsonObject recipe = object(Path.of(
                "src/main/resources/data/cruciblecraft/recipe/" + file));
        assertEquals("cruciblecraft:shaped_catalyst", recipe.get("type").getAsString());
        assertEquals(
                pattern,
                recipe.getAsJsonArray("pattern").asList().stream()
                        .map(value -> value.getAsString())
                        .toList());
        assertEquals(
                "minecraft:clay_ball",
                recipe.getAsJsonObject("ingredients")
                        .getAsJsonObject("C")
                        .get("item")
                        .getAsString());
        JsonObject catalysts = recipe.getAsJsonObject("catalysts");
        assertEquals(
                "cruciblecraft:crafting_tools/knife",
                catalysts.getAsJsonObject("k").get("tag").getAsString());
        assertEquals(
                "cruciblecraft:crafting_tools/rolling_pin",
                catalysts.getAsJsonObject("R").get("tag").getAsString());
        assertEquals(
                resultId,
                recipe.getAsJsonObject("result").get("id").getAsString());
    }

    private static JsonObject object(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), path.toString());
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
