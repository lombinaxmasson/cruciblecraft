package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SteamChainResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");
    @Test void survivalCarbonRecipeUsesOnlyCoalCokeAndOneDust() throws Exception {
        String json = Files.readString(ROOT.resolve("data/cruciblecraft/recipe/coal_coke_to_carbon_dust.json"));
        assertTrue(json.contains("\"cruciblecraft:coal_coke\""));
        assertTrue(json.contains("\"id\": \"cruciblecraft:carbon_dust\""));
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
        resource("data/cruciblecraft/recipe/crusher/raw_ore_to_crushed_ore.json");
    }
    private static void resource(String path) { assertTrue(Files.isRegularFile(ROOT.resolve(path)), path); }
}
