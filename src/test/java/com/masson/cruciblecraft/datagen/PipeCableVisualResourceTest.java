package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void gt6PipeAndCableTexturesArePresent() {
        Path copper = ASSETS.resolve("gt6_import/materialicons/copper");
        Path iconsets = ASSETS.resolve("gt6_import/iconsets");
        assertTrue(Files.isRegularFile(copper.resolve("pipeside.png")));
        assertTrue(Files.isRegularFile(copper.resolve("pipeside_overlay.png")));
        assertTrue(Files.isRegularFile(copper.resolve("wire.png")));
        assertTrue(Files.isRegularFile(copper.resolve("wire_overlay.png")));
        assertTrue(Files.isRegularFile(iconsets.resolve("insulation_tiny.png")));
        assertTrue(Files.isRegularFile(iconsets.resolve("insulation_full.png")));
        assertTrue(Files.isRegularFile(iconsets.resolve("pipe_restrictor.png")));
        assertFalse(Files.isRegularFile(copper.resolve("cable.png")));
        assertFalse(Files.isRegularFile(iconsets.resolve("cable.png")));
    }

    @Test
    void generatedConductorModelsUseSharedGt6Iconsets() throws Exception {
        JsonObject core = json(GENERATED.resolve("conductor/cablegt01_core.json"));
        JsonObject textures = core.getAsJsonObject("textures");
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/wire",
                textures.get("side").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6_import/iconsets/insulation_full",
                textures.get("end").getAsString());
        assertFalse(textures.has("overlay"));
        assertEquals(
                "#end",
                core.getAsJsonArray("elements")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("faces")
                        .getAsJsonObject("north")
                        .get("texture")
                        .getAsString());
        JsonObject item = json(GENERATED.resolve("conductor/cablegt01_item.json"));
        assertEquals(3, item.getAsJsonArray("elements").size());
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/wire_overlay",
                item.getAsJsonObject("textures").get("overlay").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6_import/iconsets/insulation_tiny",
                item.getAsJsonObject("textures").get("insulation").getAsString());
        JsonObject itemSide = item.getAsJsonArray("elements")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("faces")
                .getAsJsonObject("up");
        assertEquals("#end", itemSide.get("texture").getAsString());
        assertEquals(1, itemSide.get("tintindex").getAsInt());
        JsonObject itemWireOverlay = item.getAsJsonArray("elements")
                .get(1)
                .getAsJsonObject()
                .getAsJsonObject("faces")
                .getAsJsonObject("north");
        assertEquals("#overlay", itemWireOverlay.get("texture").getAsString());
        assertFalse(itemWireOverlay.has("tintindex"));
        JsonObject itemJacket = item.getAsJsonArray("elements")
                .get(2)
                .getAsJsonObject()
                .getAsJsonObject("faces")
                .getAsJsonObject("north");
        assertEquals("#insulation", itemJacket.get("texture").getAsString());
        assertEquals(1, itemJacket.get("tintindex").getAsInt());
        JsonObject wire = json(GENERATED.resolve("conductor/wiregt01_core.json"));
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/wire",
                wire.getAsJsonObject("textures").get("side").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/wire_overlay",
                wire.getAsJsonObject("textures").get("overlay").getAsString());
        JsonObject pipe = json(GENERATED.resolve("pipe/fluid_4_core.json"));
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/pipeside",
                pipe.getAsJsonObject("textures").get("side").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/pipeside_overlay",
                pipe.getAsJsonObject("textures").get("side_overlay").getAsString());
        JsonObject pipeArm = json(GENERATED.resolve("pipe/fluid_4_arm.json"));
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/pipeside",
                pipeArm.getAsJsonObject("textures").get("side").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/pipetiny",
                pipeArm.getAsJsonObject("textures").get("end").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6_import/materialicons/copper/pipetiny_overlay",
                pipeArm.getAsJsonObject("textures").get("end_overlay").getAsString());
        JsonObject armSide = pipeArm.getAsJsonArray("elements")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("faces")
                .getAsJsonObject("up");
        assertEquals("#side", armSide.get("texture").getAsString());
        JsonObject armCap = pipeArm.getAsJsonArray("elements")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("faces")
                .getAsJsonObject("north");
        assertEquals("#end", armCap.get("texture").getAsString());
    }

    @Test
    void retiredNumberedWireItemsRemainUnregistered() {
        Path models = Path.of(
                "src/main/resources/assets/cruciblecraft/models/item/electric_wire");
        for (String material : java.util.List.of("gold", "lead")) {
            for (int strands : java.util.List.of(3, 5, 6, 7, 9, 10, 11, 13, 14, 15)) {
                assertFalse(Files.exists(models.resolve(
                        strands + "x_" + material + "_wire.json")));
            }
        }
    }

    private static JsonObject json(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), path.toString());
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
