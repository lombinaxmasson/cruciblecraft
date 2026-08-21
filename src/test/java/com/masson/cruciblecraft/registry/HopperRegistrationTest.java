package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialLoader;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

class HopperRegistrationTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
    @Test
    void oneHundredTwentyHoldersMatchTheFlatCatalog() {
        assertEquals(60, HopperVariantCatalog.baseEntries().size());
        assertEquals(120, HopperVariantCatalog.variants().size());
        assertEquals(120, ModBlocks.hopperBlocksById().size());
        assertEquals(120, ModItems.hopperItemsById().size());
        assertEquals("hopper", ModBlockEntities.HOPPER.getId().getPath());
        assertEquals(
                "steel_dust_funnel",
                ModBlockEntities.DUST_FUNNEL.getId().getPath());
        assertEquals(
                "steel_dust_funnel",
                ModBlocks.STEEL_DUST_FUNNEL.getId().getPath());
        assertEquals(
                "steel_dust_funnel",
                ModItems.STEEL_DUST_FUNNEL.getId().getPath());
        Set<String> ids = new HashSet<>();
        for (HopperVariant variant : HopperVariantCatalog.variants()) {
            assertTrue(ids.add(variant.id().toString()), variant.id().toString());
            assertSame(
                    ModBlocks.hopperBlocksById().get(variant.id()),
                    ModBlocks.hopperBlocksById().get(variant.id()));
            assertTrue(
                    ModBlocks.hopperBlocksById().containsKey(variant.id()),
                    variant.id().toString());
            assertTrue(
                    ModItems.hopperItemsById().containsKey(variant.id()),
                    variant.id().toString());
            assertEquals(
                    variant.id(),
                    ModBlocks.hopperBlocksById().get(variant.id()).getId());
            assertEquals(
                    variant.id(),
                    ModItems.hopperItemsById().get(variant.id()).getId());
        }
        assertFalse(ModBlocks.hopperBlocksById().containsKey(
                ResourceLocation.parse("cruciblecraft:hopper")));
        assertFalse(ModItems.hopperItemsById().containsKey(
                ResourceLocation.parse("cruciblecraft:queue_hopper")));
        assertEquals(
                60,
                HopperVariantCatalog.variantsOf(HopperKind.HOPPER).size());
        assertEquals(
                60,
                HopperVariantCatalog.variantsOf(HopperKind.QUEUE_HOPPER).size());
    }

    @Test
    void everyCatalogMaterialResolvesAndPinsSlotSentinels(
            @TempDir Path configDirectory) {
        Set<String> materials = MaterialLoader.load(configDirectory).values()
                .stream()
                .map(MaterialDefinition::id)
                .collect(Collectors.toUnmodifiableSet());
        HopperVariantCatalog.baseEntries().forEach(entry ->
                assertTrue(
                        materials.contains(entry.materialPath()),
                        entry.materialId().toString()));
        assertEquals(
                1,
                HopperVariantCatalog.require(
                        ResourceLocation.parse("cruciblecraft:lead_hopper"))
                        .slots());
        assertEquals(
                2,
                HopperVariantCatalog.require(
                        ResourceLocation.parse(
                                "cruciblecraft:bismuth_queue_hopper"))
                        .slots());
        assertEquals(
                36,
                HopperVariantCatalog.require(
                        ResourceLocation.parse(
                                "cruciblecraft:infinity_hopper"))
                        .slots());
    }
}
