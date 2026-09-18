package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.def.ThermalProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaterialLoaderTest {
    @Test
    void loadsStartupStructuralMaterialDefinitions(@TempDir Path configDirectory)
            throws Exception {
        Files.writeString(configDirectory.resolve("testium.json"), """
                {
                  "id": "testium",
                  "generation_flags": [
                    "cruciblecraft:generates_ingot",
                    "cruciblecraft:generates_dust"
                  ],
                  "exclude_prefixes": ["dust", "small_dust", "tiny_dust"],
                  "include_prefixes": ["plate"],
                  "thermal": {
                    "melting_point": 1000,
                    "boiling_point": 2000,
                    "density": 5
                  }
                }
                """);

        MaterialDefinition loaded = MaterialLoader.load(configDirectory).get("testium");

        assertEquals(
                List.of(MaterialPrefixes.INGOT, MaterialPrefixes.PLATE),
                loaded.forms());
    }

    @Test
    void removesCompositionCyclesAndTheirDependents(
            @TempDir Path configDirectory) {
        MaterialDefinition cycleA = material(
                "cycle_a",
                Map.of("cycle_b", 1));
        MaterialDefinition cycleB = material(
                "cycle_b",
                Map.of("cycle_a", 1));
        MaterialDefinition dependent = material(
                "cycle_dependent",
                Map.of("cycle_a", 1));

        Map<String, MaterialDefinition> loaded = MaterialLoader.load(
                configDirectory,
                List.of(cycleA, cycleB, dependent));

        assertFalse(loaded.containsKey("cycle_a"));
        assertFalse(loaded.containsKey("cycle_b"));
        assertFalse(loaded.containsKey("cycle_dependent"));
        assertTrue(loaded.containsKey("iron"));
    }

    @Test
    void chromiumUsesTranslationKeyAndIsNotFurnaceSmeltable(
            @TempDir Path configDirectory) {
        Map<String, MaterialDefinition> loaded = MaterialLoader.load(configDirectory);
        MaterialDefinition chromium = loaded.get("chromium");
        assertTrue(chromium.nameKey().isEmpty());
        assertEquals("material.cruciblecraft.chromium", chromium.translationKey());
        assertFalse(chromium.furnaceSmeltable());
        assertTrue(loaded.get("copper").furnaceSmeltable());
        assertFalse(loaded.get("iron").furnaceSmeltable());
    }

    private static MaterialDefinition material(
            String id,
            Map<String, Integer> composition) {
        return new MaterialDefinition(
                id,
                id,
                Optional.empty(),
                1,
                "#FFFFFF",
                "metallic",
                List.of(MaterialPrefixes.DUST),
                Map.of(),
                new ThermalProperties(1_000),
                false,
                composition,
                false);
    }
}
