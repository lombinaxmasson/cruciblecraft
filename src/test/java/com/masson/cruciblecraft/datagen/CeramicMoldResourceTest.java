package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParser;

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
                "raw_ingot_mold",
                "raw_plate_mold",
                "raw_rod_mold",
                "raw_bolt_mold",
                "ingot_mold_firing",
                "plate_mold_firing",
                "rod_mold_firing",
                "bolt_mold_firing")) {
            paths.add(Path.of("src/main/resources/data/cruciblecraft/recipe/" + recipe + ".json"));
        }
        for (String item : List.of(
                "raw_ceramic_crucible",
                "raw_ceramic_mold",
                "raw_ingot_mold",
                "raw_plate_mold",
                "raw_rod_mold",
                "raw_bolt_mold",
                "ingot_mold",
                "plate_mold",
                "rod_mold",
                "bolt_mold")) {
            paths.add(Path.of("src/generated/resources/assets/cruciblecraft/models/item/" + item + ".json"));
        }

        for (Path path : paths) {
            assertTrue(
                    JsonParser.parseString(Files.readString(path)).isJsonObject(),
                    () -> path + " must contain a JSON object");
        }
    }
}
