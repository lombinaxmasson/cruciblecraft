package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.client.color.MachineBlockColor;

class T18EnergyChainResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final Path GENERATED = Path.of("src/generated/resources");

    @Test
    void manifestListsGt6EnergyImports() throws Exception {
        var manifest = JsonParser.parseString(Files.readString(
                ROOT.resolve("assets/cruciblecraft/t34_gt6_energy_art_manifest.json")))
                .getAsJsonObject();
        assertEquals(1, manifest.get("schema_version").getAsInt());
        assertTrue(manifest.get("imports").getAsJsonArray().size() >= 60);
        var first = manifest.getAsJsonArray("imports").get(0).getAsJsonObject();
        assertTrue(first.get("gt6_source").getAsString().contains(
                "assets/gregtech/textures/blocks/"));
        assertTrue(first.get("destination").getAsString().startsWith(
                "assets/cruciblecraft/textures/block/machine/"));
        assertFalse(first.get("sha256").getAsString().isBlank());
        String notes = manifest.getAsJsonArray("notes").toString();
        assertTrue(notes.contains("barometer"));
        assertTrue(notes.contains("32-step heat tint"));
    }

    @Test
    void fuelEngineUsesMotorLiquidThreeFaceModels() throws Exception {
        assertModelParent("fuel_engine", "machine_3face_2_layer");
        assertModelParent("fuel_engine_active", "machine_3face_2_layer");
        assertTextureRoot("fuel_engine", "machines/generators/motor_liquid");
        assertBlockstateLitVariants(readBlockstate("fuel_engine"));
        assertItemUsesInactiveLayers("fuel_engine");
    }

    @Test
    void burningGasGeneratorUsesBurningGasSixFaceModels() throws Exception {
        assertModelParent("burning_gas_generator", "machine_cube_2_layer");
        assertModelParent("burning_gas_generator_active", "machine_cube_2_layer");
        assertTextureRoot("burning_gas_generator", "machines/generators/burning_gas");
        assertFalse(modelJson("burning_gas_generator").contains("burning_liquid"));
        assertFalse(modelJson("burning_gas_generator").contains("burning_solid"));
        assertFalse(modelJson("burning_gas_generator").contains("hot_fluid"));
        assertBlockstateLitVariants(readBlockstate("burning_gas_generator"));
        assertItemUsesInactiveLayers("burning_gas_generator");
    }

    @Test
    void bronzeBoilerUsesBoilerSteamThreeFaceModelWithFacing() throws Exception {
        assertModelParent("bronze_boiler", "machine_boiler_2_layer");
        assertTextureRoot("bronze_boiler", "machines/tanks/boiler_steam");
        var blockstate = JsonParser.parseString(Files.readString(
                ROOT.resolve("assets/cruciblecraft/blockstates/bronze_boiler.json")))
                .getAsJsonObject()
                .getAsJsonObject("variants");
        assertTrue(blockstate.has("facing=north"));
        assertFalse(blockstate.toString().contains("lit="));
        assertFalse(modelJson("bronze_boiler").contains("overlay_active"));
    }

    @Test
    void bronzeSteamEngineUsesKineticSteamVoxelTextures() throws Exception {
        String model = modelJson("bronze_steam_engine");
        assertTrue(model.contains("machines/engines/kinetic_steam")
                || model.contains("block/machine/bronze_steam_engine/colored/cage"));
        assertFalse(model.contains("metal_surface"));
        assertFalse(model.contains("overlay_active"));
        var blockstate = JsonParser.parseString(Files.readString(
                ROOT.resolve("assets/cruciblecraft/blockstates/bronze_steam_engine.json")))
                .getAsJsonObject()
                .getAsJsonObject("variants");
        assertTrue(blockstate.has("facing=north"));
        assertFalse(blockstate.toString().contains("lit="));
    }

    @Test
    void bronzeDynamoUsesElectricRotationThreeFaceModelsAndTinAlloyTint() throws Exception {
        assertModelParent("bronze_dynamo", "machine_3face_2_layer");
        assertModelParent("bronze_dynamo_active", "machine_3face_2_layer");
        assertTextureRoot("bronze_dynamo", "machines/dynamos/electric_rotation");
        assertBlockstateLitVariants(JsonParser.parseString(Files.readString(
                ROOT.resolve("assets/cruciblecraft/blockstates/bronze_dynamo.json")))
                .getAsJsonObject());
        assertEquals("tin_alloy",
                MachineBlockColor.casingMaterialId("bronze_dynamo"));
    }

    private static JsonObject readBlockstate(String id) throws Exception {
        return JsonParser.parseString(Files.readString(resolveBlockstate(id)))
                .getAsJsonObject();
    }

    private static Path resolveBlockstate(String id) {
        Path main = ROOT.resolve("assets/cruciblecraft/blockstates/" + id + ".json");
        if (Files.isRegularFile(main)) {
            return main;
        }
        return GENERATED.resolve("assets/cruciblecraft/blockstates/" + id + ".json");
    }

    private static void assertModelParent(String id, String parentSuffix) throws Exception {
        var model = JsonParser.parseString(modelJson(id)).getAsJsonObject();
        assertTrue(
                model.get("parent").getAsString().endsWith(parentSuffix),
                id + " parent=" + model.get("parent"));
    }

    private static void assertTextureRoot(String machineId, String gt6Root) throws Exception {
        Path textureDir = ROOT.resolve(
                "assets/cruciblecraft/textures/block/machine/" + machineId + "/colored");
        assertTrue(Files.isDirectory(textureDir), textureDir.toString());
        try (var paths = Files.list(textureDir)) {
            assertTrue(paths.findAny().isPresent());
        }
        assertTrue(modelJson(machineId).contains("block/machine/" + machineId));
        assertTrue(Files.readString(ROOT.resolve(
                "assets/cruciblecraft/t34_gt6_energy_art_manifest.json")).contains(gt6Root));
    }

    private static String modelJson(String id) throws Exception {
        Path generated = GENERATED.resolve("assets/cruciblecraft/models/block/" + id + ".json");
        Path main = ROOT.resolve("assets/cruciblecraft/models/block/" + id + ".json");
        Path path = Files.isRegularFile(main) ? main : generated;
        assertTrue(Files.isRegularFile(path), id);
        return Files.readString(path);
    }

    private static void assertBlockstateLitVariants(JsonObject blockstateRoot) {
        JsonObject variants = blockstateRoot.getAsJsonObject("variants");
        assertTrue(variants.has("facing=north,lit=false")
                || variants.has("lit=false,facing=north")
                || variants.toString().contains("lit=false"));
        assertTrue(variants.has("facing=north,lit=true")
                || variants.has("lit=true,facing=north")
                || variants.toString().contains("lit=true"));
        assertTrue(variants.toString().contains("facing=east"));
    }

    private static void assertItemUsesInactiveLayers(String id) throws Exception {
        Path main = ROOT.resolve("assets/cruciblecraft/models/item/" + id + ".json");
        Path generated = GENERATED.resolve("assets/cruciblecraft/models/item/" + id + ".json");
        Path path = Files.isRegularFile(main) ? main : generated;
        assertTrue(Files.isRegularFile(path), id);
        var item = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals("minecraft:item/generated", item.get("parent").getAsString());
        String textures = item.getAsJsonObject("textures").toString();
        assertTrue(textures.contains("block/machine/" + id + "/colored/front"));
        assertTrue(textures.contains("block/machine/" + id + "/overlay/front"));
        assertFalse(textures.contains("overlay_active"));
    }
}
