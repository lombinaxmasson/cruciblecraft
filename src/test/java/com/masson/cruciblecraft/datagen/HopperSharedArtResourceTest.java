package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class HopperSharedArtResourceTest {
    private static final Path MODELS = Path.of(
            "src/main/resources/assets/cruciblecraft/models/block");
    private static final Path TEXTURES = Path.of(
            "src/main/resources/assets/cruciblecraft/textures/block/material");

    @Test
    void sharedHopperFamilyModelsAreTintableAndLoadable() throws IOException {
        assertTrue(Files.isRegularFile(TEXTURES.resolve("block.png")));
        assertTrue(Files.isRegularFile(TEXTURES.resolve("block_overlay.png")));
        for (String name : new String[] {
                "hopper.json",
                "hopper_side.json",
                "queue_hopper.json",
                "queue_hopper_side.json",
                "dust_funnel.json"}) {
            JsonObject model = JsonParser.parseString(
                    Files.readString(MODELS.resolve(name))).getAsJsonObject();
            JsonObject textures = model.getAsJsonObject("textures");
            assertEquals(
                    "cruciblecraft:block/material/block",
                    textures.get("colored").getAsString(),
                    name);
            assertTrue(textures.has("particle"), name);
            boolean overlayFamily = name.startsWith("queue_")
                    || name.startsWith("dust_");
            if (overlayFamily) {
                assertEquals(
                        "cruciblecraft:block/material/block_overlay",
                        textures.get("overlay").getAsString(),
                        name);
            } else {
                assertFalse(textures.has("overlay"), name);
            }
            boolean sawColored = false;
            boolean sawOverlay = false;
            for (JsonElement element : model.getAsJsonArray("elements")) {
                JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
                for (var entry : faces.entrySet()) {
                    JsonObject face = entry.getValue().getAsJsonObject();
                    String texture = face.get("texture").getAsString();
                    if ("#colored".equals(texture)) {
                        sawColored = true;
                        assertEquals(0, face.get("tintindex").getAsInt(), name);
                    } else if ("#overlay".equals(texture)) {
                        sawOverlay = true;
                        assertFalse(face.has("tintindex"), name + " " + entry.getKey());
                    }
                }
            }
            assertTrue(sawColored, name);
            assertEquals(overlayFamily, sawOverlay, name);
        }
        assertFalse(Files.exists(Path.of(
                "src/main/resources/assets/cruciblecraft/textures/block/lead_hopper.png")));
    }
}
