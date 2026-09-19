package com.masson.cruciblecraft.content.item.tool;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;

class HammerCrushTest {
    @Test
    void onlyOrePrefixesConvert() {
        assertTrue(HammerCrush.isOrePrefix(MaterialPrefixes.ORE));
        assertTrue(HammerCrush.isOrePrefix(MaterialPrefixes.RAW_ORE));
        assertFalse(HammerCrush.isOrePrefix(MaterialPrefixes.CRUSHED_ORE));
        assertFalse(HammerCrush.isOrePrefix(MaterialPrefixes.TINY_CRUSHED_ORE));
        assertFalse(HammerCrush.isOrePrefix(MaterialPrefixes.DUST));
        assertFalse(HammerCrush.isOrePrefix(MaterialPrefixes.INGOT));
    }
}
