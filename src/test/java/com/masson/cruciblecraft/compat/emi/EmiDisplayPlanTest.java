package com.masson.cruciblecraft.compat.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;

/**
 * Pins the dust-family projection: exactly the materials with all three dust
 * forms registered, no cross-material mixes, one family per material.
 * (Material prefixes themselves are covered by the catalog tests.)
 */
class EmiDisplayPlanTest {
    @Test
    void dustFamiliesCoverExactlyTheTripleDustMaterials(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        Map<String, List<com.masson.cruciblecraft.api.material.MaterialPrefix>> registered =
                MaterialRegistrationGate.load(materials);

        List<EmiDisplayPlan.DustFamily> families =
                EmiDisplayPlan.dustFamilies(materials, registered);

        long expected = materials.stream()
                .filter(material -> registered.get(material.id()) != null
                        && registered.get(material.id())
                                .contains(MaterialPrefixes.DUST)
                        && registered.get(material.id())
                                .contains(MaterialPrefixes.SMALL_DUST)
                        && registered.get(material.id())
                                .contains(MaterialPrefixes.TINY_DUST))
                .count();
        assertEquals(expected, families.size());
        assertTrue(families.size() > 0, "the projection must not be empty");

        Set<String> materialIds = new HashSet<>();
        Set<String> itemIds = new HashSet<>();
        for (EmiDisplayPlan.DustFamily family : families) {
            assertTrue(materialIds.add(family.materialId()),
                    "duplicate family for " + family.materialId());
            var material = materials.stream()
                    .filter(candidate -> candidate.id()
                            .equals(family.materialId()))
                    .findFirst()
                    .orElseThrow();
            String expectedDust = material.formItems().getOrDefault(
                    MaterialPrefixes.DUST,
                    "cruciblecraft:" + material.registryName(
                            MaterialPrefixes.DUST));
            String expectedSmall = material.formItems().getOrDefault(
                    MaterialPrefixes.SMALL_DUST,
                    "cruciblecraft:" + material.registryName(
                            MaterialPrefixes.SMALL_DUST));
            String expectedTiny = material.formItems().getOrDefault(
                    MaterialPrefixes.TINY_DUST,
                    "cruciblecraft:" + material.registryName(
                            MaterialPrefixes.TINY_DUST));
            assertEquals(expectedDust, family.dust());
            assertEquals(expectedSmall, family.smallDust());
            assertEquals(expectedTiny, family.tinyDust());
            itemIds.add(family.dust());
            itemIds.add(family.smallDust());
            itemIds.add(family.tinyDust());
        }
        // No item is shared across families.
        assertEquals(families.size() * 3L, itemIds.size());
    }
}
