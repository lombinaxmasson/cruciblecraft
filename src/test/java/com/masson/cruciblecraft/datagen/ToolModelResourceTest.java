package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class ToolModelResourceTest {
    private static final Path GENERATED = Path.of(
            "src/generated/resources/assets/cruciblecraft/models/item");

    @Test
    void stickHandleToolsCompositeHeadAndRod() throws Exception {
        for (String name : java.util.List.of(
                "material_pickaxe",
                "material_shovel",
                "material_axe",
                "material_hoe",
                "smithing_hammer",
                "material_soft_hammer",
                "material_spade",
                "material_double_axe",
                "material_sense",
                "material_plow",
                "material_construction_pick",
                "material_gem_pick",
                "material_builder_wand",
                "material_universal_spade")) {
            JsonObject textures = json(GENERATED.resolve(name + ".json"))
                    .getAsJsonObject("textures");
            assertEquals(
                    "cruciblecraft:item/material/rod",
                    textures.get("layer2").getAsString(),
                    name);
            assertEquals(
                    "cruciblecraft:item/material/rod_overlay",
                    textures.get("layer3").getAsString(),
                    name);
        }
        JsonObject sword = json(GENERATED.resolve("material_sword.json"))
                .getAsJsonObject("textures");
        assertEquals(
                "cruciblecraft:item/tool/handle_sword",
                sword.get("layer2").getAsString());
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/assets/cruciblecraft/textures/item/material/rod.png")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/assets/cruciblecraft/textures/item/material/rod_overlay.png")));
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
