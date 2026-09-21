package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class MixingBowlArtResourceTest {
    private static final Path GT6 = Path.of(
            "gt6_referencable_port_code/gregtech6_w/src/main/resources/assets/gregtech");
    private static final Path CC = Path.of(
            "src/main/resources/assets/cruciblecraft");

    @Test
    void clayBowlAndMixingBowlMatchGt6Sheets() throws Exception {
        assertTrue(Files.isDirectory(GT6), GT6.toString());
        assertArrayEquals(
                Files.readAllBytes(GT6.resolve(
                        "textures/items/gt.multiitem.randomtools/995.png")),
                Files.readAllBytes(CC.resolve(
                        "textures/item/gt6_import/raw_ceramic_bowl.png")));
        for (String face : java.util.List.of(
                "bottom", "insides", "sides", "top")) {
            assertArrayEquals(
                    Files.readAllBytes(GT6.resolve(
                            "textures/blocks/machines/tools/mixing_bowl/colored/"
                                    + face + ".png")),
                    Files.readAllBytes(CC.resolve(
                            "textures/block/gt6_import/mixing_bowl/colored/"
                                    + face + ".png")),
                    face);
            assertArrayEquals(
                    Files.readAllBytes(GT6.resolve(
                            "textures/blocks/machines/tools/mixing_bowl/overlay/"
                                    + face + ".png")),
                    Files.readAllBytes(CC.resolve(
                            "textures/block/gt6_import/mixing_bowl/overlay/"
                                    + face + ".png")),
                    face);
        }
        JsonObject manifest = JsonParser.parseString(Files.readString(CC.resolve(
                "gt6_mixing_bowl_art_manifest.json")))
                .getAsJsonObject();
        assertEquals(9, manifest.getAsJsonArray("imports").size());
        assertEquals(
                "gt6_referencable_port_code/gregtech6_w",
                manifest.get("source").getAsString());
    }
}
