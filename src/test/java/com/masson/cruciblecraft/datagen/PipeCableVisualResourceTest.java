package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class PipeCableVisualResourceTest {
    private static final Path ASSETS = Path.of(
            "src/main/resources/assets/cruciblecraft/textures/block");
    private static final Path GENERATED = Path.of(
            "src/generated/resources/assets/cruciblecraft/models");

    @Test
    void gt6PipeAndCableTexturesArePresent() {
        Path copper = ASSETS.resolve("gt6_import/materialicons/copper");
        Path iconsets = ASSETS.resolve("gt6_import/iconsets");
        assertTrue(Files.isRegularFile(copper.resolve("pipetiny.png")));
        assertTrue(Files.isRegularFile(copper.resolve("pipetiny_overlay.png")));
        assertTrue(Files.isRegularFile(copper.resolve("wire.png")));
        assertTrue(Files.isRegularFile(copper.resolve("wire_overlay.png")));
        assertTrue(Files.isRegularFile(iconsets.resolve("insulation_tiny.png")));
        assertTrue(Files.isRegularFile(iconsets.resolve("pipe_restrictor.png")));
        assertFalse(Files.isRegularFile(copper.resolve("cable.png")));
        assertFalse(Files.isRegularFile(iconsets.resolve("cable.png")));
    }

    @Test
    void generatedConductorModelsUseSharedGt6Iconsets() throws Exception {
        JsonObject core = json(GENERATED.resolve("conductor/cablegt01_core.json"));
        JsonObject textures = core.getAsJsonObject("textures");
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/wire",
                textures.get("side").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6_import/iconsets/insulation_tiny",
                textures.get("overlay").getAsString());
        JsonObject item = json(GENERATED.resolve("conductor/cablegt01_item.json"));
        assertTrue(item.getAsJsonArray("elements").size() >= 2);
        JsonObject wire = json(GENERATED.resolve("conductor/wiregt01_core.json"));
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/wire",
                wire.getAsJsonObject("textures").get("side").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/wire_overlay",
                wire.getAsJsonObject("textures").get("overlay").getAsString());
        JsonObject pipe = json(GENERATED.resolve("pipe/fluid_4_core.json"));
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/pipetiny",
                pipe.getAsJsonObject("textures").get("side").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/pipetiny_overlay",
                pipe.getAsJsonObject("textures").get("overlay").getAsString());
    }

    private static JsonObject json(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), path.toString());
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
