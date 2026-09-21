package com.masson.cruciblecraft.worldgen.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TreeHoleFluidCatalogTest {
    @Test
    void rubberTreeSapIsTheOnlyTreeHoleOverlayFluid() {
        assertEquals(1, TreeHoleFluidCatalog.FLUID_COUNT);
        assertEquals(1, TreeHoleFluidCatalog.fluids().size());
        var spec = TreeHoleFluidCatalog.fluids().getFirst();
        assertEquals("cruciblecraft:rubber_tree_sap", spec.id().toString());
        assertEquals("fluidrubbertreesap", spec.sourceFluid());
        assertEquals("Rubber Tree Sap", spec.englishName());
        assertTrue(spec.colorRgb() > 0);
    }
}
