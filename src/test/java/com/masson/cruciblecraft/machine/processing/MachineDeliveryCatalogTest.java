package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void laserEngraverIsDeliveryOnlyAndHasTextures() {
        var host = MachineDeliveryCatalog.require(
                ResourceLocation.parse("cruciblecraft:laser_engraver"));
        assertFalse(host.kindCatalog());
        assertEquals("laser", host.specFamily());
        assertTrue(MachineTextureProfiles.hasMachineTextures(host.textureProfile()));
        assertEquals("distillery", MachineTextureProfiles.textureId("distillation_tower"));
        assertEquals("boiler", MachineTextureProfiles.textureId("large_boiler"));
        assertEquals("coke_oven", MachineTextureProfiles.textureId("large_crucible"));
        assertEquals("dryer", MachineTextureProfiles.textureId("drying"));
        assertEquals("bath", MachineTextureProfiles.shapedMachineModel("bath"));
    }

    private static Stream<ProcessingMachineSpec> allSpecs() {
        return Stream.concat(
                Stream.of(ModProcessingMachines.CRUSHER),
                ModProcessingMachines.CONFIGURED_MACHINES.stream());
    }
}
