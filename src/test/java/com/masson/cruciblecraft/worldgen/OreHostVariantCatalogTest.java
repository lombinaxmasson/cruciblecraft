package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

class OreHostVariantCatalogTest {
    @Test
    void allFourteenOreIdsResolveToBothHosts() {
        List<String> materials =
                List.of("copper", "tin", "iron", "gold", "zinc", "lead", "nickel");
        assertEquals(14, OreHostVariantCatalog.registeredPaths().size());

        for (String material : materials) {
            String stone = material + "_ore";
            String deepslate = "deepslate_" + material + "_ore";
            assertTrue(OreHostVariantCatalog.registeredPaths().contains(stone));
            assertTrue(OreHostVariantCatalog.registeredPaths().contains(deepslate));
            assertEquals(stone, OreHostVariantCatalog.adaptPath(stone, Host.STONE).orElseThrow());
            assertEquals(deepslate, OreHostVariantCatalog.adaptPath(stone, Host.DEEPSLATE).orElseThrow());
            assertEquals(stone, OreHostVariantCatalog.adaptPath(deepslate, Host.STONE).orElseThrow());
            assertEquals(deepslate, OreHostVariantCatalog.adaptPath(deepslate, Host.DEEPSLATE).orElseThrow());
        }
    }

    @Test
    void unknownPathsAreNotGuessed() {
        assertTrue(OreHostVariantCatalog.adaptPath("future_ore", Host.STONE).isEmpty());
    }
}
