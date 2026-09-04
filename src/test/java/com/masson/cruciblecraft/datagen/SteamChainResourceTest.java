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
        resource("assets/cruciblecraft/gt6_energy_art_manifest.json");
        resource("assets/cruciblecraft/models/block/machine_3face_2_layer.json");
        resource("assets/cruciblecraft/models/block/machine_boiler_2_layer.json");
        resource("assets/cruciblecraft/models/item/steam_bucket.json", GENERATED);
        resource("data/c/tags/fluid/steam.json");
        assertTrue(Files.isDirectory(ORE_CHAIN_CRUSHER.resolve("copper")));
    }

    @Test
    void fireboxHasGeneratedSurvivalResourceClosure() throws Exception {
        for (String path : java.util.List.of(
                "assets/cruciblecraft/blockstates/firebox.json",
                "assets/cruciblecraft/models/item/firebox.json",
                "data/cruciblecraft/loot_table/blocks/firebox.json",
                "data/cruciblecraft/recipe/machines/firebox.json")) {
            assertTrue(Files.isRegularFile(GENERATED.resolve(path)), path);
        }
        for (String path : java.util.List.of(
                "assets/cruciblecraft/models/block/firebox_unlit.json",
                "assets/cruciblecraft/models/block/firebox_lit.json",
                "assets/cruciblecraft/textures/block/gt6_import/firebox/colored_front.png",
                "assets/cruciblecraft/textures/block/gt6_import/firebox/overlay_active_front.png",
                "assets/cruciblecraft/textures/block/gt6_import/firebox/overlay_active_front.png.mcmeta")) {
            assertTrue(Files.isRegularFile(ROOT.resolve(path)), path);
        }
        var variants = JsonParser.parseString(Files.readString(GENERATED.resolve(
                "assets/cruciblecraft/blockstates/firebox.json")))
                .getAsJsonObject()
                .getAsJsonObject("variants");
        assertEquals(
                "cruciblecraft:block/firebox_unlit",
                variants.getAsJsonObject("facing=north,lit=false")
                        .get("model").getAsString());
        assertEquals(
                "cruciblecraft:block/firebox_lit",
                variants.getAsJsonObject("facing=north,lit=true")
                        .get("model").getAsString());
        String recipe = Files.readString(GENERATED.resolve(
                "data/cruciblecraft/recipe/machines/firebox.json"));
        assertTrue(recipe.contains("\"cruciblecraft:firebrick\""));
        assertTrue(recipe.contains("\"minecraft:furnace\""));
        assertTrue(recipe.contains("\"cruciblecraft:firebox\""));
    }

    @Test
    void bronzeEnergyArtUsesPinnedGt6LayersAndInventoryModels()
            throws Exception {
        var fuelEngine = model("fuel_engine");
        assertTrue(fuelEngine.get("credit").getAsString().contains(
                "MultiTileEntityMotorLiquid"));
        assertHasTexture(
                fuelEngine,
                "cruciblecraft:block/machine/fuel_engine/colored/front");
        assertHasTexture(
                fuelEngine,
                "cruciblecraft:block/machine/fuel_engine/colored/back");
        assertHasTexture(
                fuelEngine,
                "cruciblecraft:block/machine/fuel_engine/colored/sides");
        assertHasTexture(
                model("fuel_engine_active"),
                "cruciblecraft:block/machine/fuel_engine/overlay_active/front");
        assertTextureFiles(
                "fuel_engine",
                java.util.List.of("front", "back", "sides"),
                true);
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/block/machine/fuel_engine/"
                        + "overlay_active/front.png.mcmeta")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/block/machine/fuel_engine/"
                        + "overlay_active/sides.png.mcmeta")));

        var gasBurningBox = model("burning_gas_generator");
        assertTrue(gasBurningBox.get("credit").getAsString().contains(
                "MultiTileEntityGeneratorGas"));
        for (String face : java.util.List.of(
                "bottom", "top", "left", "front", "right", "back")) {
            String direction = switch (face) {
                case "bottom" -> "down";
                case "top" -> "up";
                case "left" -> "west";
                case "right" -> "east";
                default -> face.equals("front") ? "north" : "south";
            };
            assertTexture(
                    gasBurningBox, "bot_" + direction,
                    "cruciblecraft:block/machine/burning_gas_generator/"
                            + "colored/" + face);
            assertTexture(
                    model("burning_gas_generator_active"),
                    "top_" + direction,
                    "cruciblecraft:block/machine/burning_gas_generator/"
                            + "overlay_active/" + face);
        }
        assertTextureFiles(
                "burning_gas_generator",
                java.util.List.of(
                        "bottom", "top", "left", "front", "right", "back"),
                true);
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/block/machine/"
                        + "burning_gas_generator/overlay_active/front.png.mcmeta")));

        var boiler = model("bronze_boiler");
        assertTrue(boiler.get("credit").getAsString().contains(
                "MultiTileEntityBoilerTank"));
        assertHasTexture(
                boiler,
                "cruciblecraft:block/machine/bronze_boiler/colored/bottom");
        assertHasTexture(
                boiler,
                "cruciblecraft:block/machine/bronze_boiler/colored/top");
        assertHasTexture(
                boiler,
                "cruciblecraft:block/machine/bronze_boiler/colored/side");
        assertTextureFiles(
                "bronze_boiler",
                java.util.List.of("bottom", "top", "side"),
                false);

        var steamEngine = model("bronze_steam_engine");
        assertTrue(steamEngine.get("credit").getAsString().contains(
                "MultiTileEntityEngineSteam"));
        for (String layer : java.util.List.of("colored", "overlay")) {
            for (String part : java.util.List.of(
                    "front", "back", "side", "cage", "pipe_side", "pipe",
                    "engine", "engine_hull")) {
                String texture = "cruciblecraft:block/machine/"
                        + "bronze_steam_engine/" + layer + "/" + part;
                assertTrue(
                        steamEngine.getAsJsonObject("textures")
                                .entrySet().stream()
                                .anyMatch(entry ->
                                        texture.equals(
                                                entry.getValue().getAsString())),
                        texture);
            }
        }
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/block/machine/"
                        + "bronze_steam_engine/colored/engine_hull.png")));

        var dynamo = model("bronze_dynamo");
        assertTrue(dynamo.get("credit").getAsString().contains(
                "MultiTileEntityDynamoElectric"));
        for (String face : java.util.List.of("front", "back", "side")) {
            assertTrue(
                    dynamo.getAsJsonObject("textures").entrySet().stream()
                            .anyMatch(entry -> entry.getValue().getAsString()
                                    .endsWith("/colored/" + face)));
            assertTrue(
                    model("bronze_dynamo_active")
                            .getAsJsonObject("textures").entrySet().stream()
                            .anyMatch(entry -> entry.getValue().getAsString()
                                    .endsWith("/overlay_active/" + face)));
        }
        assertTextureFiles(
                "bronze_dynamo",
                java.util.List.of("front", "back", "side"),
                true);

        assertFacingLitVariants(
                GENERATED,
                "assets/cruciblecraft/blockstates/fuel_engine.json",
                "cruciblecraft:block/fuel_engine",
                "cruciblecraft:block/fuel_engine_active");
        assertFacingLitVariants(
                GENERATED,
                "assets/cruciblecraft/blockstates/burning_gas_generator.json",
                "cruciblecraft:block/burning_gas_generator",
                "cruciblecraft:block/burning_gas_generator_active");
        assertFacingLitVariants(
                ROOT,
                "assets/cruciblecraft/blockstates/bronze_dynamo.json",
                "cruciblecraft:block/bronze_dynamo",
                "cruciblecraft:block/bronze_dynamo_active");

        assertLayeredItem(GENERATED, "fuel_engine", "fuel_engine", "front");
        assertLayeredItem(
                GENERATED,
                "burning_gas_generator",
                "burning_gas_generator",
                "front");
        assertLayeredItem(ROOT, "bronze_boiler", "bronze_boiler", "side");
        assertLayeredItem(
                ROOT, "bronze_steam_engine", "bronze_steam_engine", "front");
        assertLayeredItem(ROOT, "bronze_dynamo", "bronze_dynamo", "front");
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

    private static com.google.gson.JsonObject model(String id)
            throws Exception {
        return JsonParser.parseString(Files.readString(ROOT.resolve(
                "assets/cruciblecraft/models/block/" + id + ".json")))
                .getAsJsonObject();
    }

    private static void assertTexture(
            com.google.gson.JsonObject model, String key, String expected) {
        assertEquals(
                expected,
                model.getAsJsonObject("textures").get(key).getAsString(),
                key);
    }

    private static void assertHasTexture(
            com.google.gson.JsonObject model, String expected) {
        assertTrue(model.getAsJsonObject("textures").entrySet().stream()
                .anyMatch(entry -> expected.equals(
                        entry.getValue().getAsString())), expected);
    }

    private static void assertTextureFiles(
            String machine, java.util.List<String> faces, boolean active)
            throws Exception {
        for (String layer : java.util.List.of("colored", "overlay")) {
            for (String face : faces) {
                resource(
                        "assets/cruciblecraft/textures/block/machine/"
                                + machine + "/" + layer + "/" + face + ".png");
            }
        }
        if (active) {
            for (String face : faces) {
                resource(
                        "assets/cruciblecraft/textures/block/machine/"
                                + machine + "/overlay_active/" + face + ".png");
            }
        }
    }

    private static void assertFacingLitVariants(
            Path root,
            String path,
            String inactiveModel,
            String activeModel) throws Exception {
        var variants = JsonParser.parseString(
                Files.readString(root.resolve(path))).getAsJsonObject()
                .getAsJsonObject("variants");
        for (String facing : java.util.List.of(
                "north", "east", "south", "west")) {
            assertEquals(
                    inactiveModel,
                    variants.getAsJsonObject(
                            "facing=" + facing + ",lit=false")
                            .get("model").getAsString());
            assertEquals(
                    activeModel,
                    variants.getAsJsonObject(
                            "facing=" + facing + ",lit=true")
                            .get("model").getAsString());
        }
    }

    private static void assertLayeredItem(
            Path root, String id, String machine, String face)
            throws Exception {
        var item = JsonParser.parseString(Files.readString(root.resolve(
                "assets/cruciblecraft/models/item/" + id + ".json")))
                .getAsJsonObject();
        assertEquals("minecraft:item/generated",
                item.get("parent").getAsString(), id);
        assertEquals(
                "cruciblecraft:block/machine/" + machine
                        + "/colored/" + face,
                item.getAsJsonObject("textures").get("layer0").getAsString(),
                id);
        assertEquals(
                "cruciblecraft:block/machine/" + machine
                        + "/overlay/" + face,
                item.getAsJsonObject("textures").get("layer1").getAsString(),
                id);
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
