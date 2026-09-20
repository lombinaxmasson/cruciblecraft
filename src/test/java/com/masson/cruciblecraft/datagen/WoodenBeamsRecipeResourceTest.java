package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.block.WoodDebark;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;
import org.junit.jupiter.api.Test;

class WoodenBeamsRecipeResourceTest {
    private static final Path GENERATED = Path.of(
            "src/generated/resources/data/cruciblecraft/recipe");

    @Test
    void cokeAndWasherJsonMatchWoodDebarkCounts() throws Exception {
        Path coke = GENERATED.resolve("coke_oven");
        Path washer = GENERATED.resolve("pressurewasher");
        int cokeRows = 0;
        int washerRows = 0;
        try (var paths = Files.list(coke)) {
            cokeRows = (int) paths.filter(path -> {
                String name = path.getFileName().toString();
                return name.startsWith("stripped_")
                        || name.endsWith("_beam.json");
            }).count();
        }
        try (var paths = Files.list(washer)) {
            washerRows = (int) paths.filter(Files::isRegularFile).count();
        }
        assertEquals(
                WoodDebark.VANILLA_BEAM_COKE_INPUTS.size()
                        + GtTreeSpecies.ALL.size(),
                cokeRows);
        assertEquals(
                WoodDebark.extraPressureWasherWoodRows(),
                washerRows);
        for (var log : WoodDebark.GT6_PRESSURE_WASHER_VANILLA_LOGS) {
            Path shadowed = washer.resolve(log.getPath() + ".json");
            assertTrue(
                    Files.notExists(shadowed),
                    "GT6 mill already owns this water log: " + shadowed);
        }
    }

    @Test
    void sampleAmountsFollowGt6BeamAndWasherRows() throws Exception {
        JsonObject willow = read("coke_oven/willow_beam.json");
        assertEquals("cruciblecraft:coke_oven", willow.get("map").getAsString());
        assertEquals(
                2,
                willow.getAsJsonArray("item_outputs")
                        .get(0).getAsJsonObject()
                        .get("count").getAsInt());
        assertEquals(
                400,
                willow.getAsJsonArray("fluid_outputs")
                        .get(0).getAsJsonObject()
                        .get("amount").getAsInt());

        JsonObject oakWasher = read("pressurewasher/oak_wood.json");
        assertEquals(
                "cruciblecraft:pressurewasher",
                oakWasher.get("map").getAsString());
        assertEquals(16, oakWasher.get("eut").getAsInt());
        assertEquals(
                "minecraft:stripped_oak_wood",
                oakWasher.getAsJsonArray("item_outputs")
                        .get(0).getAsJsonObject()
                        .get("id").getAsString());
        assertEquals(
                "cruciblecraft:bark/dust",
                oakWasher.getAsJsonArray("item_outputs")
                        .get(1).getAsJsonObject()
                        .get("id").getAsString());

        JsonObject cinnamon = read("pressurewasher/cinnamon_log.json");
        assertEquals(
                "cruciblecraft:cinnamon/bark_don_t_let_anyone_challenge_you",
                cinnamon.getAsJsonArray("item_outputs")
                        .get(1).getAsJsonObject()
                        .get("id").getAsString());
    }

    @Test
    void hugeWoodPipeUsesWoodenBeamsTag() throws Exception {
        JsonObject recipe = read("pipe_acquisition/wood/huge_fluid_pipe.json");
        assertEquals(
                "cruciblecraft:wooden_beams",
                recipe.getAsJsonObject("key")
                        .getAsJsonObject("W")
                        .get("tag")
                        .getAsString());
    }

    private static JsonObject read(String relative) throws Exception {
        Path path = GENERATED.resolve(relative);
        assertTrue(Files.isRegularFile(path), path.toString());
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
