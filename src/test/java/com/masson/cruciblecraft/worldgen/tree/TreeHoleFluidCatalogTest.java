package com.masson.cruciblecraft.worldgen.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TreeHoleFluidCatalogTest {
    @Test
    void rubberAndMapleSapAreTreeHoleOverlayFluids() {
        assertEquals(2, TreeHoleFluidCatalog.FLUID_COUNT);
        assertEquals(2, TreeHoleFluidCatalog.fluids().size());
        var spec = TreeHoleFluidCatalog.fluids().get(0);
        assertEquals("cruciblecraft:rubber_tree_sap", spec.id().toString());
        assertEquals("fluidrubbertreesap", spec.sourceFluid());
        assertEquals("Rubber Tree Sap", spec.englishName());
        assertTrue(spec.colorRgb() > 0);
        var maple = TreeHoleFluidCatalog.fluids().get(1);
        assertEquals("cruciblecraft:maplesap", maple.id().toString());
        assertEquals("maplesap", maple.sourceFluid());
        assertEquals("Maple Sap", maple.englishName());
        assertTrue(maple.colorRgb() > 0);
    }
}
