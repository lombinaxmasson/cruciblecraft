package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class WoodenBeamsArtResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final Path MANIFEST = ROOT.resolve(
            "assets/cruciblecraft/gt6_wooden_beams_art_manifest.json");

    @Test
    void beamTexturesAreAlreadyPresentGt6Iconsets() throws Exception {
        JsonObject document = JsonParser.parseString(Files.readString(MANIFEST))
                .getAsJsonObject();
        JsonArray imports = document.getAsJsonArray("imports");
        assertEquals(18, imports.size());
        for (var element : imports) {
            JsonObject row = element.getAsJsonObject();
            assertEquals("already_present", row.get("status").getAsString());
            assertEquals(
                    "gt6_referencable_port_code/gregtech6_w",
                    row.get("source").getAsString());
            Path destination = ROOT.resolve(row.get("destination").getAsString());
            assertTrue(Files.isRegularFile(destination), destination.toString());
            assertTrue(
                    row.get("destination").getAsString().contains("/iconsets/beam_"),
                    row.get("destination").getAsString());
        }
    }
}
