package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class T19CoverResourceTest {
    private static final Path GENERATED =
            Path.of("src/generated/resources");
    private static final Map<String, String> CHINESE_COVER_NAMES = Map.of(
            "conveyor_cover", "传送带盖板",
            "retriever_item_cover", "物品检索器盖板",
            "robot_arm_cover", "机械臂盖板",
            "pressure_valve_cover", "压力阀盖板",
            "selector_manual_cover", "手动选择器盖板");

    @Test
    void selectedFiveHaveRecipeModelAndBothLanguages() throws Exception {
        JsonObject english = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/zh_cn.json"));
        for (var entry : CHINESE_COVER_NAMES.entrySet()) {
            String item = entry.getKey();
            Path recipe = GENERATED.resolve(
                    "data/cruciblecraft/recipe/" + item + ".json");
            Path model = GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + item + ".json");
            assertTrue(Files.isRegularFile(recipe), recipe.toString());
            assertTrue(Files.isRegularFile(model), model.toString());
            assertEquals(
                    "cruciblecraft:" + item,
                    json(recipe).getAsJsonObject("result")
                            .get("id").getAsString());
            assertTrue(english.has("item.cruciblecraft." + item));
            assertEquals(
                    entry.getValue(),
                    chinese.get("item.cruciblecraft." + item).getAsString());
        }
        for (String key : Set.of(
                "jade.cruciblecraft.pipe_covers",
                "tooltip.cruciblecraft.cover.behavior",
                "tooltip.cruciblecraft.cover.parameters")) {
            assertTrue(english.has(key));
            assertTrue(chinese.has(key));
        }
        assertEquals(
                "离心机",
                chinese.get("block.cruciblecraft.centrifuge").getAsString());
        assertEquals(
                "筛选机",
                chinese.get("block.cruciblecraft.sifter").getAsString());
        assertEquals(
                "挤压机",
                chinese.get("block.cruciblecraft.extruder").getAsString());
        for (String value : CHINESE_COVER_NAMES.values()) {
            assertFalse(value.contains("Cover"));
        }
    }

    @Test
    void definitionCatalogAndSchemaExposeStrictBounds() throws Exception {
        Path catalog = Path.of(
                "src/main/resources/data/cruciblecraft/"
                        + "cover_definitions.json");
        Path schema = Path.of(
                "src/main/resources/data/cruciblecraft/schema/"
                        + "cover_definitions.schema.json");
        JsonObject catalogJson = json(catalog);
        assertEquals(9, catalogJson.getAsJsonArray("definitions").size());
        catalogJson.getAsJsonArray("definitions").forEach(row ->
                assertFalse(row.getAsJsonObject().has("translationKey")));
        JsonObject schemaJson = json(schema);
        JsonObject definitionSchema = schemaJson.getAsJsonObject("$defs")
                .getAsJsonObject("definition");
        assertFalse(definitionSchema.getAsJsonObject("properties")
                .has("translationKey"));
        assertFalse(definitionSchema.getAsJsonArray("required").asList()
                .stream().anyMatch(field ->
                        field.getAsString().equals("translationKey")));
        String schemaText = Files.readString(schema);
        assertTrue(schemaText.contains("\"additionalProperties\": false"));
        assertTrue(schemaText.contains("\"maximum\": 8000"));
        assertTrue(schemaText.contains("\"maximum\": 1000000"));
        assertTrue(schemaText.contains("\"maximum\": 64"));
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(
                Files.readString(path)).getAsJsonObject();
    }
}
