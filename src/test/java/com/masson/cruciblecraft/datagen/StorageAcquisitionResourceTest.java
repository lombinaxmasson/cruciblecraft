package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.mte.MteInPlaceAcquisitionCatalog;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;

class StorageAcquisitionResourceTest {
    private static final Path GENERATED = Path.of("src/generated/resources");

    @Test
    void catalogRowsHaveAssetsAndVisibleRowsHaveRecipes() throws Exception {
        var english = document("assets/cruciblecraft/lang/en_us.json");
        var chinese = document("assets/cruciblecraft/lang/zh_cn.json");
        Set<String> pickaxe = values("data/minecraft/tags/block/mineable/pickaxe.json");
        Set<String> axe = values("data/minecraft/tags/block/mineable/axe.json");
        Set<String> foldedCatalystRecipes = MteInPlaceAcquisitionCatalog.recipes()
                .stream()
                .map(recipe -> recipe.path() + ".json")
                .filter(path -> path.startsWith("storage/"))
                .collect(Collectors.toUnmodifiableSet());
        int recipes = 0;
        int foldedRecipes = 0;
        for (StorageVariant variant : StorageVariantCatalog.variants()) {
            String path = variant.path();
            String key = "block.cruciblecraft." + path;
            assertTrue(english.has(key), key);
            assertEquals(variant.english(), english.get(key).getAsString(), key);
            assertEquals(variant.chinese(), chinese.get(key).getAsString(), key);
            assertTrue(Files.exists(GENERATED.resolve(
                    "assets/cruciblecraft/blockstates/" + path + ".json")), path);
            assertTrue(Files.exists(GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + path + ".json")), path);
            assertTrue(Files.exists(GENERATED.resolve(
                    "data/cruciblecraft/loot_table/blocks/" + path + ".json")), path);
            Path recipe = GENERATED.resolve(
                    "data/cruciblecraft/recipe/storage/" + path + ".json");
            boolean hasDirectAcquisitionRecipe = variant.sourceVisible()
                    && !"folded_gt6_catalyst".equals(
                            variant.acquisitionProfile());
            if (hasDirectAcquisitionRecipe) {
                assertTrue(Files.exists(recipe), path);
                recipes++;
            } else if (foldedCatalystRecipes.contains("storage/" + path + ".json")) {
                assertTrue(Files.exists(recipe), path);
                foldedRecipes++;
            } else {
                assertFalse(Files.exists(recipe), path);
            }
            String id = variant.id().toString();
            assertTrue(pickaxe.contains(id) || axe.contains(id), id);
        }
        assertEquals(
                StorageVariantCatalog.sourceVisible().stream()
                        .filter(variant -> !"folded_gt6_catalyst".equals(
                                variant.acquisitionProfile()))
                        .count(),
                recipes);
        assertEquals(foldedCatalystRecipes.size(), foldedRecipes);
        assertEquals(625, StorageVariantCatalog.variants().size());
    }

    private static JsonObject document(String relative) throws Exception {
        return JsonParser.parseString(
                Files.readString(GENERATED.resolve(relative))).getAsJsonObject();
    }

    private static Set<String> values(String relative) throws Exception {
        JsonArray array = document(relative).getAsJsonArray("values");
        return StreamSupport.stream(array.spliterator(), false)
                .map(element -> element.getAsString())
                .collect(Collectors.toSet());
    }
}
