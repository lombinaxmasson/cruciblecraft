package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class BedrockOreCatalogTest {
    @Test
    void noModLoaderWorldgenCounts() {
        assertEquals(40, BedrockOreCatalog.VEINS.size());
        assertEquals(33, BedrockOreCatalog.OVERWORLD_COUNT);
        assertEquals(7, BedrockOreCatalog.NETHER_COUNT);
        assertEquals(
                BedrockOreCatalog.OVERWORLD_COUNT,
                BedrockOreCatalog.forNether(false).size());
        assertEquals(
                BedrockOreCatalog.NETHER_COUNT,
                BedrockOreCatalog.forNether(true).size());
    }

    @Test
    void firstAndLastRowsMatchLoaderWorldgen() {
        assertEquals("ore.bedrock.diamond", BedrockOreCatalog.VEINS.getFirst().gt6Name());
        assertEquals(128000, BedrockOreCatalog.VEINS.getFirst().probability());
        assertEquals("diamond", BedrockOreCatalog.VEINS.getFirst().materialId());
        var last = BedrockOreCatalog.VEINS.getLast();
        assertEquals("ore.bedrock.ancientdebris", last.gt6Name());
        assertEquals("ancient_debris", last.materialId());
        assertTrue(last.nether());
        assertFalse(BedrockOreCatalog.VEINS.getFirst().nether());
        assertEquals(
                Optional.of(IndicatorFlower.PANDANUS_CANDELABRUM),
                BedrockOreCatalog.VEINS.getFirst().flower());
        assertTrue(BedrockOreCatalog.VEINS.getLast().flower().isEmpty());
    }

    @Test
    void loaderWorldgenFlowerMetas() {
        assertEquals(
                IndicatorFlower.of(IndicatorFlower.Family.A, 0),
                BedrockOreCatalog.VEINS.stream()
                        .filter(vein -> "ore.bedrock.gold.a".equals(vein.gt6Name()))
                        .findFirst()
                        .orElseThrow()
                        .flower()
                        .orElseThrow());
        assertEquals(
                IndicatorFlower.of(IndicatorFlower.Family.B, 2),
                BedrockOreCatalog.VEINS.stream()
                        .filter(vein -> "ore.bedrock.gold.b".equals(vein.gt6Name()))
                        .findFirst()
                        .orElseThrow()
                        .flower()
                        .orElseThrow());
        assertEquals(
                IndicatorFlower.TUNGSTUS,
                BedrockOreCatalog.VEINS.stream()
                        .filter(vein -> "ore.bedrock.tungstate".equals(vein.gt6Name()))
                        .findFirst()
                        .orElseThrow()
                        .flower()
                        .orElseThrow());
    }

    @Test
    void duplicateStibniteNameKeepsBothMaterials() {
        assertTrue(BedrockOreCatalog.VEINS.stream().anyMatch(
                vein -> "arsenopyrite".equals(vein.materialId())
                        && vein.probability() == 8000));
        assertTrue(BedrockOreCatalog.VEINS.stream().anyMatch(
                vein -> "stibnite".equals(vein.materialId())
                        && vein.probability() == 4000));
    }

    @Test
    void goldHasTwoIndependentRolls() {
        long gold = BedrockOreCatalog.forNether(false).stream()
                .filter(vein -> "gold".equals(vein.materialId()))
                .count();
        assertEquals(2, gold);
    }

    @Test
    void muffinExtentsMatchWorldgenOresBedrock() {
        assertEquals(7, BedrockOreVeins.MUFFIN_MIN.length);
        assertEquals(7, BedrockOreVeins.MUFFIN_MAX.length);
        assertEquals(5, BedrockOreVeins.MUFFIN_MIN[0]);
        assertEquals(11, BedrockOreVeins.MUFFIN_MAX[0]);
        assertEquals(0, BedrockOreVeins.MUFFIN_MIN[4]);
        assertEquals(16, BedrockOreVeins.MUFFIN_MAX[4]);
    }

    @Test
    void probabilitiesMatchLoaderWorldgen() {
        int[] expected = {
            128000, 96000, 96000, 96000, 96000, 96000, 96000, 96000, 96000,
            60000, 60000, 32000, 32000, 16000, 16000, 16000, 14000, 8000, 8000,
            7000, 6000, 6000, 5000, 5000, 4000, 4000, 3000, 3000, 3000, 3000,
            2000, 2000, 2000, 4000, 4000, 4000, 2000, 2000, 8000, 4000
        };
        assertEquals(expected.length, BedrockOreCatalog.VEINS.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(
                    expected[i],
                    BedrockOreCatalog.VEINS.get(i).probability(),
                    BedrockOreCatalog.VEINS.get(i).gt6Name());
        }
    }
}
