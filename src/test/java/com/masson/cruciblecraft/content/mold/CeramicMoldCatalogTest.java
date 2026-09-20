package com.masson.cruciblecraft.content.mold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;

import org.junit.jupiter.api.Test;

class CeramicMoldCatalogTest {
    @Test
    void shapedVariantsCoverGt6ClayMoldMetadata900To929() {
        assertEquals(30, CeramicMoldCatalog.SHAPED.size());
        Set<Integer> metas = new HashSet<>();
        Set<Integer> patterns = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (CeramicMoldCatalog.Variant variant : CeramicMoldCatalog.SHAPED) {
            assertTrue(ids.add(variant.id()), variant.id());
            assertTrue(metas.add(variant.gt6Meta()), variant.id());
            assertTrue(patterns.add(variant.firedPattern()), variant.id());
            assertEquals(
                    variant.firedPattern(),
                    variant.firedPattern() & ((1 << MoldRecipes.CELL_COUNT) - 1),
                    variant.id());
            assertEquals(variant, CeramicMoldCatalog.require(variant.id()));
            assertEquals(
                    variant, CeramicMoldCatalog.findByPattern(variant.firedPattern()));
            assertTrue(MoldRecipes.recipe(variant.firedPattern()).isPresent(), variant.id());
        }
        for (int meta = 900; meta <= 929; meta++) {
            assertTrue(metas.contains(meta), "missing GT6 meta " + meta);
        }
    }

    @Test
    void nuggetFiringMaskIsTheRepresentativeNugget() {
        assertEquals(
                Integer.valueOf(0b0_00000_00000_00100_00000_00000),
                MoldRecipes.representativeMasks().get(MaterialPrefixes.NUGGET));
        assertNotNull(CeramicMoldCatalog.findByPattern(0b0_00000_00000_00100_00000_00000));
        assertEquals("nugget", CeramicMoldCatalog.findByPattern(
                0b0_00000_00000_00100_00000_00000).id());
    }
}
