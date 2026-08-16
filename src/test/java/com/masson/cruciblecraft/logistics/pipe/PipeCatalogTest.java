package com.masson.cruciblecraft.logistics.pipe;

import java.nio.file.Path;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog.Kind;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipeCatalogTest {
    @Test
    void sourceBackedMetadataClosesExactRuntimeBudget(@TempDir Path config) {
        var materials = MaterialLoader.load(config).values();
        if (!PipeCatalog.isInitialized()) {
            PipeCatalog.initialize(
                    materials,
                    MaterialRegistrationGate.load(materials));
        }

        assertEquals(282, PipeCatalog.all().size());
        assertEquals(210, PipeCatalog.fluid().size());
        assertEquals(72, PipeCatalog.item().size());
        assertTrue(PipeCatalog.all().size()
                <= PipeCatalog.MAX_RUNTIME_BLOCKS);
        assertEquals(72, MaterialPrefixes.TINY_FLUID_PIPE.units());
        assertEquals(144, MaterialPrefixes.SMALL_FLUID_PIPE.units());
        assertEquals(432, MaterialPrefixes.FLUID_PIPE.units());
        assertEquals(864, MaterialPrefixes.LARGE_FLUID_PIPE.units());
        assertEquals(1728, MaterialPrefixes.HUGE_FLUID_PIPE.units());
        assertEquals(432, MaterialPrefixes.ITEM_PIPE.units());
        assertEquals(864, MaterialPrefixes.LARGE_ITEM_PIPE.units());
        assertEquals(1728, MaterialPrefixes.HUGE_ITEM_PIPE.units());
        assertEquals(
                "pipeTiny",
                PipeCatalog.requireSpecification(
                        Kind.FLUID, "tiny_fluid_pipe"));
        assertEquals(
                "pipeTiny",
                PipeCatalog.requireSpecification(Kind.FLUID, "pipeTiny"));
        assertEquals(
                "pipeLarge",
                PipeCatalog.requireSpecification(
                        Kind.ITEM, "large_item_pipe"));
        assertThrows(
                IllegalArgumentException.class,
                () -> PipeCatalog.requireSpecification(
                        Kind.FLUID, "large_item_pipe"));
        assertThrows(
                IllegalArgumentException.class,
                () -> PipeCatalog.requireSpecification(
                        Kind.ITEM, "pipeTiny"));

        var copperFluid = PipeCatalog.require(
                "copper", MaterialPrefixes.FLUID_PIPE, Kind.FLUID);
        assertEquals(600, copperFluid.fluid().capacityMb());
        assertEquals(
                1696,
                copperFluid.fluid().maxTemperatureKelvin());
        assertTrue(copperFluid.fluid().gasProof());
        assertFalse(copperFluid.fluid().acidProof());

        var tinItem = PipeCatalog.require(
                "tin", MaterialPrefixes.ITEM_PIPE, Kind.ITEM);
        assertEquals(16_384, tinItem.item().stepSize());
        assertEquals(1, tinItem.item().stacksPerSecond());
        for (String material : java.util.List.of("copper", "tin", "iron")) {
            assertTrue(PipeCatalog.contains(
                    material, MaterialPrefixes.FLUID_PIPE, Kind.FLUID));
            assertTrue(PipeCatalog.contains(
                    material, MaterialPrefixes.ITEM_PIPE, Kind.ITEM));
        }
    }
}
