package com.masson.cruciblecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;

import org.junit.jupiter.api.Test;

class CrusherRecipeRulesTest {
    @Test
    void serializerModelRejectsUnsafeRecipeValues() {
        assertThrows(IllegalArgumentException.class, () -> CrusherRecipeRules.validate(
                MaterialPrefixes.RAW_ORE, MaterialPrefixes.CRUSHED_ORE, 0, 16, 128));
        assertThrows(IllegalArgumentException.class, () -> CrusherRecipeRules.validate(
                MaterialPrefixes.RAW_ORE, MaterialPrefixes.CRUSHED_ORE, 1, 0, 128));
        assertThrows(IllegalArgumentException.class, () -> CrusherRecipeRules.validate(
                MaterialPrefixes.RAW_ORE, MaterialPrefixes.CRUSHED_ORE, 1, 16, 0));
        assertThrows(IllegalArgumentException.class, () -> CrusherRecipeRules.validate(
                MaterialPrefixes.RAW_ORE, MaterialPrefixes.RAW_ORE, 1, 16, 128));
        assertThrows(IllegalArgumentException.class, () -> CrusherRecipeRules.validate(
                MaterialPrefixes.RAW_ORE, MaterialPrefixes.CRUSHED_ORE, 2, 16, 128,
                Map.of("lead", 0)));
    }

    @Test
    void materialDurationsOverrideTheDefaultFromTheGt6Dump() {
        Map<String, Integer> materialDurations =
                Map.of("lead", 256, "nickel", 384);
        assertEquals(128, CrusherRecipeRules.durationFor(
                "copper", 128, materialDurations));
        assertEquals(256, CrusherRecipeRules.durationFor(
                "lead", 128, materialDurations));
        assertEquals(384, CrusherRecipeRules.durationFor(
                "nickel", 128, materialDurations));
    }
}
