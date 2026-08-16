package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void gtmPipeAndCableTexturesArePresent() {
        assertTrue(Files.isRegularFile(ASSETS.resolve("material/wire_side.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve("material/wire_end.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve("pipe/pipe_side.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve("pipe/pipe_tiny_in.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve("pipe/pipe_normal_in.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve("cable/insulation_0.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve("cable/insulation_5.png")));
    }

    @Test
    void generatedConductorModelsUseSideAndEndInsteadOfASingleAllTexture()
            throws Exception {
        JsonObject core = json(GENERATED.resolve("conductor/cablegt01_core.json"));
        JsonObject textures = core.getAsJsonObject("textures");
        assertEquals(
                "cruciblecraft:block/cable/insulation_5",
                textures.get("side").getAsString());
        assertEquals(
                "cruciblecraft:block/material/wire_end",
                textures.get("end").getAsString());
        JsonObject item = json(GENERATED.resolve("conductor/cablegt01_item.json"));
        assertTrue(item.getAsJsonArray("elements").size() >= 1);
        JsonObject wire = json(GENERATED.resolve("conductor/wiregt01_core.json"));
        assertEquals(
                "cruciblecraft:block/material/wire_side",
                wire.getAsJsonObject("textures").get("side").getAsString());
        JsonObject pipe = json(GENERATED.resolve("pipe/fluid_4_core.json"));
        assertEquals(
                "cruciblecraft:block/pipe/pipe_side",
                pipe.getAsJsonObject("textures").get("side").getAsString());
        assertEquals(
                "cruciblecraft:block/pipe/pipe_tiny_in",
                pipe.getAsJsonObject("textures").get("end").getAsString());
    }

    private static JsonObject json(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), path.toString());
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
