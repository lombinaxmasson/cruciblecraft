package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class GtBuildingBlockCatalogTest {
    @Test
    void catalogKeepsGlassGlowGlassAndPromotedLeftovers() {
        assertEquals(246, GtBuildingBlockCatalog.VARIANT_COUNT);
        assertEquals(21, GtBuildingBlockCatalog.PROMOTED_LEFTOVER_COUNT);
        assertEquals(246, GtBuildingBlockCatalog.variants().size());
        Set<String> ids = new HashSet<>();
        int glass = 0;
        int glow = 0;
        int slabs = 0;
        for (GtBlockObjectCatalog.Variant variant : GtBuildingBlockCatalog.variants()) {
            assertTrue(ids.add(variant.id().toString()), variant.id()::toString);
            if (variant.glassLike()) {
                if (variant.glowGlass()) {
                    glow++;
                } else {
                    glass++;
                }
            }
            if (variant.slab()) {
                slabs++;
            }
        }
        assertEquals(246, ids.size());
        assertEquals(112, glass);
        assertEquals(112, glow);
        assertTrue(slabs >= 202);
        assertNotNull(GtBuildingBlockCatalog.find(
                ResourceLocation.parse("cruciblecraft:glass/black")));
        assertNotNull(GtBuildingBlockCatalog.find(
                ResourceLocation.parse("cruciblecraft:glow_glass/white/slab_down")));
        assertNotNull(GtBuildingBlockCatalog.find(
                ResourceLocation.parse("cruciblecraft:diggable/turf")));
        assertNotNull(GtBuildingBlockCatalog.find(
                ResourceLocation.parse("cruciblecraft:asphalt/white/slab_up")));
        assertNotNull(GtBuildingBlockCatalog.find(
                ResourceLocation.parse("cruciblecraft:diggable/mud")));
    }
}
