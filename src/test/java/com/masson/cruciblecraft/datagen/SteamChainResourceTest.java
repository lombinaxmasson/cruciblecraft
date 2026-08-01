package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class SteamChainResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final Path ORE_CHAIN_CRUSHER = Path.of(
            "src/ore_chain_generated/resources/data/cruciblecraft/recipe/"
                    + "ore_chain/crusher");
    @Test void survivalCarbonRecipeUsesOnlyCoalCokeAndOneDust() throws Exception {
        String json = Files.readString(ROOT.resolve("data/cruciblecraft/recipe/coal_coke_to_carbon_dust.json"));
        assertTrue(json.contains("\"cruciblecraft:coal_coke\""));
        assertTrue(json.contains("\"id\": \"cruciblecraft:carbon/dust\""));
        assertTrue(json.contains("\"count\": 1"));
    }
    @Test void machinesHaveModelsLootRecipesAndCrusherData() {
        for (String id : java.util.List.of("bronze_boiler", "bronze_steam_engine", "bronze_crusher")) {
            resource("assets/cruciblecraft/blockstates/" + id + ".json");
            resource("assets/cruciblecraft/models/block/" + id + ".json");
            resource("assets/cruciblecraft/models/item/" + id + ".json");
            resource("data/cruciblecraft/loot_table/blocks/" + id + ".json");
            resource("data/cruciblecraft/recipe/" + id + ".json");
        }
        resource("assets/cruciblecraft/models/item/steam_bucket.json");
        resource("data/c/tags/fluid/steam.json");
        assertTrue(Files.isDirectory(ORE_CHAIN_CRUSHER.resolve("copper")));
    }

    @Test
    void crusherRecipePreservesTopologyYieldAndMaterialDurations() throws Exception {
        assertCrusherRecipe("copper", 128);
        assertCrusherRecipe("lead", 256);
        assertCrusherRecipe("nickel", 384);
    }

    private static void resource(String path) { assertTrue(Files.isRegularFile(ROOT.resolve(path)), path); }

    private static void assertCrusherRecipe(String material, int duration) throws Exception {
        Path directory = ORE_CHAIN_CRUSHER.resolve(material);
        Path path;
        try (var paths = Files.list(directory)) {
            path = paths.filter(Files::isRegularFile).findFirst().orElseThrow();
        }
        var recipe = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals("cruciblecraft:gt_recipe", recipe.get("type").getAsString());
        assertEquals(
                2,
                recipe.getAsJsonArray("item_outputs")
                        .get(0).getAsJsonObject()
                        .get("count").getAsInt());
        assertEquals(duration, recipe.get("duration").getAsInt());
    }
}
