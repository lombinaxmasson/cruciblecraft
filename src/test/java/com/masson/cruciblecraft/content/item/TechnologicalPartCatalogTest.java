package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class TechnologicalPartCatalogTest {
    @Test
    void catalogCoversT1ToT6CircuitPath() {
        Set<String> paths = TechnologicalPartCatalog.parts().stream()
                .map(TechnologicalPartCatalog.Part::registryPath)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(28, paths.size());
        assertTrue(paths.containsAll(Set.of(
                "circuit_basic",
                "circuit_good",
                "circuit_advanced",
                "circuit_elite",
                "circuit_master",
                "circuit_ultimate",
                "circuit_wire_gold",
                "circuit_plate_gold",
                "circuit_part_good",
                "circuit_part_advanced",
                "circuit_part_elite",
                "circuit_part_ultimate")));
    }
}
