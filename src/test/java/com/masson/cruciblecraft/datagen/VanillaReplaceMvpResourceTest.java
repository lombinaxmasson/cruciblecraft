package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class VanillaReplaceMvpResourceTest {
    private static final Path ROOT = Path.of(".");
    private static final Path PAPER = Path.of(
            "src/main/resources/data/minecraft/recipe/paper.json");
    private static final Path FURNACE = Path.of(
            "src/main/resources/data/minecraft/recipe/furnace.json");
    private static final Path BONE_MEAL = Path.of(
            "src/main/resources/data/minecraft/recipe/bone_meal.json");
    private static final Path TAG = Path.of(
            "src/main/resources/data/cruciblecraft/tags/item/"
                    + "crafting_firestarter.json");
    private static final Path LOCK = Path.of(
            "tools/waves/content/vanilla-replace-mvp/"
                    + "vanilla_replace_lock.json");
    private static final Set<String> OVERLAYS = Set.of(
            "paper.json",
            "furnace.json",
            "bone_meal.json",
            "magma_cream.json",
            "bucket.json",
            "anvil.json",
            "heavy_weighted_pressure_plate.json",
            "light_weighted_pressure_plate.json",
            "compass.json",
            "iron_door.json",
            "cauldron.json",
            "hopper.json",
            "iron_bars.json",
            "clock.json",
            "lever.json",
            "repeater.json",
            "comparator.json",
            "piston.json",
            "sticky_piston.json",
            "dropper.json",
            "dispenser.json",
            "lead.json",
            "minecart.json",
            "chest_minecart.json",
            "furnace_minecart.json",
            "hopper_minecart.json",
            "tnt_minecart.json",
            "chainmail_helmet.json",
            "chainmail_chestplate.json",
            "chainmail_leggings.json",
            "chainmail_boots.json",
            "cookie.json",
            "golden_apple.json",
            "golden_carrot.json");

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
    void furnaceRequiresFirestarter() throws Exception {
        JsonObject furnace = object(FURNACE);
        assertEquals(
                "minecraft:crafting_shaped",
                furnace.get("type").getAsString());
        assertEquals(
                "cruciblecraft:crafting_firestarter",
                furnace.getAsJsonObject("key")
                        .getAsJsonObject("F")
                        .get("tag")
                        .getAsString());
        assertEquals(
                "XXX",
                furnace.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(
                "XFX",
                furnace.getAsJsonArray("pattern").get(1).getAsString());
    }

    @Test
    void boneMealIsHammerCatalyst() throws Exception {
        JsonObject boneMeal = object(BONE_MEAL);
        assertEquals(
                "cruciblecraft:shaped_catalyst",
                boneMeal.get("type").getAsString());
        assertEquals(
                1,
                boneMeal.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(
                "h  ",
                boneMeal.getAsJsonArray("pattern").get(0).getAsString());
    }

    @Test
    void firestarterTagMatchesOdCraftingFirestarterMembers() throws Exception {
        JsonObject tag = object(TAG);
        assertFalse(tag.get("replace").getAsBoolean());
        var values = tag.getAsJsonArray("values");
        assertEquals(4, values.size());
        assertEquals("minecraft:flint_and_steel", values.get(0).getAsString());
        assertEquals("minecraft:fire_charge", values.get(1).getAsString());
        assertEquals("cruciblecraft:match", values.get(2).getAsString());
        assertEquals(
                "cruciblecraft:material_flint_and_tinder",
                values.get(3).getAsString());
    }

    @Test
    void lockRecordsOpeningThroughRedstone() throws Exception {
        JsonObject lock = object(LOCK);
        assertEquals("VANILLA_REPLACE_MVP_READY", lock.get("status").getAsString());
        assertEquals(4, lock.getAsJsonArray("removed").size());
        assertTrue(containsRecipe(lock.getAsJsonArray("removed"), "minecraft:magma_cream"));
        assertTrue(containsRecipe(lock.getAsJsonArray("removed"), "minecraft:cookie"));
        assertTrue(containsRecipe(lock.getAsJsonArray("removed"), "minecraft:golden_apple"));
        assertTrue(containsRecipe(lock.getAsJsonArray("removed"), "minecraft:golden_carrot"));
        JsonArray substituted = lock.getAsJsonArray("substituted");
        assertTrue(substituted.size() >= 30);
        assertTrue(containsRecipe(substituted, "minecraft:paper"));
        assertTrue(containsRecipe(substituted, "minecraft:furnace"));
        assertTrue(containsRecipe(substituted, "minecraft:bone_meal"));
        assertTrue(containsRecipe(substituted, "minecraft:piston"));
        assertTrue(containsRecipe(substituted, "minecraft:lead"));
        assertTrue(containsRecipe(substituted, "minecraft:minecart"));
        assertTrue(containsRecipe(substituted, "minecraft:chainmail_helmet"));
        String deferred = lock.getAsJsonArray("deferred").toString();
        assertFalse(deferred.contains("\"minecraft:furnace\""));
        assertFalse(deferred.contains("\"minecraft:bone_meal\""));
        assertTrue(lock.getAsJsonArray("added").size() >= 1);
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
            var names = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
            assertTrue(names.contains("paper.json"));
            for (String name : names) {
                if (OVERLAYS.contains(name) || name.endsWith("_planks.json")) {
                    continue;
                }
                throw new AssertionError("unexpected minecraft overlay " + name);
            }
        }
    }

    private static boolean containsRecipe(JsonArray rows, String recipeId) {
        for (int index = 0; index < rows.size(); index++) {
            if (recipeId.equals(
                    rows.get(index)
                            .getAsJsonObject()
                            .get("recipe_id")
                            .getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject object(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
