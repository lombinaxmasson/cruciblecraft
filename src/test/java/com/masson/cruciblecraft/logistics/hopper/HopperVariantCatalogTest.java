package com.masson.cruciblecraft.logistics.hopper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class HopperVariantCatalogTest {
    @Test
    void sourceBackedProjectionIsSixtyThenOneHundredTwenty() {
        assertEquals(60, HopperVariantCatalog.baseEntries().size());
        assertEquals(60, HopperVariantCatalog.variantsOf(HopperKind.HOPPER).size());
        assertEquals(
                60,
                HopperVariantCatalog.variantsOf(HopperKind.QUEUE_HOPPER).size());
        assertEquals(120, HopperVariantCatalog.variants().size());
        Set<String> ids = HopperVariantCatalog.variants().stream()
                .map(variant -> variant.id().toString())
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(120, ids.size());
        assertFalse(ids.contains("cruciblecraft:hopper"));
        assertFalse(ids.contains("cruciblecraft:queue_hopper"));
    }

    @Test
    void representativeSlotsFollowTheSourceRows() {
        assertEquals(
                1,
                HopperVariantCatalog.require(id("lead_hopper")).slots());
        assertEquals(
                2,
                HopperVariantCatalog.require(id("bismuth_queue_hopper")).slots());
        assertEquals(
                5,
                HopperVariantCatalog.require(id("steel_hopper")).slots());
        assertEquals(
                5,
                HopperVariantCatalog.require(id("steel_queue_hopper")).slots());
        assertEquals(
                36,
                HopperVariantCatalog.require(id("infinity_hopper")).slots());
        assertEquals(
                36,
                HopperVariantCatalog.require(id("infinity_queue_hopper")).slots());
    }

    @Test
    void idsAreMaterialPrefixedAndRequireRejectsUnknown() {
        HopperVariantCatalog.variants().forEach(variant -> {
            assertTrue(
                    variant.id().getPath().endsWith("_" + variant.kind().pathSuffix()),
                    variant.id().toString());
            assertFalse(
                    variant.id().getPath().equals(variant.kind().pathSuffix()),
                    variant.id().toString());
        });
        assertThrows(
                IllegalArgumentException.class,
                () -> HopperVariantCatalog.require(id("hopper")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
