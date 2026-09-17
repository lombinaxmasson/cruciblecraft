package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FluidSpringCatalogTest {
    @Test
    void noModLoaderWorldgenCounts() {
        assertEquals(8, FluidSpringCatalog.VEINS.size());
        assertEquals(7, FluidSpringCatalog.OVERWORLD_COUNT);
        assertEquals(1, FluidSpringCatalog.NETHER_COUNT);
        assertEquals(
                FluidSpringCatalog.OVERWORLD_COUNT,
                FluidSpringCatalog.forNether(false).size());
        assertEquals(
                FluidSpringCatalog.NETHER_COUNT,
                FluidSpringCatalog.forNether(true).size());
    }

    @Test
    void firstAndLastRowsMatchLoaderWorldgen() {
        assertEquals(
                "overworld.fluid.oil.extraheavy",
                FluidSpringCatalog.VEINS.getFirst().gt6Name());
        assertEquals(400, FluidSpringCatalog.VEINS.getFirst().probability());
        assertEquals(
                SpringFluidKind.OIL_EXTRA_HEAVY,
                FluidSpringCatalog.VEINS.getFirst().fluid());
        assertEquals(2, FluidSpringCatalog.VEINS.getFirst().indicatorType());
        assertEquals(6000, FluidSpringCatalog.VEINS.getFirst().springAmount());
        var last = FluidSpringCatalog.VEINS.getLast();
        assertEquals("nether.fluid.lava", last.gt6Name());
        assertEquals(SpringFluidKind.LAVA, last.fluid());
        assertTrue(last.nether());
        assertFalse(FluidSpringCatalog.VEINS.getFirst().nether());
    }

    @Test
    void probabilitiesAndIndicatorsMatchLoaderWorldgen() {
        int[] expected = {400, 400, 400, 400, 200, 100, 200, 100};
        int[] indicators = {2, 2, 2, 2, 1, 3, 1, 1};
        int[] amounts = {6000, 6000, 6000, 6000, 3000, 500, 1000, 500};
        assertEquals(expected.length, FluidSpringCatalog.VEINS.size());
        for (int i = 0; i < expected.length; i++) {
            var vein = FluidSpringCatalog.VEINS.get(i);
            assertEquals(expected[i], vein.probability(), vein.gt6Name());
            assertEquals(indicators[i], vein.indicatorType(), vein.gt6Name());
            assertEquals(amounts[i], vein.springAmount(), vein.gt6Name());
        }
        assertEquals(
                IndicatorGrass.BROWN,
                IndicatorGrass.ofIndicatorType(2));
        assertEquals(
                IndicatorGrass.YELLOW,
                IndicatorGrass.ofIndicatorType(1));
        assertEquals(
                IndicatorGrass.MEDIUM,
                IndicatorGrass.ofIndicatorType(3));
    }

    @Test
    void oilsAreDistinctFromCrudeOil() {
        assertTrue(FluidSpringCatalog.VEINS.stream().noneMatch(
                vein -> "crude_oil".equals(vein.fluid().path())));
    }
}
