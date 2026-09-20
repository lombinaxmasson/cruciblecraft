package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * GT6 {@code Loader_Recipes_Woods} NERFED_WOOD hand/saw plank counts.
 * Vanilla log recipes overlay {@code minecraft:*_planks}; GT trees write
 * {@code cruciblecraft:wood/*}.
 */
class WoodPlankCraftingResourceTest {
    private static final Path VANILLA = Path.of(
            "src/main/resources/data/minecraft/recipe");
    private static final Path GT_WOOD = Path.of(
            "src/main/resources/data/cruciblecraft/recipe/wood");
    private static final List<String> VANILLA_WOODS = List.of(
            "oak",
            "spruce",
            "birch",
            "jungle",
            "acacia",
            "dark_oak",
            "mangrove",
            "cherry",
            "crimson",
            "warped");
    private static final Map<String, String> VANILLA_TAGS = Map.of(
            "oak", "minecraft:oak_logs",
            "spruce", "minecraft:spruce_logs",
            "birch", "minecraft:birch_logs",
            "jungle", "minecraft:jungle_logs",
            "acacia", "minecraft:acacia_logs",
            "dark_oak", "minecraft:dark_oak_logs",
            "mangrove", "minecraft:mangrove_logs",
            "cherry", "minecraft:cherry_logs",
            "crimson", "minecraft:crimson_stems",
            "warped", "minecraft:warped_stems");
    private static final Map<String, String> GT_TREES = Map.of(
            "rubber", "rubberwood_planks",
            "maple", "maple_planks",
            "willow", "willow_planks",
            "blue_mahoe", "blue_mahoe_planks",
            "hazel", "hazel_planks",
            "cinnamon", "cinnamon_planks",
            "coconut", "coconut_planks",
            "rainbowood", "rainbowood_planks",
            "blue_spruce", "blue_spruce_planks");

    @Test
    void vanillaLogsHandCraftTwoPlanks() throws Exception {
        for (String wood : VANILLA_WOODS) {
            JsonObject recipe = object(VANILLA.resolve(wood + "_planks.json"));
            assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
            assertEquals(
                    VANILLA_TAGS.get(wood),
                    recipe.getAsJsonArray("ingredients")
                            .get(0)
                            .getAsJsonObject()
                            .get("tag")
                            .getAsString());
            JsonObject result = recipe.getAsJsonObject("result");
            assertEquals("minecraft:" + wood + "_planks", result.get("id").getAsString());
            assertEquals(2, result.get("count").getAsInt(), wood);
        }
    }

    @Test
    void gtTreeLogsHandCraftTwoMatchingPlanks() throws Exception {
        for (var entry : GT_TREES.entrySet()) {
            JsonObject recipe = object(GT_WOOD.resolve(entry.getValue() + ".json"));
            assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
            assertEquals(
                    "cruciblecraft:tree/" + entry.getKey() + "_log",
                    recipe.getAsJsonArray("ingredients")
                            .get(0)
                            .getAsJsonObject()
                            .get("item")
                            .getAsString());
            JsonObject result = recipe.getAsJsonObject("result");
            assertEquals(
                    "cruciblecraft:gt_wood/" + entry.getValue(),
                    result.get("id").getAsString());
            assertEquals(2, result.get("count").getAsInt(), entry.getValue());
        }
    }

    @Test
    void sawRecipesGiveFourPlanks() throws Exception {
        for (String wood : VANILLA_WOODS) {
            assertSaw(
                    GT_WOOD.resolve(wood + "_planks_saw.json"),
                    VANILLA_TAGS.get(wood),
                    true,
                    "minecraft:" + wood + "_planks");
        }
        for (var entry : GT_TREES.entrySet()) {
            assertSaw(
                    GT_WOOD.resolve(entry.getValue() + "_saw.json"),
                    "cruciblecraft:tree/" + entry.getKey() + "_log",
                    false,
                    "cruciblecraft:gt_wood/" + entry.getValue());
        }
    }

    private static void assertSaw(
            Path path,
            String input,
            boolean tag,
            String resultId) throws Exception {
        JsonObject recipe = object(path);
        assertEquals("cruciblecraft:shaped_catalyst", recipe.get("type").getAsString());
        assertEquals(
                "cruciblecraft:crafting_tools/saw",
                recipe.getAsJsonObject("catalysts")
                        .getAsJsonObject("s")
                        .get("tag")
                        .getAsString());
        JsonObject log = recipe.getAsJsonObject("ingredients").getAsJsonObject("L");
        assertEquals(input, log.get(tag ? "tag" : "item").getAsString());
        assertEquals("s  ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("L  ", recipe.getAsJsonArray("pattern").get(1).getAsString());
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(resultId, result.get("id").getAsString());
        assertEquals(4, result.get("count").getAsInt());
    }

    private static JsonObject object(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), path.toString());
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
