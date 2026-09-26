package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void factoryMatchesLiveSlicerAndRollformer() {
        assertFactoryMatches(ModProcessingMachines.SLICER, 65_536L, 8_192L);
        assertFactoryMatches(ModProcessingMachines.ROLLFORMER, 4_096L, 256L);
        assertTrue(ProcessingMachineIgnition.requires(ModProcessingMachines.BURN_MIXER));
        assertFalse(ProcessingMachineIgnition.requires(ModProcessingMachines.SLICER));
        assertEquals(6, ModProcessingMachines.BURN_MIXER.items().inputs().size());
        assertEquals(1, ModProcessingMachines.BURN_MIXER.items().outputs().size());
        assertEquals(6, ModProcessingMachines.BURN_MIXER.fluids().inputs().size());
        assertEquals(2, ModProcessingMachines.BURN_MIXER.fluids().outputs().size());
        assertEquals(
                ModProcessingMachines.BURN_MIXER_FLUID_INPUT,
                ModProcessingMachines.BURN_MIXER.fluids().inputs().getFirst().capacity());
        assertEquals(
                ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT,
                ModProcessingMachines.CATALYTIC_CRACKER.fluids().outputs().getFirst().capacity());
        assertEquals(9, ModProcessingMachines.STEAM_CRACKER.fluids().outputs().size());
        assertEquals(0, ModProcessingMachines.CRYSTALLISATION_CRUCIBLE.fluids().outputs().size());
    }

    private static void assertFactoryMatches(
            ProcessingMachineSpec live, long capacity, long packet) {
        ProcessingMachineSpec generated = ProcessingMachineSpecFactory.create(
                MachineDeliveryCatalog.require(live.id()),
                live.recipeMap(),
                capacity,
                packet,
                4_000,
                live.ui().statuses(),
                live.validator());
        assertEquals(live.id(), generated.id());
        assertEquals(live.recipeMapId(), generated.recipeMapId());
        assertEquals(live.requireRecipeMap().id(), generated.requireRecipeMap().id());
        assertEquals(live.items().inputs(), generated.items().inputs());
        assertEquals(live.items().outputs(), generated.items().outputs());
        for (int slot = 0; slot < live.items().slotCount(); slot++) {
            assertEquals(live.items().role(slot), generated.items().role(slot), live.id() + " slot " + slot);
        }
        assertEquals(live.fluids(), generated.fluids());
        assertEquals(live.energy(), generated.energy());
        assertEquals(live.sidedIo(), generated.sidedIo());
        assertEquals(live.buffering(), generated.buffering());
        assertEquals(live.ui(), generated.ui());
        assertSame(live.validator(), generated.validator());
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
