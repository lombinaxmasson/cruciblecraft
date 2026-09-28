package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class HandheldToolArtResourceTest {
    private static final Path CC = Path.of(
            "src/main/resources/assets/cruciblecraft/textures/item/tool");

    @Test
    void committedHandheldIconsStayDistinctAndManifested() throws Exception {
        assertFalse(
                java.util.Arrays.equals(
                        Files.readAllBytes(CC.resolve("wrench.png")),
                        Files.readAllBytes(CC.resolve("wire_cutter.png"))),
                "handheld wrench and wire cutter must not share one sheet");
        JsonObject manifest = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/cruciblecraft/"
                        + "gt6_handheld_tool_art_manifest.json")))
                .getAsJsonObject();
        assertEquals(5, manifest.getAsJsonArray("imports").size());
        assertEquals(
                "gt6_referencable_port_code/gregtech6_w",
                manifest.get("source").getAsString());
    }
}
