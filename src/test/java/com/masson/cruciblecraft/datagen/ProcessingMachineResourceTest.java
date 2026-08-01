package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

class ProcessingMachineResourceTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path MAIN = Path.of("src/main/resources");
    private static final List<String> MACHINES = ModProcessingMachines.CONFIGURED_MACHINES
            .stream().map(spec -> spec.id().getPath()).toList();

    @Test
    void configuredMachinesHaveLootAndMiningTags() throws Exception {
        var pickaxe = values(MAIN, "data/minecraft/tags/block/mineable/pickaxe.json");
        var stone = values(MAIN, "data/minecraft/tags/block/needs_stone_tool.json");
        for (String machine : MACHINES) {
            String id = "cruciblecraft:" + machine;
            assertTrue(pickaxe.contains(id), id + " must be pickaxe-mineable");
            assertTrue(stone.contains(id), id + " must require a stone-tier tool");
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "data/cruciblecraft/loot_table/blocks/" + machine + ".json")));
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "data/cruciblecraft/recipe/machines/" + machine + ".json")));
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "assets/cruciblecraft/blockstates/" + machine + ".json")));
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "assets/cruciblecraft/models/block/" + machine + ".json")));
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + machine + ".json")));
            var blockstate = JsonParser.parseString(Files.readString(GENERATED.resolve(
                    "assets/cruciblecraft/blockstates/" + machine + ".json"))).getAsJsonObject();
            assertEquals(
                    Set.of("facing=north", "facing=east", "facing=south", "facing=west"),
                    blockstate.getAsJsonObject("variants").keySet());
            var model = JsonParser.parseString(Files.readString(GENERATED.resolve(
                    "assets/cruciblecraft/models/block/" + machine + ".json"))).getAsJsonObject();
            assertEquals("minecraft:block/orientable", model.get("parent").getAsString());
            var textures = model.getAsJsonObject("textures");
            assertTrue(textures.has("front"), machine + " needs a visible front texture");
            assertNotEquals(textures.get("front").getAsString(), textures.get("side").getAsString());
            var lang = JsonParser.parseString(Files.readString(GENERATED.resolve(
                    "assets/cruciblecraft/lang/en_us.json"))).getAsJsonObject();
            assertTrue(lang.has("block.cruciblecraft." + machine));
        }
    }

    private static List<String> values(Path rootPath, String path) throws Exception {
        var root = JsonParser.parseString(
                Files.readString(rootPath.resolve(path))).getAsJsonObject();
        return root.getAsJsonArray("values").asList().stream()
                .map(value -> value.getAsString()).toList();
    }
}
