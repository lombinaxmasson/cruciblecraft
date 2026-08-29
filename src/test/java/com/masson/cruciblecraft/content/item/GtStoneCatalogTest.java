package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class GtStoneCatalogTest {
    @Test
    void catalogKeepsOneHundredNineteenIdentitiesAndFourHundredSixVariants() {
        assertEquals(119, GtStoneCatalog.IDENTITY_COUNT);
        assertEquals(406, GtStoneCatalog.VARIANT_COUNT);
        assertEquals(406, GtStoneCatalog.variants().size());
        assertEquals(
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                GtStoneCatalog.sourceRevision());
        Set<String> ids = new HashSet<>();
        int slabs = 0;
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            assertTrue(ids.add(variant.id().toString()), variant.id()::toString);
            assertTrue(variant.registryPath().startsWith("gt_stone/"));
            assertTrue(variant.registryPath().contains("_m"));
            if (variant.slab()) {
                slabs++;
                assertTrue(variant.registryPath().contains("_slab_"));
            }
        }
        assertEquals(406, ids.size());
        assertTrue(slabs > 0);
        assertFalse(ids.contains("cruciblecraft:gt_stone/andesite"));
        GtStoneCatalog.Variant andesite = GtStoneCatalog.require(
                ResourceLocation.parse("cruciblecraft:gt_stone/andesite_m8"));
        assertFalse(andesite.slab());
        assertEquals(8, andesite.meta());
    }
}
