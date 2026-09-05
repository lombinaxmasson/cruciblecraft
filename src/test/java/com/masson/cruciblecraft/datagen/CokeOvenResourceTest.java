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
    void machineAndGeneratedJsonIsValid() throws IOException {
        List<Path> paths = List.of(
                Path.of("src/generated/resources/data/cruciblecraft/recipe/coke_oven/coal.json"),
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
