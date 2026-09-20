package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MachineDeliveryCatalogTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void kindCatalogHostsCoverEveryMachineKind() {
        Set<String> kinds = MachineKindCatalog.kinds().stream()
                .map(kind -> kind.id().toString())
                .collect(Collectors.toSet());
        Set<String> deliveryKinds = MachineDeliveryCatalog.hosts().stream()
                .filter(MachineDeliveryCatalog.Host::kindCatalog)
                .map(host -> host.id().toString())
                .collect(Collectors.toSet());
        assertEquals(kinds, deliveryKinds);
    }

    @Test
    void liveSpecsMatchDeliverySlotsAndRecipeMap() {
        allSpecs().forEach(spec -> {
            var host = MachineDeliveryCatalog.require(spec.id());
            assertEquals(host.recipeMap(), spec.recipeMapId(), spec.id().toString());
            assertEquals(
                    host.itemInputs(),
                    spec.items().inputs().size(),
                    spec.id() + " item inputs");
            assertEquals(
                    host.itemOutputs(),
                    spec.items().outputs().size(),
                    spec.id() + " item outputs");
            assertEquals(
                    host.fluidInputs(),
                    spec.fluids().inputs().size(),
                    spec.id() + " fluid inputs");
            assertEquals(
                    host.fluidOutputs(),
                    spec.fluids().outputs().size(),
                    spec.id() + " fluid outputs");
        });
    }

    @Test
    void laserWelderLiveSlotsMatchGt6WelderPanel() {
        var host = MachineDeliveryCatalog.require(
                ResourceLocation.parse("cruciblecraft:laser_welder"));
        assertEquals(9, host.itemInputs());
        assertEquals(1, host.itemOutputs());
        assertEquals(1, host.fluidInputs());
        assertEquals(0, host.fluidOutputs());
        assertEquals(host.gt6InItems(), host.itemInputs());
        assertEquals(host.gt6OutItems(), host.itemOutputs());
        assertEquals(host.gt6InFluids(), host.fluidInputs());
        assertEquals(host.gt6OutFluids(), host.fluidOutputs());
        assertEquals(9, ModProcessingMachines.LASER_WELDER.items().inputs().size());
        assertEquals(10, ModProcessingMachines.LASER_WELDER.items().slotCount());
    }

    @Test
    void laserEngraverIsCatalogHostAndHasTextures() {
        var host = MachineDeliveryCatalog.require(
                ResourceLocation.parse("cruciblecraft:laser_engraver"));
        assertTrue(host.kindCatalog());
        assertEquals("laser", host.specFamily());
        assertTrue(MachineTextureProfiles.hasMachineTextures(host.textureProfile()));
        assertEquals("distillation_tower", MachineTextureProfiles.textureId("distillation_tower"));
        assertEquals("cryo_distillation_tower", MachineTextureProfiles.textureId("cryo_distillation_tower"));
        assertEquals("boiler", MachineTextureProfiles.textureId("large_boiler"));
        assertEquals("large_crucible", MachineTextureProfiles.textureId("large_crucible"));
        assertEquals("dryer", MachineTextureProfiles.textureId("drying"));
        assertNull(MachineTextureProfiles.shapedMachineModel("bath"));
        assertEquals("mortar", MachineTextureProfiles.shapedMachineModel("mortar"));
        assertEquals("sifter", MachineTextureProfiles.shapedMachineModel("sifter"));
    }

    @Test
    void largeCrucibleDoesNotAliasCokeOvenTextures() {
        assertEquals("large_crucible", MachineTextureProfiles.textureId("large_crucible"));
    }

    private static Stream<ProcessingMachineSpec> allSpecs() {
        return Stream.concat(
                Stream.of(ModProcessingMachines.CRUSHER),
                ModProcessingMachines.CONFIGURED_MACHINES.stream());
    }
}
