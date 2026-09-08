package com.masson.cruciblecraft.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class NuclearFissionCatalogTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path RODS = ROOT.resolve(
            "src/main/resources/data/cruciblecraft/nuclear_reactor_rods.json");
    private static final Path MATRIX = ROOT.resolve(
            "tools/waves/runtime/fission-survival/d0_nuclear_source_matrix.json");
    private static final Path RECIPES = ROOT.resolve(
            "src/main/resources/data/cruciblecraft/recipe");
    private static final String WAVE_NS =
            "src/main/resources/data/cruciblecraft_wave_runtime_fission_survival";

    @Test
    void catalogLocksFortySixRodsAndEightKinds() throws Exception {
        JsonObject document = parse(RODS);
        JsonArray rods = document.getAsJsonArray("rods");
        assertEquals(46, rods.size());
        Map<String, Integer> kinds = new HashMap<>();
        Set<String> ids = new HashSet<>();
        JsonObject uranium = null;
        for (JsonElement element : rods) {
            JsonObject rod = element.getAsJsonObject();
            String id = rod.get("id").getAsString();
            assertTrue(ids.add(id), "duplicate rod " + id);
            kinds.merge(rod.get("kind").getAsString(), 1, Integer::sum);
            if ("uranium238_fuel_rod".equals(id)) {
                uranium = rod;
            }
            if ("nuclear".equals(rod.get("kind").getAsString())) {
                assertTrue(rod.has("depleted"), id + " missing depleted target");
                assertTrue(rod.has("durability"), id + " missing durability");
                assertTrue(rod.has("neutron_self"), id + " missing neutron_self");
            }
            if ("breeder".equals(rod.get("kind").getAsString())) {
                assertTrue(rod.has("product"), id + " missing product target");
                assertTrue(rod.has("durability"), id + " missing durability");
                assertTrue(rod.has("neutron_loss"), id + " missing neutron_loss");
            }
        }
        assertEquals(8, kinds.size());
        assertEquals(1, kinds.get("empty"));
        assertEquals(1, kinds.get("absorber"));
        assertEquals(1, kinds.get("reflector"));
        assertEquals(1, kinds.get("moderator"));
        assertEquals(17, kinds.get("nuclear"));
        assertEquals(17, kinds.get("depleted"));
        assertEquals(4, kinds.get("breeder"));
        assertEquals(4, kinds.get("product"));
        assertTrue(uranium != null);
        assertEquals(6_000_000_000L, uranium.get("durability").getAsLong());
        assertEquals(4, uranium.get("neutron_self").getAsInt());
        assertEquals(4, uranium.get("neutron_other").getAsInt());
        assertEquals(16, uranium.get("neutron_div").getAsInt());
        assertEquals(512, uranium.get("neutron_max").getAsInt());
        assertEquals(
                "uranium238_depleted_rod",
                uranium.get("depleted").getAsString());
    }

    @Test
    void sourceMatrixIsExactFortyEightAndRecipesExist() throws Exception {
        JsonObject matrix = parse(MATRIX);
        assertEquals(48, matrix.get("nuclear_source_relations").getAsInt());
        assertEquals(0, matrix.get("missing").getAsInt());
        assertEquals(0, matrix.get("extra").getAsInt());
        assertEquals(0, matrix.get("duplicate").getAsInt());
        JsonArray relations = matrix.getAsJsonArray("relations");
        assertEquals(48, relations.size());
        Set<String> recipeIds = new HashSet<>();
        for (JsonElement element : relations) {
            JsonObject row = element.getAsJsonObject();
            assertEquals("ready", row.get("status").getAsString());
            String recipeId = row.get("recipe_id").getAsString();
            assertTrue(recipeIds.add(recipeId), "duplicate " + recipeId);
            Path recipe = RECIPES.resolve(recipeId + ".json");
            assertTrue(Files.isRegularFile(recipe), "missing " + recipe);
        }
    }

    @Test
    void catalogAndGameTestTemplateAreBundled() {
        assertTrue(Files.isRegularFile(RODS));
        assertTrue(Files.isRegularFile(MATRIX));
        assertTrue(Files.isRegularFile(Path.of(WAVE_NS + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                WAVE_NS + "/gametest/structure/empty.nbt")));
    }

    private static JsonObject parse(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
