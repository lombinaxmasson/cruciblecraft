package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.mold.CeramicMoldCatalog;

import org.junit.jupiter.api.Test;

class CeramicMoldResourceTest {
    @Test
    void ceramicRecipesAndGeneratedModelsAreValidJson() throws IOException {
        List<Path> paths = new ArrayList<>(List.of(
                Path.of("src/main/resources/data/cruciblecraft/materials/ceramic.json"),
                Path.of("src/generated/resources/assets/cruciblecraft/blockstates/ceramic_mold.json"),
                Path.of("src/main/resources/assets/cruciblecraft/models/block/ceramic_mold.json"),
                Path.of("src/main/resources/assets/cruciblecraft/models/block/ceramic_mold_filled.json")));
        for (String recipe : List.of(
                "raw_ceramic_crucible",
                "raw_ceramic_mold",
                "firebrick",
                "ceramic_mold_firing")) {
            paths.add(Path.of("src/main/resources/data/cruciblecraft/recipe/" + recipe + ".json"));
        }
        JsonObject english = JsonParser.parseString(Files.readString(Path.of(
                "src/generated/resources/assets/cruciblecraft/lang/en_us.json")))
                .getAsJsonObject();
        JsonObject chinese = JsonParser.parseString(Files.readString(Path.of(
                "src/generated/resources/assets/cruciblecraft/lang/zh_cn.json")))
                .getAsJsonObject();
        for (CeramicMoldCatalog.Variant variant : CeramicMoldCatalog.SHAPED) {
            String raw = CeramicMoldCatalog.rawItemId(variant);
            String fired = CeramicMoldCatalog.firedItemId(variant);
            paths.add(Path.of("src/main/resources/data/cruciblecraft/recipe/" + raw + ".json"));
            paths.add(Path.of(
                    "src/main/resources/data/cruciblecraft/recipe/" + fired + "_firing.json"));
            Path rawModelPath = Path.of(
                    "src/generated/resources/assets/cruciblecraft/models/item/" + raw + ".json");
            Path firedModelPath = Path.of(
                    "src/generated/resources/assets/cruciblecraft/models/item/" + fired + ".json");
            paths.add(rawModelPath);
            paths.add(firedModelPath);
            assertTrue(
                    Files.isRegularFile(Path.of(
                            "src/main/resources/assets/cruciblecraft/textures/item/gt6_import/"
                                    + raw + ".png")),
                    () -> raw + " is missing its GT6 clay icon");
            JsonObject rawModel = JsonParser.parseString(Files.readString(rawModelPath))
                    .getAsJsonObject();
            assertEquals(
                    "cruciblecraft:item/gt6_import/" + raw,
                    rawModel.getAsJsonObject("textures").get("layer0").getAsString(),
                    raw);
            JsonObject firedModel = JsonParser.parseString(Files.readString(firedModelPath))
                    .getAsJsonObject();
            assertEquals(
                    "cruciblecraft:block/ceramic_mold",
                    firedModel.get("parent").getAsString(),
                    fired);
            String rawKey = "item.cruciblecraft." + raw;
            String firedKey = "item.cruciblecraft." + fired;
            assertEquals(variant.englishRaw(), english.get(rawKey).getAsString(), rawKey);
            assertEquals(variant.englishFired(), english.get(firedKey).getAsString(), firedKey);
            assertEquals(variant.chineseRaw(), chinese.get(rawKey).getAsString(), rawKey);
            assertEquals(variant.chineseFired(), chinese.get(firedKey).getAsString(), firedKey);
        }
        for (String item : List.of(
                "raw_ceramic_crucible",
                "raw_ceramic_mold",
                "ceramic_mold")) {
            paths.add(Path.of("src/generated/resources/assets/cruciblecraft/models/item/" + item + ".json"));
        }

        for (Path path : paths) {
            assertTrue(
                    JsonParser.parseString(Files.readString(path)).isJsonObject(),
                    () -> path + " must contain a JSON object");
        }
        for (String model : List.of("ceramic_mold.json", "ceramic_mold_filled.json")) {
            var json = JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/assets/cruciblecraft/models/block/" + model)))
                    .getAsJsonObject();
            assertTrue(
                    "cruciblecraft:ceramic_mold".equals(
                            json.get("loader").getAsString()),
                    () -> model + " must use the ceramic_mold geometry loader");
            assertTrue(
                    json.getAsJsonArray("elements").size() == 18,
                    () -> model + " must keep the GT6 frame and drop the 25 cells");
        }
    }

    @Test
    void foundryMoldParentUsesCeramicMoldGeometryWithoutBakedCells() throws IOException {
        var json = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/cruciblecraft/models/block/mte_foundry_mold.json")))
                .getAsJsonObject();
        assertTrue(
                "cruciblecraft:ceramic_mold".equals(json.get("loader").getAsString()),
                "foundry mold parent must use the ceramic_mold geometry loader");
        assertTrue(
                json.getAsJsonArray("elements").size() == 17,
                "foundry mold parent must keep the GT6 frame and drop the 25 cells");
        var blockstate = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/cruciblecraft/blockstates/foundry/mold_stone.json")))
                .getAsJsonObject()
                .getAsJsonObject("variants");
        for (var entry : blockstate.entrySet()) {
            var variant = entry.getValue().getAsJsonObject();
            assertTrue(
                    !variant.has("x") && !variant.has("y"),
                    () -> entry.getKey() + " rotates the mold off world XZ");
        }
    }
}
