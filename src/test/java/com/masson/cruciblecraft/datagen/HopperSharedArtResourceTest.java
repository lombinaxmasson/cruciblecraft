package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class HopperSharedArtResourceTest {
    private static final Path MODELS = Path.of(
            "src/main/resources/assets/cruciblecraft/models/block");
    private static final Path TEXTURES = Path.of(
            "src/main/resources/assets/cruciblecraft/textures/block");

    @Test
    void sharedHopperFamilyModelsUseGt6ColoredSheets() throws IOException {
        assertTrue(Files.isRegularFile(TEXTURES.resolve("hopper/colored_bottom.png")));
        assertTrue(Files.isRegularFile(TEXTURES.resolve("hopper/colored_top.png")));
        assertTrue(Files.isRegularFile(TEXTURES.resolve("hopper/colored_side.png")));
        assertTrue(Files.isRegularFile(
                TEXTURES.resolve("queue_hopper/colored_side.png")));
        assertTrue(Files.isRegularFile(
                TEXTURES.resolve("dust_funnel/colored_hole.png")));
        for (String name : new String[] {
                "hopper.json",
                "hopper_side.json",
                "queue_hopper.json",
                "queue_hopper_side.json",
                "dust_funnel.json"}) {
            JsonObject model = JsonParser.parseString(
                    Files.readString(MODELS.resolve(name))).getAsJsonObject();
            JsonObject textures = model.getAsJsonObject("textures");
            boolean dust = name.startsWith("dust_");
            boolean queue = name.startsWith("queue_");
            String folder = dust ? "dust_funnel" : queue ? "queue_hopper" : "hopper";
            String sideKey = dust ? "sides" : "side";
            assertEquals(
                    "cruciblecraft:block/" + folder + "/colored_bottom",
                    textures.get("bottom").getAsString(),
                    name);
            assertEquals(
                    "cruciblecraft:block/" + folder + "/colored_top",
                    textures.get("top").getAsString(),
                    name);
            assertEquals(
                    "cruciblecraft:block/" + folder + "/colored_" + sideKey,
                    textures.get(sideKey).getAsString(),
                    name);
            assertEquals("#" + sideKey, textures.get("particle").getAsString(), name);
            assertFalse(textures.has("colored"), name);
            assertFalse(textures.has("overlay"), name);
            if (dust) {
                assertEquals(
                        "cruciblecraft:block/dust_funnel/colored_hole",
                        textures.get("hole").getAsString(),
                        name);
            } else {
                assertFalse(textures.has("hole"), name);
            }
            Set<String> allowed = dust
                    ? Set.of("#bottom", "#top", "#sides", "#hole")
                    : Set.of("#bottom", "#top", "#side");
            boolean sawHole = false;
            for (JsonElement element : model.getAsJsonArray("elements")) {
                JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
                for (var entry : faces.entrySet()) {
                    JsonObject face = entry.getValue().getAsJsonObject();
                    String texture = face.get("texture").getAsString();
                    assertTrue(allowed.contains(texture), name + " " + texture);
                    assertEquals(0, face.get("tintindex").getAsInt(), name);
                    if ("#hole".equals(texture)) {
                        sawHole = true;
                    }
                }
            }
            assertEquals(dust, sawHole, name);
        }
        assertFalse(Files.exists(Path.of(
                "src/main/resources/assets/cruciblecraft/textures/block/lead_hopper.png")));
    }
}
