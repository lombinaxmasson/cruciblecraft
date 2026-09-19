package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CrucibleInteriorArtResourceTest {
    @Test
    void interiorTexturesAreImportedFromGt6RoughIcons() throws Exception {
        Path root = Path.of("src/main/resources/assets/cruciblecraft");
        assertTrue(Files.isRegularFile(root.resolve(
                "textures/block/gt6_import/materialicons/rough_molten.png")));
        assertTrue(Files.isRegularFile(root.resolve(
                "textures/block/gt6_import/materialicons/rough_block_raw.png")));
        String manifest = Files.readString(root.resolve("gt6_art_manifest.json"));
        assertTrue(manifest.contains("materialicons/ROUGH/molten.png"));
        assertTrue(manifest.contains("materialicons/ROUGH/blockRaw.png"));
    }
}
