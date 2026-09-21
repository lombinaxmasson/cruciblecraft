package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class CoilHostIdentityTest {
    private static final Path CATALOG = Path.of(
            "src/main/resources/data/cruciblecraft/mte_inplace_catalog.json");
    private static final Path ACQUISITION = Path.of(
            "src/main/resources/data/cruciblecraft/mte_inplace_acquisition.json");
    private static final Path OVEN = Path.of(
            "src/main/resources/data/cruciblecraft/multiblock_structures/large_oven.json");
    private static final int[] COIL_METAS = {18040, 18041, 18042, 18043, 18044, 18045};

    @Test
    void sixCoilMetasAreLiveInplaceIdentities() throws Exception {
        Map<Integer, JsonObject> byMeta = catalogByMeta();
        Set<String> runtimeIds = new HashSet<>();
        for (int meta : COIL_METAS) {
            JsonObject row = byMeta.get(meta);
            assertTrue(row != null, "missing coil meta " + meta);
            assertEquals("MULTIBLOCK_PART", row.get("kind").getAsString());
            String runtime = row.get("runtime_id").getAsString();
            assertTrue(
                    runtime.startsWith("cruciblecraft:multiblock/large_"),
                    runtime);
            assertTrue(runtime.endsWith("_coil"), runtime);
            assertTrue(runtimeIds.add(runtime), "duplicate runtime " + runtime);
            String chinese = row.get("chinese_name").getAsString();
            assertTrue(
                    chinese.codePoints().anyMatch(
                            code -> Character.UnicodeScript.of(code)
                                    == Character.UnicodeScript.HAN),
                    "coil CJK missing for " + meta + ": " + chinese);
        }
        assertEquals(6, runtimeIds.size());
        assertEquals(
                "cruciblecraft:multiblock/large_niobium_titanium_coil",
                byMeta.get(18041).get("runtime_id").getAsString());
        assertEquals(
                "cruciblecraft:multiblock/large_carborundum_coil",
                byMeta.get(18043).get("runtime_id").getAsString());
        assertEquals(
                "cruciblecraft:multiblock/large_iridium_coil",
                byMeta.get(18045).get("runtime_id").getAsString());
    }

    @Test
    void coilRecipesUseEightQuadrupleWiresAndWireCutter() throws Exception {
        JsonArray recipes = JsonParser.parseReader(new InputStreamReader(
                Files.newInputStream(ACQUISITION), StandardCharsets.UTF_8))
                .getAsJsonObject()
                .getAsJsonArray("recipes");
        Set<String> paths = new HashSet<>();
        for (JsonElement element : recipes) {
            JsonObject recipe = element.getAsJsonObject();
            String path = recipe.get("path").getAsString();
            if (!path.startsWith("multiblock/large_") || !path.endsWith("_coil")) {
                continue;
            }
            paths.add(path);
            assertEquals(
                    "cruciblecraft:material_wire_cutter",
                    recipe.getAsJsonObject("catalysts")
                            .getAsJsonObject("x")
                            .get("item")
                            .getAsString());
            JsonArray pattern = recipe.getAsJsonArray("pattern");
            assertEquals("WWW", pattern.get(0).getAsString());
            assertEquals("WxW", pattern.get(1).getAsString());
            assertEquals("WWW", pattern.get(2).getAsString());
            JsonObject wire = recipe.getAsJsonObject("ingredients")
                    .getAsJsonObject("W");
            assertTrue(
                    wire.get("item").getAsString().endsWith("/quadruple_wire"),
                    path);
            assertEquals(
                    "cruciblecraft:" + path,
                    recipe.getAsJsonObject("result").get("id").getAsString());
        }
        assertEquals(
                Set.of(
                        "multiblock/large_copper_coil",
                        "multiblock/large_niobium_titanium_coil",
                        "multiblock/large_nichrome_coil",
                        "multiblock/large_carborundum_coil",
                        "multiblock/large_osmium_coil",
                        "multiblock/large_iridium_coil"),
                paths);
    }

    @Test
    void ovenCoilsUseUniformGroupXor() throws Exception {
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                Files.newInputStream(OVEN), StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonObject coils = document.getAsJsonObject("palette").getAsJsonObject("N");
        assertEquals("tag", coils.get("type").getAsString());
        assertEquals("cruciblecraft:large_oven_coils", coils.get("tag").getAsString());
        assertEquals("oven_coils", coils.get("uniform_group").getAsString());
    }

    @Test
    void fusionIridiumHasOneRuntimeId() throws Exception {
        Map<Integer, JsonObject> byMeta = catalogByMeta();
        assertEquals(
                "cruciblecraft:multiblock/large_iridium_coil",
                byMeta.get(18045).get("runtime_id").getAsString());
        assertFalse(Files.exists(Path.of(
                "src/generated/resources/data/cruciblecraft/recipe/machines/large_iridium_coil.json")));
        assertFalse(Files.exists(Path.of(
                "src/generated/resources/assets/cruciblecraft/blockstates/large_iridium_coil.json")));
    }

    @Test
    void hostKindsAreDedicatedControllers() throws Exception {
        Map<Integer, JsonObject> byMeta = catalogByMeta();
        assertEquals("MATTER_FABRICATOR", byMeta.get(17199).get("kind").getAsString());
        assertEquals("LARGE_DYNAMO", byMeta.get(17221).get("kind").getAsString());
        assertEquals("LARGE_DYNAMO", byMeta.get(17224).get("kind").getAsString());
        assertEquals("VON_DA_GRAAGG", byMeta.get(17996).get("kind").getAsString());
        assertEquals("LIGHTNING_ROD", byMeta.get(17998).get("kind").getAsString());
        assertEquals("MULTIBLOCK_PART", byMeta.get(18104).get("kind").getAsString());
    }

    private static Map<Integer, JsonObject> catalogByMeta() throws Exception {
        JsonArray identities = JsonParser.parseReader(new InputStreamReader(
                Files.newInputStream(CATALOG), StandardCharsets.UTF_8))
                .getAsJsonObject()
                .getAsJsonArray("identities");
        Map<Integer, JsonObject> byMeta = new HashMap<>();
        for (JsonElement element : identities) {
            JsonObject row = element.getAsJsonObject();
            byMeta.put(row.get("meta").getAsInt(), row);
        }
        return byMeta;
    }
}
