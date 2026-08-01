package com.masson.cruciblecraft.content.mold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;

import org.junit.jupiter.api.Test;

class MoldCastingRulesTest {
    @Test
    void shapeMasksMatchGt6FiveByFivePatterns() {
        assertEquals(0b0_01110_01110_01110_01110_01110, MoldShape.INGOT.mask());
        assertEquals(0b0_11111_11111_11111_11111_11111, MoldShape.PLATE.mask());
        assertEquals(0b0_00000_00000_11111_00000_00000, MoldShape.ROD.mask());
        assertEquals(0b0_00000_00000_00100_00100_00000, MoldShape.BOLT.mask());
    }

    @Test
    void pureMaterialUsesTheFormUnitCost() {
        var batch = MoldCastingRules.smallestBatch(
                Map.of("copper", MaterialPrefixes.INGOT.units()),
                MaterialPrefixes.ROD).orElseThrow();
        assertEquals(Map.of("copper", MaterialPrefixes.ROD.units()), batch.cost());
        assertEquals(1, batch.outputCount());
    }

    @Test
    void threeToOneAlloyBoltUsesTheSmallestIntegralBatch() {
        var batch = MoldCastingRules.smallestBatch(
                Map.of("copper", 108, "tin", 36),
                MaterialPrefixes.BOLT).orElseThrow();
        assertEquals(2, batch.outputCount());
        assertEquals(Map.of("copper", 27, "tin", 9), batch.cost());
    }

    @Test
    void coolingAndCeramicLimitUseGt6Boundaries() {
        assertEquals(995.0F, MoldCastingRules.cool(1_000.0F, 20.0F));
        assertEquals(20.0F, MoldCastingRules.cool(22.0F, 20.0F));
        assertEquals(2_227.04F, MoldCastingRules.maximumTemperature(1_727.0F), 0.01F);
        assertTrue(MoldCastingRules.maximumTemperature(1_727.0F) > 2_000.0F);
    }
}
