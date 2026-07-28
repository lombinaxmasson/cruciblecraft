package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialForm;

class OreResourceTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final List<String> MATERIALS =
            List.of("copper", "tin", "iron", "gold", "zinc", "lead", "nickel");

    @Test
    void everyOreHasRegistrationFacingResources() {
        for (String material : MATERIALS) {
            for (String prefix : List.of("", "deepslate_")) {
                String ore = prefix + material + "_ore";
                assertResource("assets/cruciblecraft/blockstates/" + ore + ".json");
                assertResource("assets/cruciblecraft/models/block/" + ore + ".json");
                assertResource("assets/cruciblecraft/models/item/" + ore + ".json");
                assertResource("data/cruciblecraft/loot_table/blocks/" + ore + ".json");
            }
            assertResource("data/c/tags/block/ores/" + material + ".json");
            assertResource("data/c/tags/item/ores/" + material + ".json");
        }
    }

    @Test
    void initialLargeVeinFamiliesAreFullyWired() {
        for (String family : List.of("copper", "tin", "iron", "gold")) {
            assertResource("data/cruciblecraft/worldgen/configured_feature/large_" + family + "_vein.json");
            assertResource("data/cruciblecraft/worldgen/placed_feature/large_" + family + "_vein.json");
        }
        assertResource("data/cruciblecraft/neoforge/biome_modifier/add_large_veins.json");
        assertResource("data/cruciblecraft/tags/block/large_vein_replaceables.json");
    }

    @Test
    void configuredFamiliesHaveDistinctRequiredSaltsAndSingleOriginPlacement() throws Exception {
        var salts = new HashSet<Integer>();
        Pattern saltPattern = Pattern.compile("\"salt\"\\s*:\\s*(-?\\d+)");
        Pattern emptyPlacement = Pattern.compile("\"placement\"\\s*:\\s*\\[\\s*]");
        for (String family : List.of("copper", "tin", "iron", "gold")) {
            String configured = Files.readString(RESOURCES.resolve(
                    "data/cruciblecraft/worldgen/configured_feature/large_" + family + "_vein.json"));
            var matcher = saltPattern.matcher(configured);
            assertTrue(matcher.find(), family + " salt");
            assertTrue(salts.add(Integer.parseInt(matcher.group(1))), family + " duplicate salt");

            String placed = Files.readString(RESOURCES.resolve(
                    "data/cruciblecraft/worldgen/placed_feature/large_" + family + "_vein.json"));
            assertTrue(emptyPlacement.matcher(placed).find(), family + " placement");
        }
        assertEquals(4, salts.size());
    }

    @Test
    void oreCookingRecipesPreserveOneIngotEconomyWithoutVanillaDuplicates() throws Exception {
        assertEquals(144, MaterialForm.RAW_ORE.units());
        assertEquals(144, MaterialForm.CRUSHED_ORE.units());

        Set<String> canonical = Set.of("copper", "iron", "gold");
        for (String material : canonical) {
            for (String process : List.of("smelting", "blasting")) {
                Path duplicate = RESOURCES.resolve(
                        "data/cruciblecraft/recipe/" + material + "_raw_ore_" + process + ".json");
                assertTrue(Files.notExists(duplicate), duplicate.toString());
            }
        }

        Pattern oneResult = Pattern.compile("\"result\"\\s*:\\s*\\{[^}]*\"count\"\\s*:\\s*1\\b",
                Pattern.DOTALL);
        Path recipes = RESOURCES.resolve("data/cruciblecraft/recipe");
        try (var paths = Files.list(recipes)) {
            var oreCooking = paths
                    .filter(path -> path.getFileName().toString().matches(
                            ".+_(raw|crushed)_ore_(smelting|blasting)\\.json"))
                    .toList();
            assertEquals(22, oreCooking.size());
            for (Path recipe : oreCooking) {
                assertTrue(oneResult.matcher(Files.readString(recipe)).find(), recipe.toString());
            }
        }
    }

    private static void assertResource(String relative) {
        assertTrue(Files.isRegularFile(RESOURCES.resolve(relative)), relative);
    }
}
