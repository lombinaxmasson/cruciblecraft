package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;
import com.masson.cruciblecraft.registry.ModBlocks.OreBlockKey;

class OreHostVariantCatalogTest {
    private static final List<String> LAYERS =
            List.of("top", "bottom", "between", "spread");

    @Test
    void everyDynamicallyRegisteredOreIdResolvesToBothHosts() {
        List<String> materials =
                List.of("copper", "tungsten", "future_material");
        Map<OreBlockKey, String> registrations = new LinkedHashMap<>();
        for (String material : materials) {
            registrations.put(new OreBlockKey(material, Host.STONE), material + "_ore");
            registrations.put(
                    new OreBlockKey(material, Host.DEEPSLATE),
                    "deepslate_" + material + "_ore");
        }
        assertEquals(
                materials.size() * 2,
                OreHostVariantCatalog.registeredPaths(registrations).size());

        for (String material : materials) {
            String stone = material + "_ore";
            String deepslate = "deepslate_" + material + "_ore";
            assertTrue(OreHostVariantCatalog.registeredPaths(registrations).contains(stone));
            assertTrue(OreHostVariantCatalog.registeredPaths(registrations).contains(deepslate));
            assertEquals(stone, OreHostVariantCatalog
                    .adaptPath(stone, Host.STONE, registrations).orElseThrow());
            assertEquals(deepslate, OreHostVariantCatalog
                    .adaptPath(stone, Host.DEEPSLATE, registrations).orElseThrow());
            assertEquals(stone, OreHostVariantCatalog
                    .adaptPath(deepslate, Host.STONE, registrations).orElseThrow());
            assertEquals(deepslate, OreHostVariantCatalog
                    .adaptPath(deepslate, Host.DEEPSLATE, registrations).orElseThrow());
        }
    }

    @Test
    void unknownPathsAreNotGuessed() {
        Map<OreBlockKey, String> registrations = Map.of(
                new OreBlockKey("known", Host.STONE), "known_ore",
                new OreBlockKey("known", Host.DEEPSLATE), "deepslate_known_ore");
        assertTrue(OreHostVariantCatalog
                .adaptPath("future_ore", Host.STONE, registrations).isEmpty());
    }

    @Test
    void generatedVeinStatesMatchSemanticMaterialsAndDynamicCatalogFixture()
            throws Exception {
        Path authorRoot = Path.of(
                "src/main/resources/data/cruciblecraft/veins");
        Path configuredRoot = Path.of(
                "src/worldgen_generated/resources/data/cruciblecraft/"
                        + "worldgen/configured_feature");
        Map<String, JsonObject> authors = new LinkedHashMap<>();
        Set<String> materials = new HashSet<>();
        try (var paths = Files.list(authorRoot)) {
            for (Path path : paths.sorted().toList()) {
                JsonObject author = JsonParser.parseString(Files.readString(path))
                        .getAsJsonObject();
                authors.put(author.get("id").getAsString(), author);
                for (String layer : LAYERS) {
                    author.getAsJsonArray(layer).forEach(entry -> materials.add(
                            entry.getAsJsonObject().get("material").getAsString()));
                }
            }
        }

        Map<OreBlockKey, String> registrations = new LinkedHashMap<>();
        for (String material : materials) {
            registrations.put(
                    new OreBlockKey(material, Host.STONE),
                    material + "_ore");
            registrations.put(
                    new OreBlockKey(material, Host.DEEPSLATE),
                    "deepslate_" + material + "_ore");
        }

        for (var authorEntry : authors.entrySet()) {
            String veinId = authorEntry.getKey();
            JsonObject author = authorEntry.getValue();
            JsonObject config = JsonParser.parseString(Files.readString(
                            configuredRoot.resolve(veinId + ".json")))
                    .getAsJsonObject()
                    .getAsJsonObject("config");
            for (String layer : LAYERS) {
                var semanticEntries = author.getAsJsonArray(layer);
                var runtimeEntries = config.getAsJsonArray(layer);
                assertEquals(semanticEntries.size(), runtimeEntries.size(), veinId);
                for (int index = 0; index < semanticEntries.size(); index++) {
                    JsonObject semantic = semanticEntries.get(index).getAsJsonObject();
                    JsonObject runtime = runtimeEntries.get(index).getAsJsonObject();
                    String material = semantic.get("material").getAsString();
                    String stonePath = material + "_ore";
                    String runtimeId = runtime
                            .getAsJsonObject("state")
                            .get("Name")
                            .getAsString();
                    assertEquals("cruciblecraft:" + stonePath, runtimeId);
                    assertEquals(
                            semantic.get("weight").getAsInt(),
                            runtime.get("weight").getAsInt());
                    assertTrue(
                            OreHostVariantCatalog.registeredPaths(registrations)
                                    .contains(stonePath),
                            runtimeId);
                    assertEquals(
                            stonePath,
                            OreHostVariantCatalog
                                    .adaptPath(stonePath, Host.STONE, registrations)
                                    .orElseThrow());
                    assertEquals(
                            "deepslate_" + stonePath,
                            OreHostVariantCatalog
                                    .adaptPath(stonePath, Host.DEEPSLATE, registrations)
                                    .orElseThrow());
                }
            }
        }
    }
}
