package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class VanillaReplaceMvpResourceTest {
    private static final Path ROOT = Path.of(".");
    private static final Path PAPER = Path.of(
            "src/main/resources/data/minecraft/recipe/paper.json");
    private static final Path TAG = Path.of(
            "src/main/resources/data/cruciblecraft/tags/item/"
                    + "crafting_firestarter.json");
    private static final Path LOCK = Path.of(
            "tools/waves/content/vanilla-replace-mvp/"
                    + "vanilla_replace_lock.json");

    @Test
    void paperSubstituteMatchesLockIo() throws Exception {
        JsonObject paper = object(PAPER);
        assertEquals("minecraft:crafting_shaped", paper.get("type").getAsString());
        assertFalse(paper.has("neoforge:conditions"));
        JsonObject result = paper.getAsJsonObject("result");
        assertEquals("minecraft:paper", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
        assertEquals(
                "XXX",
                paper.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(
                "minecraft:sugar_cane",
                paper.getAsJsonObject("key")
                        .getAsJsonObject("X")
                        .get("item")
                        .getAsString());
    }

    @Test
    void firestarterTagMatchesOdCraftingFirestarterMembers() throws Exception {
        JsonObject tag = object(TAG);
        assertFalse(tag.get("replace").getAsBoolean());
        var values = tag.getAsJsonArray("values");
        assertEquals(3, values.size());
        assertEquals("minecraft:flint_and_steel", values.get(0).getAsString());
        assertEquals("minecraft:fire_charge", values.get(1).getAsString());
        assertEquals("cruciblecraft:match", values.get(2).getAsString());
    }

    @Test
    void lockDoesNotRemoveFurnaceOrBoneMeal() throws Exception {
        JsonObject lock = object(LOCK);
        assertEquals("VANILLA_REPLACE_MVP_READY", lock.get("status").getAsString());
        assertEquals(0, lock.getAsJsonArray("removed").size());
        assertEquals(0, lock.getAsJsonArray("added").size());
        assertEquals(1, lock.getAsJsonArray("substituted").size());
        assertEquals(
                "minecraft:paper",
                lock.getAsJsonArray("substituted")
                        .get(0)
                        .getAsJsonObject()
                        .get("recipe_id")
                        .getAsString());
        String deferred = lock.getAsJsonArray("deferred").toString();
        assertTrue(deferred.contains("minecraft:furnace"));
        assertTrue(deferred.contains("minecraft:bone_meal"));
        assertEquals(0, lock.get("owns_families").getAsInt());
        assertEquals(0, lock.get("generated_recipe_count").getAsInt());
        assertTrue(lock.get("production_lock").isJsonNull());
    }

    @Test
    void isolatedGameTestTemplatesExist() {
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_vanilla_replace_mvp";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }

    @Test
    void minecraftRecipeTreeOnlyContainsLockOwnedFiles() throws Exception {
        Path recipes = ROOT.resolve("src/main/resources/data/minecraft/recipe");
        assertTrue(Files.isRegularFile(recipes.resolve("paper.json")));
        try (var stream = Files.list(recipes)) {
            long count = stream.filter(Files::isRegularFile).count();
            assertEquals(1, count);
        }
    }

    private static JsonObject object(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
