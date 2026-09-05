package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class ChemicalMachineResourceTest {
    private static final Path MAIN = Path.of("src/main/resources");
    private static final Path GENERATED = Path.of("src/generated/resources");

    @Test
    void dedicatedChemicalMachinesHaveGeneratedSurvivalResources() {
        for (String id : List.of(
                "electrolyzer",
                "mixer",
                "distillery",
                "autoclave",
                "drying",
                "compressor")) {
            assertResource(GENERATED, "assets/cruciblecraft/blockstates/" + id + ".json");
            assertResource(GENERATED, "assets/cruciblecraft/models/block/" + id + ".json");
            assertResource(GENERATED, "assets/cruciblecraft/models/item/" + id + ".json");
            assertResource(GENERATED, "data/cruciblecraft/loot_table/blocks/" + id + ".json");
            assertResource(GENERATED, "data/cruciblecraft/recipe/machines/" + id + ".json");
        }
        assertFalse(Files.exists(GENERATED.resolve(
                "data/cruciblecraft/recipe/machines/chemical_reactor.json")));
    }

    @Test
    void bronzeDynamoHasFixedRuntimeResources() throws Exception {
        for (String path : java.util.List.of(
                "assets/cruciblecraft/blockstates/bronze_dynamo.json",
                "assets/cruciblecraft/models/block/bronze_dynamo.json",
                "assets/cruciblecraft/models/block/bronze_dynamo_active.json",
                "assets/cruciblecraft/models/item/bronze_dynamo.json",
                "data/cruciblecraft/loot_table/blocks/bronze_dynamo.json")) {
            assertResource(MAIN, path);
        }
        assertResource(GENERATED, "data/cruciblecraft/recipe/bronze_dynamo.json");
        var blockstate = com.google.gson.JsonParser.parseString(Files.readString(
                MAIN.resolve("assets/cruciblecraft/blockstates/bronze_dynamo.json")))
                .getAsJsonObject()
                .getAsJsonObject("variants");
        assertTrue(blockstate.toString().contains("lit=true"));
        assertTrue(blockstate.toString().contains("bronze_dynamo_active"));
    }

    private static void assertResource(Path root, String path) {
        assertTrue(Files.isRegularFile(root.resolve(path)), path);
    }
}
