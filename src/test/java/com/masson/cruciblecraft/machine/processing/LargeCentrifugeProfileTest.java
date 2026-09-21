package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeCentrifugeProfileTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void controllerProfileIsSourceBackedAndOutsideSingleBlockVariants() {
        // Opening casing catalog originally closed with 9 variants.
        // Kinetic and heat variants extend that catalog without
        // changing this profile.
        assertTrue(MachineTierCatalog.entries().size() > 33);
        assertTrue(ModMachineVariants.ALL.size() > 33);
        assertEquals(
                33,
                MachineTierCatalog.entries().stream()
                        .filter(entry ->
                                ModMachineVariants.isOpening(entry.variantId()))
                        .count());
        Set<String> heatKinds = Set.of(
                "cruciblecraft:distillery",
                "cruciblecraft:drying",
                "cruciblecraft:smelter");
        assertEquals(
                24,
                MachineTierCatalog.entries().stream()
                        .filter(entry ->
                                ModMachineVariants.isOpening(entry.variantId())
                                        && !heatKinds.contains(
                                                entry.kindId().toString()))
                        .count());
        assertEquals(12, MachineTierCatalog.controllerTierBands().size());

        TierProfile tower =
                ModMultiblockControllers.DISTILLATION_TOWER_VARIANT
                        .tierBand();
        assertEquals(
                "cruciblecraft:distillation_tower_profile",
                tower.tierBandId().toString());
        assertEquals(
                "cruciblecraft:distillation_tower",
                tower.materialId());
        assertEquals(EnergyType.HEAT, tower.energyType());
        assertEquals(512L, tower.inputNominal());
        assertEquals(1_024L, tower.inputMaximum());
        assertEquals(4_096L, tower.energyCapacity());
        assertEquals(1, tower.parallelLimit());
        assertEquals(10_000, tower.efficiency());

        TierProfile cryo =
                ModMultiblockControllers.CRYO_DISTILLATION_TOWER_VARIANT
                        .tierBand();
        assertEquals(
                "cruciblecraft:cryo_distillation_tower_profile",
                cryo.tierBandId().toString());
        assertEquals(
                "cruciblecraft:cryo_distillation_tower",
                cryo.materialId());
        assertEquals(EnergyType.CU, cryo.energyType());
        assertEquals(512L, cryo.inputNominal());
        assertEquals(1_024L, cryo.inputMaximum());
        assertEquals(4_096L, cryo.energyCapacity());

        TierProfile profile =
                ModMultiblockControllers.LARGE_CENTRIFUGE_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:large_centrifuge_profile",
                profile.tierBandId().toString());
        assertEquals(
                "cruciblecraft:large_centrifuge",
                profile.materialId());
        assertEquals(EnergyType.KINETIC_ROTATION, profile.energyType());
        assertEquals(512L, profile.inputMinimum());
        assertEquals(512L, profile.inputNominal());
        assertEquals(4_096L, profile.inputMaximum());
        assertEquals(4_096L, profile.energyCapacity());
        assertEquals(16, profile.parallelLimit());
        assertEquals(5_000, profile.efficiency());

        TierProfile titanium = ModMachineVariants.require(
                net.minecraft.resources.ResourceLocation
                        .fromNamespaceAndPath(
                                "cruciblecraft",
                                "titanium_centrifuge"))
                .tierBand();
        assertNotEquals(
                titanium.tierBandId(), profile.tierBandId());
        assertNotEquals(titanium.materialId(), profile.materialId());
        assertEquals(
                4_096L,
                ModMultiblockControllers.LARGE_CENTRIFUGE_VARIANT
                        .runtimeSpec()
                        .energy()
                        .maxPacket());
    }

    @Test
    void cheapPolicyAcceptsFullEutAndKeepsParallelDurationBehavior() {
        MachineKindSpec kind =
                ModMultiblockControllers.LARGE_CENTRIFUGE_KIND;
        TierProfile profile =
                ModMultiblockControllers.LARGE_CENTRIFUGE_VARIANT.tierBand();
        assertSame(ModProcessingMachines.CENTRIFUGE, kind.behavior());
        assertEquals(
                ModMachineVariants.CENTRIFUGE.recipeMapId(),
                kind.recipeMapId());
        assertEquals(
                MachineKindSpec.OverclockPolicy.CHEAP,
                kind.overclockPolicy());
        assertTrue(kind.parallelDuration());

        assertTrue(MachineExecutionPlan.create(
                recipe(4_096L, 40), kind, profile, 1).isPresent());
        assertFalse(MachineExecutionPlan.create(
                recipe(4_097L, 40), kind, profile, 1).isPresent());

        MachineExecutionPlan cheap = MachineExecutionPlan.create(
                recipe(16L, 100), kind, profile, 1).orElseThrow();
        assertEquals(16L, cheap.minimumPower());
        assertEquals(512L, cheap.nominalPower());
        assertEquals(3_200L, cheap.totalWork());
        assertEquals(7, cheap.effectiveDuration());
        assertEquals(0, cheap.overclockSteps());

        MachineExecutionPlan parallel = MachineExecutionPlan.create(
                recipe(16L, 100), kind, profile, 16).orElseThrow();
        assertEquals(16, parallel.operations());
        assertEquals(51_200L, parallel.totalWork());
        assertEquals(100, parallel.effectiveDuration());
    }

    @Test
    void bundledSourceContractPinsControllerProfileAndCheapPolicy() {
        var stream = MachineTierCatalog.class.getResourceAsStream(
                "/data/cruciblecraft/machine_tiers.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject source = document.getAsJsonObject("source");
        String controllerSource = source.getAsJsonObject(
                "controller_profile_rows")
                .get("cruciblecraft:large_centrifuge_profile")
                .getAsString();
        assertTrue(
                controllerSource.contains("Loader_MultiTileEntities.java:1229"));
        assertTrue(controllerSource.endsWith(
                "#structure_projection.large_centrifuge"));
        assertTrue(source.getAsJsonObject("normalization")
                .get("controller_profile")
                .getAsString()
                .contains("CHEAP"));
        JsonObject profile = document
                .getAsJsonArray("controller_profiles")
                .get(0)
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:large_centrifuge_profile",
                profile.get("tierBand").getAsString());
        assertEquals(4_096, profile.get("inputMaximum").getAsInt());
        assertEquals(16, profile.get("parallel").getAsInt());
        assertEquals(5_000, profile.get("efficiency").getAsInt());
    }

    @Test
    void physicalPortsBridgeOneSourceDerivedHostLayout() {
        var stream = LargeCentrifugeProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/"
                        + "large_centrifuge.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject palette = document.getAsJsonObject("palette");
        int itemFluidPorts = 0;
        int energyPorts = 0;
        int controllers = 0;
        for (var row : document.getAsJsonArray("structure")) {
            JsonObject predicate = palette.getAsJsonObject(
                    row.getAsJsonObject().get("predicate").getAsString());
            if ("controller".equals(predicate.get("type").getAsString())) {
                controllers++;
            } else if ("item_fluid".equals(
                    predicate.get("port").getAsString())) {
                itemFluidPorts++;
            } else if ("energy_input".equals(
                    predicate.get("port").getAsString())) {
                energyPorts++;
            }
        }

        ProcessingMachineSpec host = ModProcessingMachines.CENTRIFUGE;
        assertEquals(15, itemFluidPorts);
        assertEquals(2, energyPorts);
        assertEquals(1, controllers);
        assertEquals(1, host.items().inputs().size());
        assertEquals(6, host.items().outputs().size());
        assertEquals(7, host.items().slotCount());
        assertEquals(1, host.fluids().inputs().size());
        assertEquals(6, host.fluids().outputs().size());
        assertEquals(7, host.fluids().all().size());
        assertNotEquals(itemFluidPorts, host.items().inputs().size());
    }

    private static GTRecipe recipe(long eut, int duration) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.COBBLESTONE)),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(new ItemStack(Items.GRAVEL)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                duration,
                eut,
                0L,
                true,
                Optional.empty());
    }
}
