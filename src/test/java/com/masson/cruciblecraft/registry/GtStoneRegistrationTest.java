package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

class GtStoneRegistrationTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void fourHundredSixHoldersMatchTheCatalogAndKeepSlabsDistinct() {
        assertEquals(119, GtStoneCatalog.IDENTITY_COUNT);
        assertEquals(406, GtStoneCatalog.VARIANT_COUNT);
        assertEquals(406, ModBlocks.gtStoneBlocksById().size());
        assertEquals(406, ModItems.gtStoneItemsById().size());
        Set<String> ids = new HashSet<>();
        int slabs = 0;
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            assertTrue(ids.add(variant.id().toString()), variant.id()::toString);
            assertTrue(ModBlocks.gtStoneBlocksById().containsKey(variant.id()));
            assertTrue(ModItems.gtStoneItemsById().containsKey(variant.id()));
            assertEquals(
                    variant.id(),
                    ModBlocks.gtStoneBlocksById().get(variant.id()).getId());
            assertEquals(
                    variant.id(),
                    ModItems.gtStoneItemsById().get(variant.id()).getId());
            if (variant.slab()) {
                slabs++;
                assertTrue(variant.registryPath().contains("slab"));
            }
        }
        assertEquals(406, ids.size());
        assertTrue(slabs > 0);
        assertFalse(ids.contains("cruciblecraft:gt_stone/andesite"));
        assertTrue(ids.contains("cruciblecraft:andesite/reinforced_bricks"));
        assertTrue(ids.contains("cruciblecraft:andesite/reinforced_bricks/slab_down"));
    }
}
