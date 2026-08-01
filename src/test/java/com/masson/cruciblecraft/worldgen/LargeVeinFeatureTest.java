package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class LargeVeinFeatureTest {
    private static final int COPPER_SALT = 1129664594;
    private static final int TIN_SALT = 1414090289;
    private static final int IRON_SALT = 1229737806;
    private static final int GOLD_SALT = 1196379204;
    private static final int TUNGSTEN_SALT = 1414876756;

    @Test
    void anchorIsStableAndInsideRegion() {
        var first = LargeVeinLayout.anchor(123456789L, -3, 7, 8, TIN_SALT);
        var second = LargeVeinLayout.anchor(123456789L, -3, 7, 8, TIN_SALT);
        assertEquals(first, second);
        assertTrue(first.x() >= -24 && first.x() < -16);
        assertTrue(first.z() >= 56 && first.z() < 64);
        assertNotEquals(first, LargeVeinLayout.anchor(987654321L, -3, 7, 8, TIN_SALT));
    }

    @Test
    void familySaltsSeparateAnchorsAndCenters() {
        long worldSeed = 0x1234_5678_9ABCDEFL;
        var copperAnchor = LargeVeinLayout.anchor(worldSeed, 4, -6, 7, COPPER_SALT);
        var ironAnchor = LargeVeinLayout.anchor(worldSeed, 4, -6, 7, IRON_SALT);
        assertNotEquals(copperAnchor, ironAnchor);

        var centers = Set.of(
                LargeVeinLayout.center(worldSeed, 4, -6, 7, COPPER_SALT, -64, 96),
                LargeVeinLayout.center(worldSeed, 4, -6, 8, TIN_SALT, -64, 96),
                LargeVeinLayout.center(worldSeed, 4, -6, 7, IRON_SALT, -64, 96),
                LargeVeinLayout.center(worldSeed, 4, -6, 10, GOLD_SALT, -64, 96),
                LargeVeinLayout.center(worldSeed, 4, -6, 12, TUNGSTEN_SALT, -64, 96));
        assertEquals(5, centers.size());
        assertNotEquals(
                LargeVeinLayout.generationRoll(worldSeed, 4, -6, COPPER_SALT),
                LargeVeinLayout.generationRoll(worldSeed, 4, -6, IRON_SALT));
    }

    @Test
    void roleSelectionRespectsLayerAndPeriphery() {
        assertEquals("spread", LargeVeinLayout.role(-0.8, 0.9, 1L));
        assertEquals("top", LargeVeinLayout.role(0.8, 0.1, 1L));
        assertEquals("bottom", LargeVeinLayout.role(-0.8, 0.1, 1L));
    }
}
