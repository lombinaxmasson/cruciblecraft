package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.client.color.MachineBlockColor;

class Gt6VisualFixResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");

    @Test
    void lensUsesGt6MetallicIconsetNotGlass() throws Exception {
        Path prefix = Path.of(
                "src/main/resources/data/cruciblecraft/material_prefixes/lens.json");
        var json = JsonParser.parseString(Files.readString(prefix)).getAsJsonObject();
        assertEquals(
                "cruciblecraft:item/material/lens",
                json.get("model_texture").getAsString());
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/item/material/lens.png")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/item/material/lens_overlay.png")));
        String manifest = Files.readString(ROOT.resolve(
                "assets/cruciblecraft/gt6_misc_prefix_art_manifest.json"));
        assertTrue(manifest.contains("materialicons/metallic/lens.png"));
        assertTrue(manifest.contains("materialicons/metallic/lens_overlay.png"));
    }

    @Test
    void bushModelIsDualLayerAndCrusherTintsFollowCatalog() throws Exception {
        String bush = Files.readString(ROOT.resolve(
                "assets/cruciblecraft/models/block/plant/gt_bush_cube_bare.json"));
        assertTrue(bush.contains("overlay/bush"));
        assertTrue(bush.contains("tintindex"));
        assertEquals("bronze", MachineBlockColor.casingMaterialId("bronze_crusher"));
        assertEquals(
                "tungstensteel",
                MachineBlockColor.casingMaterialId("tungstensteel_crusher"));
    }
}
