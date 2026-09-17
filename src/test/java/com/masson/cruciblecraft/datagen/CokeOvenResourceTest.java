package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CokeOvenResourceTest {
    @Test
    void remainderCokeOvenRecipesStayOnJavaSourceAmounts() throws IOException {
        Path authored = Path.of(
                "src/main/resources/data/cruciblecraft/recipe/coke_oven");
        int authoredRows = 0;
        try (var paths = Files.walk(authored)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                authoredRows++;
                var recipe = JsonParser.parseString(Files.readString(path))
                        .getAsJsonObject();
                assertTrue(recipe.get("type").getAsString()
                        .equals("cruciblecraft:gt_recipe"));
                assertTrue(recipe.get("map").getAsString()
                        .equals("cruciblecraft:coke_oven"));
                assertTrue(recipe.get("eut").getAsInt() == 0);
                assertTrue(recipe.getAsJsonArray("fluid_outputs")
                        .get(0).getAsJsonObject()
                        .get("id").getAsString()
                        .equals("cruciblecraft:creosote"));
                assertTrue(recipe.getAsJsonObject("provenance")
                        .get("source_kind").getAsString()
                        .equals("gt6_java_source"));
            }
        }
        assertTrue(authoredRows == 19, "expected 19 authored coke oven rows");
    }

    @Test
    void machineAndGeneratedJsonIsValid() throws IOException {
        List<Path> paths = List.of(
                Path.of("src/generated/resources/data/cruciblecraft/recipe/coke_oven/coal.json"),
                Path.of("src/main/resources/data/cruciblecraft/recipe/coke_oven/oak_log.json"),
                Path.of("src/main/resources/data/cruciblecraft/recipe/coke_oven/lignite_gem.json"),
                Path.of("src/main/resources/data/cruciblecraft/recipe/coke_oven_controller.json"),
                Path.of("src/generated/resources/assets/cruciblecraft/blockstates/coke_oven.json"),
                Path.of("src/generated/resources/assets/cruciblecraft/models/block/coke_oven.json"),
                Path.of("src/generated/resources/assets/cruciblecraft/models/item/coke_oven.json"),
                Path.of("src/generated/resources/assets/cruciblecraft/models/item/creosote_bucket.json"),
                Path.of("src/generated/resources/assets/cruciblecraft/lang/en_us.json"),
                Path.of("src/generated/resources/data/cruciblecraft/loot_table/blocks/coke_oven.json"));

        for (Path path : paths) {
            assertTrue(
                    JsonParser.parseString(Files.readString(path)).isJsonObject(),
                    () -> path + " must contain a JSON object");
        }
    }
}
