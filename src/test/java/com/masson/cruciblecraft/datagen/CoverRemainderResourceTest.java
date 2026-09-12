package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;

import org.junit.jupiter.api.Test;

class CoverRemainderResourceTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path ROOT = Path.of(".");

    @Test
    void everyMachineCoverHasImportedModelRecipeAndLanguages()
            throws Exception {
        JsonObject english = json(
                GENERATED.resolve("assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(
                GENERATED.resolve("assets/cruciblecraft/lang/zh_cn.json"));
        for (MachineCoverKinds.ItemCover cover : MachineCoverKinds.ITEMS) {
            String item = cover.itemPath();
            Path model = GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + item + ".json");
            Path recipe = GENERATED.resolve(
                    "data/cruciblecraft/recipe/" + item + ".json");
            assertTrue(Files.isRegularFile(model), item + " model");
            assertTrue(Files.isRegularFile(recipe), item + " recipe");
            assertEquals(
                    "cruciblecraft:" + item,
                    json(recipe).getAsJsonObject("result")
                            .get("id").getAsString());
            assertEquals(
                    cover.english(),
                    english.get("item.cruciblecraft." + item).getAsString());
            assertEquals(
                    cover.chinese(),
                    chinese.get("item.cruciblecraft." + item).getAsString());
            assertEquals(
                    "cruciblecraft:item/gt6_import/" + item,
                    json(model).getAsJsonObject("textures")
                            .get("layer0").getAsString());
        }
    }

    @Test
    void sidecarAndArtManifestStayExact() throws Exception {
        JsonObject sidecar = json(Path.of(
                "src/main/resources/data/cruciblecraft/"
                        + "machine_cover_definitions.json"));
        assertEquals(
                MachineCoverKinds.DEFINITION_COUNT,
                sidecar.getAsJsonArray("definitions").size());
        JsonObject manifest = json(Path.of(
                "src/main/resources/assets/cruciblecraft/"
                        + "gt6_machine_covers_art_manifest.json"));
        assertEquals(24, manifest.getAsJsonArray("entries").size());
        assertTrue(manifest.getAsJsonArray("block_entries").size() > 0);
        assertEquals(
                "936083c247a70b1bbc5f19996a83d75c27d196e2",
                manifest.get("source_revision").getAsString());
        for (var key : java.util.List.of("entries", "block_entries")) {
            for (var entry : manifest.getAsJsonArray(key)) {
                JsonObject row = entry.getAsJsonObject();
                Path source = ROOT.resolve(row.get("source").getAsString());
                Path destination = ROOT.resolve(
                        "src/main/resources/" + row.get("destination").getAsString());
                assertTrue(Files.isRegularFile(source), source.toString());
                assertTrue(
                        Files.isRegularFile(destination),
                        destination.toString());
                assertArrayEquals(
                        Files.readAllBytes(source),
                        Files.readAllBytes(destination),
                        row.get("destination").getAsString());
            }
        }
    }

    @Test
    void blankRecipeDoesNotUseSelectorCircuitAsAStandIn() throws Exception {
        JsonObject recipe = json(GENERATED.resolve(
                "data/cruciblecraft/recipe/cover_blank_cover.json"));
        String serialized = recipe.toString();
        assertFalse(serialized.contains("programmed_circuit"));
        assertTrue(serialized.contains("aluminium/plate"));
        assertTrue(serialized.contains("aluminium/screw"));
        assertTrue(serialized.contains("smithing_hammer"));
        assertTrue(serialized.contains("material_screwdriver"));
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseReader(Files.newBufferedReader(path))
                .getAsJsonObject();
    }
}
