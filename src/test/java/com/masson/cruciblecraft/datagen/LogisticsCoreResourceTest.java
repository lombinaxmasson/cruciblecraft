package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class LogisticsCoreResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final Path GENERATED =
            Path.of("src/generated/resources");
    private static final Path MANIFEST = ROOT.resolve(
            "assets/cruciblecraft/logistics_core_art_manifest.json");

    @Test
    void manifestCopiesLocalGt6WTextures() throws Exception {
        JsonObject manifest = json(MANIFEST);
        assertEquals(1, manifest.get("schema_version").getAsInt());
        assertEquals(
                "gt6_referencable_port_code/gregtech6_w",
                manifest.get("source").getAsString());
        JsonArray imports = manifest.getAsJsonArray("imports");
        assertEquals(55, imports.size());
        for (var element : imports) {
            JsonObject row = element.getAsJsonObject();
            String destination = row.get("destination").getAsString();
            String source = row.get("gt6_source").getAsString();
            assertTrue(destination.startsWith("assets/cruciblecraft/textures/"));
            assertTrue(destination.contains("/gt6_import/"));
            assertTrue(source.startsWith("assets/gregtech/textures/"));
            assertFalse(source.contains("github"));
            assertTrue(
                    Files.isRegularFile(ROOT.resolve(destination)),
                    destination);
        }
    }

    @Test
    void dumpCoverUsesImportedGt6IconNotPipeFilter() throws Exception {
        JsonObject model = json(GENERATED.resolve(
                "assets/cruciblecraft/models/item/"
                        + "logistics_generic_dump_cover.json"));
        assertEquals(
                "cruciblecraft:item/gt6_import/logistics_generic_dump_cover",
                model.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/item/gt6_import/"
                        + "logistics_generic_dump_cover.png")));
    }

    @Test
    void coreBlocksUseImportedModelsNotBorrowedPorts() throws Exception {
        JsonObject core = json(GENERATED.resolve(
                "assets/cruciblecraft/blockstates/logistics_core.json"));
        JsonObject north = core.getAsJsonObject("variants")
                .getAsJsonObject("facing=north");
        assertEquals(
                "cruciblecraft:block/logistics_core",
                north.get("model").getAsString());
        JsonObject wall = json(ROOT.resolve(
                "assets/cruciblecraft/blockstates/multiblock/galvanized_steel_wall.json"));
        assertEquals(
                "cruciblecraft:block/large_crucible_wall",
                wall.getAsJsonObject("variants")
                        .getAsJsonObject("facing=north")
                        .get("model")
                        .getAsString());
        String wallModel = Files.readString(ROOT.resolve(
                "assets/cruciblecraft/models/block/large_crucible_wall.json"));
        assertTrue(wallModel.contains("gt6_import/multiblockparts/metalwall"));
        assertFalse(wallModel.contains("multiblock_energy_input_port"));
        assertFalse(wallModel.contains("multiblock_casing"));
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
