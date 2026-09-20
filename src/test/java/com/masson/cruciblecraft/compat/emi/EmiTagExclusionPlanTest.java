package com.masson.cruciblecraft.compat.emi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;

class EmiTagExclusionPlanTest {
    @Test
    void clientPackHidesMaterialSpecificTagsAndNamesParentForms(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);
        JsonObject exclusions = json(clientFiles.get(
                "assets/emi/tag/exclusions/cruciblecraft.json"));
        Set<String> items = strings(exclusions, "item");
        Set<String> blocks = strings(exclusions, "block");
        assertTrue(items.contains("c:dusts/iron"));
        assertTrue(items.contains("c:dusts/copper"));
        assertTrue(items.contains("cruciblecraft:materials/iron"));
        assertFalse(items.contains("c:dusts"));
        assertFalse(items.contains("c:crushed_ores/copper"));
        assertTrue(blocks.contains("c:storage_blocks/aluminium"));
        assertFalse(blocks.contains("c:storage_blocks"));
        JsonObject english = json(clientFiles.get(
                "assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(clientFiles.get(
                "assets/cruciblecraft/lang/zh_cn.json"));
        assertTrue(english.has("tag.item.c.dusts"));
        assertTrue(chinese.has("tag.item.c.dusts"));
    }

    private static Set<String> strings(JsonObject root, String key) {
        Set<String> values = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray(key)) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
