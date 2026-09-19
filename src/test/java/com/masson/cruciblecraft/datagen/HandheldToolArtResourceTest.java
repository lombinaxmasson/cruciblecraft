package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class HandheldToolArtResourceTest {
    private static final Path GT6 = Path.of(
            "gt6_referencable_port_code/gregtech6_w/src/main/resources/"
                    + "assets/gregtech/textures/items/iconsets");
    private static final Path CC = Path.of(
            "src/main/resources/assets/cruciblecraft/textures/item/tool");

    @Test
    void wrenchAndWireCutterMatchGt6HandheldIconsNotToolHeads() throws Exception {
        assumeGt6();
        assertArrayEquals(
                Files.readAllBytes(GT6.resolve("wrench.png")),
                Files.readAllBytes(CC.resolve("wrench.png")));
        assertArrayEquals(
                Files.readAllBytes(GT6.resolve("wire_cutter.png")),
                Files.readAllBytes(CC.resolve("wire_cutter.png")));
        assertArrayEquals(
                Files.readAllBytes(GT6.resolve("wire_cutter_overlay.png")),
                Files.readAllBytes(CC.resolve("wire_cutter_overlay.png")));
        assertFalse(
                java.util.Arrays.equals(
                        Files.readAllBytes(CC.resolve("wrench.png")),
                        Files.readAllBytes(CC.resolve("wire_cutter.png"))),
                "handheld wrench and wire cutter must not share one sheet");
        JsonObject manifest = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/cruciblecraft/"
                        + "gt6_handheld_tool_art_manifest.json")))
                .getAsJsonObject();
        assertEquals(3, manifest.getAsJsonArray("imports").size());
        assertEquals(
                "gt6_referencable_port_code/gregtech6_w",
                manifest.get("source").getAsString());
    }

    private static void assumeGt6() {
        assertTrue(Files.isDirectory(GT6), GT6.toString());
    }
}
