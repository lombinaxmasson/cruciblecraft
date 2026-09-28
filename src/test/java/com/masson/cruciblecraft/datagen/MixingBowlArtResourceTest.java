package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class MixingBowlArtResourceTest {
    private static final Path CC = Path.of(
            "src/main/resources/assets/cruciblecraft");

    @Test
    void committedClayBowlAndMixingBowlArtIsManifested() throws Exception {
        JsonObject manifest = JsonParser.parseString(Files.readString(CC.resolve(
                "gt6_mixing_bowl_art_manifest.json")))
                .getAsJsonObject();
        assertEquals(9, manifest.getAsJsonArray("imports").size());
        assertEquals(
                "gt6_referencable_port_code/gregtech6_w",
                manifest.get("source").getAsString());
        assertTrue(Files.isRegularFile(CC.resolve(
                "textures/item/gt6_import/raw_ceramic_bowl.png")));
        for (String face : java.util.List.of(
                "bottom", "insides", "sides", "top")) {
            assertTrue(Files.isRegularFile(CC.resolve(
                    "textures/block/gt6_import/mixing_bowl/colored/"
                            + face + ".png")), face);
            assertTrue(Files.isRegularFile(CC.resolve(
                    "textures/block/gt6_import/mixing_bowl/overlay/"
                            + face + ".png")), face);
        }
    }
}
