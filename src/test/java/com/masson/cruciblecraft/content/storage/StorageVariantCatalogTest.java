package com.masson.cruciblecraft.content.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class StorageVariantCatalogTest {
    @Test
    void bundledCatalogIsSixTwentyFourPlusOne() {
        assertEquals(624, StorageVariantCatalog.STORAGE_COUNT);
        assertEquals(1, StorageVariantCatalog.LOGISTICS_COUNT);
        assertEquals(625, StorageVariantCatalog.TOTAL_COUNT);
        assertEquals(625, StorageVariantCatalog.variants().size());
        assertEquals(624, StorageVariantCatalog.storageVariants().size());
        assertNotNull(StorageVariantCatalog.logistics());
        assertFalse(StorageVariantCatalog.logistics().countsTowardStorage624());
        assertTrue(StorageVariantCatalog.logistics().logistics());
        Set<String> ids = StorageVariantCatalog.variants().stream()
                .map(variant -> variant.id().toString())
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(625, ids.size());
        assertTrue(ids.contains("cruciblecraft:bookshelf_7100"));
        assertTrue(ids.contains("cruciblecraft:bookshelf_7000"));
        assertTrue(ids.contains("cruciblecraft:locker_7300"));
        assertTrue(ids.contains("cruciblecraft:charging_locker_7500"));
        assertTrue(ids.contains("cruciblecraft:mass_storage_barrel_6999"));
        assertTrue(ids.contains("cruciblecraft:storage_inserter_32751"));
        assertTrue(ids.contains("cruciblecraft:mass_storage_logistics_6200"));
        assertEquals(
                StorageVariantCatalog.SOURCE_REVISION,
                "3703e40308c8c030763fd6297dea8b210d2a77b1");
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_t44/structure/empty.nbt")));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_t44/gametest/structure/empty.nbt")));
    }

    @Test
    void visibleRowsAreEighteenAndHiddenRowsKeepIdentities() {
        assertEquals(18, StorageVariantCatalog.sourceVisible().size());
        StorageVariantCatalog.sourceVisible().forEach(variant ->
                assertTrue(variant.sourceVisible(), variant.id().toString()));
        long hidden = StorageVariantCatalog.variants().stream()
                .filter(variant -> !variant.sourceVisible())
                .count();
        assertEquals(607, hidden);
        StorageVariant oak = StorageVariantCatalog.require(
                ResourceLocation.parse("cruciblecraft:bookshelf_7000"));
        assertTrue(oak.sourceVisible());
        StorageVariant hiddenPlank = StorageVariantCatalog.require(
                ResourceLocation.parse("cruciblecraft:bookshelf_7001"));
        assertFalse(hiddenPlank.sourceVisible());
        assertEquals(Integer.valueOf(1), hiddenPlank.plankIndex());
    }

    @Test
    void profileHostsCoverEveryRow() {
        int assigned = 0;
        for (StorageBehaviorProfile profile : StorageBehaviorProfile.values()) {
            assigned += StorageVariantCatalog.of(profile).size();
        }
        assertEquals(625, assigned);
        assertEquals(
                2,
                StorageVariantCatalog.of(StorageBehaviorProfile.LOCKER).size()
                        + StorageVariantCatalog.of(
                                StorageBehaviorProfile.LOCKER_CHARGING).size());
        assertEquals(
                1,
                StorageVariantCatalog.of(
                        StorageBehaviorProfile.MASS_STORAGE_LOGISTICS).size());
    }
}
