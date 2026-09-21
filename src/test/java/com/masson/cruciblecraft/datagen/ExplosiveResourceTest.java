package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class ExplosiveResourceTest {
    private static final Path GT6 = Path.of(
            "gt6_referencable_port_code/gregtech6_w/src/main/resources/"
                    + "assets/gregtech/textures/blocks/machines/tools/dynamite");
    private static final Path CC = Path.of(
            "src/main/resources/assets/cruciblecraft/textures/block/gt6_import/dynamite");

    @Test
    void dynamiteTexturesAreCopiedFromGt6() {
        List<String> textures = List.of(
                "colored/front.png",
                "colored/back.png",
                "colored/side.png",
                "colored_active/front.png",
                "colored_active/back.png",
                "colored_active/side.png",
                "overlay/front.png",
                "overlay/back.png",
                "overlay/side.png",
                "overlay_active/front.png",
                "overlay_active/back.png",
                "overlay_active/side.png");
        for (String texture : textures) {
            assertTrue(Files.exists(GT6.resolve(texture)), texture);
            assertTrue(Files.exists(CC.resolve(texture)), texture);
        }
    }

    @Test
    void remoteActivatorUsesTheGt6IconAndRecipeInputs() {
        Path source = Path.of(
                "gt6_referencable_port_code/gregtech6_w/src/main/resources/"
                        + "assets/gregtech/textures/items/gt.multiitem.randomtools/9000.png");
        Path destination = Path.of(
                "src/main/resources/assets/cruciblecraft/textures/item/"
                        + "gt6_import/remote_activator.png");
        assertTrue(Files.exists(source));
        assertTrue(Files.exists(destination));

        JsonObject recipe = JsonParser.parseString(read(
                Path.of("src/main/resources/data/cruciblecraft/recipe/remote_activator.json")))
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:remote_activator",
                recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(
                "cruciblecraft:chromium/plate",
                recipe.getAsJsonObject("key").getAsJsonObject("P")
                        .get("item").getAsString());
        assertEquals(
                "cruciblecraft:chromium/screw",
                recipe.getAsJsonObject("key").getAsJsonObject("T")
                        .get("item").getAsString());
        assertEquals(
                List.of("TPE", "BC ", " PT"),
                recipe.getAsJsonArray("pattern").asList().stream()
                        .map(element -> element.getAsString())
                        .toList());
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception failure) {
            throw new AssertionError(path.toString(), failure);
        }
    }
}
