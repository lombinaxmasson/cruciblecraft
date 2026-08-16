package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class SteamChainResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final Path GENERATED =
            Path.of("src/generated/resources");
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
        resource("assets/cruciblecraft/models/item/steam_bucket.json", GENERATED);
        resource("data/c/tags/fluid/steam.json");
        assertTrue(Files.isDirectory(ORE_CHAIN_CRUSHER.resolve("copper")));
    }

    @Test
    void fireboxHasGeneratedSurvivalResourceClosure() throws Exception {
        for (String path : java.util.List.of(
                "assets/cruciblecraft/blockstates/firebox.json",
                "assets/cruciblecraft/models/block/firebox.json",
                "assets/cruciblecraft/models/item/firebox.json",
                "data/cruciblecraft/loot_table/blocks/firebox.json",
                "data/cruciblecraft/recipe/machines/firebox.json")) {
            assertTrue(Files.isRegularFile(GENERATED.resolve(path)), path);
        }
        String recipe = Files.readString(GENERATED.resolve(
                "data/cruciblecraft/recipe/machines/firebox.json"));
        assertTrue(recipe.contains("\"cruciblecraft:firebrick\""));
        assertTrue(recipe.contains("\"minecraft:furnace\""));
        assertTrue(recipe.contains("\"cruciblecraft:firebox\""));
    }

    @Test
    void crusherRecipePreservesTopologyYieldAndMaterialDurations() throws Exception {
        assertCrusherRecipe("copper", 128);
        assertCrusherRecipe("lead", 256);
        assertCrusherRecipe("nickel", 384);
    }

    private static void resource(String path) {
        resource(path, ROOT);
    }

    private static void resource(String path, Path root) {
        assertTrue(Files.isRegularFile(root.resolve(path)), path);
    }

    private static void assertCrusherRecipe(String material, int duration) throws Exception {
        Path directory = ORE_CHAIN_CRUSHER.resolve(material);
        java.util.List<Path> recipePaths;
        try (var paths = Files.list(directory)) {
            recipePaths = paths.filter(Files::isRegularFile).toList();
        }
        assertEquals(2, recipePaths.size());
        com.google.gson.JsonObject rawRecipe = null;
        com.google.gson.JsonObject oreBlockRecipe = null;
        for (Path path : recipePaths) {
            var recipe = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            String tag = recipe.getAsJsonArray("item_inputs")
                    .get(0).getAsJsonObject().get("tag").getAsString();
            if (tag.equals("c:raw_materials/" + material)) {
                rawRecipe = recipe;
            } else if (tag.equals("c:ores/" + material)) {
                oreBlockRecipe = recipe;
            }
        }
        assertTrue(rawRecipe != null, material + " raw-ore crusher recipe missing");
        assertTrue(oreBlockRecipe != null, material + " ore-block crusher recipe missing");
        assertEquals("cruciblecraft:gt_recipe", rawRecipe.get("type").getAsString());
        assertEquals(
                2,
                rawRecipe.getAsJsonArray("item_outputs")
                        .get(0).getAsJsonObject()
                        .get("count").getAsInt());
        assertEquals(
                5,
                oreBlockRecipe.getAsJsonArray("item_outputs")
                        .get(0).getAsJsonObject()
                        .get("count").getAsInt());
        assertEquals(duration, rawRecipe.get("duration").getAsInt());
        assertEquals(duration, oreBlockRecipe.get("duration").getAsInt());
        assertEquals(rawRecipe.get("eut"), oreBlockRecipe.get("eut"));
        assertEquals(
                rawRecipe.get("output_chances"),
                oreBlockRecipe.get("output_chances"));
    }
}
