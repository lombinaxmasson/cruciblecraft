package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.logistics.pipe.PipeAcquisitionRecipeCatalog;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipeAcquisitionResourceTest {
    private static final Path GENERATED =
            Path.of("src/generated/resources");

    @Test
    void allTwentyFiveOutputsHaveReachableRuntimeResources(
            @TempDir Path configDirectory) throws Exception {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var serverFiles =
                GeneratedMaterialPack.planServerFiles(materials, registered);
        var clientFiles =
                GeneratedMaterialPack.planClientFiles(materials, registered);

        assertEquals(25, PipeAcquisitionRecipeCatalog.ALL.size());
        for (var spec : PipeAcquisitionRecipeCatalog.ALL) {
            var material = materials.stream()
                    .filter(candidate ->
                            candidate.id().equals(spec.materialId()))
                    .findFirst()
                    .orElseThrow();
            assertTrue(
                    registered.get(spec.materialId()).contains(spec.output()),
                    spec.id().toString());
            String registryPath =
                    spec.materialId() + "/" + spec.output().serializedName();
            Path recipe = GENERATED.resolve(
                    "data/cruciblecraft/recipe/"
                            + spec.id().getPath() + ".json");
            Path blockstate = GENERATED.resolve(
                    "assets/cruciblecraft/blockstates/"
                            + registryPath + ".json");
            Path blockModel = GENERATED.resolve(
                    "assets/cruciblecraft/models/"
                            + registryPath + ".json");
            Path loot = GENERATED.resolve(
                    "data/cruciblecraft/loot_table/blocks/"
                            + registryPath + ".json");
            for (Path path : java.util.List.of(
                    recipe, blockstate, blockModel, loot)) {
                assertTrue(Files.isRegularFile(path), path.toString());
            }

            JsonObject recipeJson = json(recipe);
            assertEquals(
                    "minecraft:crafting_shaped",
                    recipeJson.get("type").getAsString());
            assertEquals(
                    "cruciblecraft:" + registryPath,
                    recipeJson.getAsJsonObject("result")
                            .get("id").getAsString());
            assertEquals(
                    spec.outputCount(),
                    recipeJson.getAsJsonObject("result")
                            .get("count").getAsInt());
            assertEquals(
                    spec.pattern(),
                    recipeJson.getAsJsonArray("pattern").asList().stream()
                            .map(element -> element.getAsString())
                            .toList());

            String tagBase = "data/" + spec.output().tagNamespace()
                    + "/tags/";
            String tagPath = spec.output().tagDirectory()
                    + "/" + material.tagName() + ".json";
            assertTrue(
                    serverFiles.containsKey(tagBase + "block/" + tagPath),
                    spec.id().toString());
            assertTrue(
                    serverFiles.containsKey(tagBase + "item/" + tagPath),
                    spec.id().toString());
            assertTrue(
                    clientFiles.containsKey(
                            "assets/cruciblecraft/models/item/"
                                    + registryPath + ".json"),
                    spec.id().toString());
        }
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(
                Files.readString(path)).getAsJsonObject();
    }
}
