package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeElectrolyzerProfileTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void controllerProfileIsSourceBackedEu() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_ELECTROLYZER_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:large_electrolyzer_profile",
                profile.tierBandId().toString());
        assertEquals(
                "cruciblecraft:large_electrolyzer",
                profile.materialId());
        assertEquals(EnergyType.ELECTRIC, profile.energyType());
        assertEquals(512L, profile.inputMinimum());
        assertEquals(512L, profile.inputNominal());
        assertEquals(4_096L, profile.inputMaximum());
        assertEquals(4_096L, profile.energyCapacity());
        assertEquals(16, profile.parallelLimit());
        assertEquals(5_000, profile.efficiency());
    }

    @Test
    void cheapPolicyAcceptsFullEutAndKeepsParallelDurationBehavior() {
        MachineKindSpec kind =
                ModMultiblockControllers.LARGE_ELECTROLYZER_KIND;
        TierProfile profile =
                ModMultiblockControllers.LARGE_ELECTROLYZER_VARIANT.tierBand();
        assertSame(ModProcessingMachines.ELECTROLYZER, kind.behavior());
        assertEquals(
                MachineKindSpec.OverclockPolicy.CHEAP,
                kind.overclockPolicy());
        assertTrue(kind.parallelDuration());

        assertTrue(MachineExecutionPlan.create(
                recipe(4_096L, 40), kind, profile, 1).isPresent());
        assertFalse(MachineExecutionPlan.create(
                recipe(4_097L, 40), kind, profile, 1).isPresent());

        MachineExecutionPlan parallel = MachineExecutionPlan.create(
                recipe(16L, 100), kind, profile, 16).orElseThrow();
        assertEquals(16, parallel.operations());
        assertEquals(100, parallel.effectiveDuration());
    }

    @Test
    void controllerIsAnyFaceWithBottomAutoOutAndNoAutoIn() {
        ProcessingMachineSpec spec =
                ModMultiblockControllers.LARGE_ELECTROLYZER_VARIANT
                        .runtimeSpec();
        Direction front = Direction.NORTH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                spec.sidedIo().items().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                spec.sidedIo().items().resolve(front, Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                spec.sidedIo().fluids().resolve(front, front));
        assertFalse(spec.sidedIo().itemsChannel().hasAutoInput());
        assertFalse(spec.sidedIo().fluidsChannel().hasAutoInput());
        assertEquals(
                Direction.DOWN,
                spec.sidedIo().itemsChannel().autoOutputWorld(front).orElseThrow());
        assertEquals(
                Direction.DOWN,
                spec.sidedIo().fluidsChannel().autoOutputWorld(front).orElseThrow());
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().energy().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().energy().resolve(front, Direction.DOWN));

        ProcessingMachineSpec single = ModProcessingMachines.ELECTROLYZER;
        assertTrue(single.sidedIo().itemsChannel().hasAutoInput());
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                single.sidedIo().items().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                single.sidedIo().energy().resolve(front, Direction.UP));
    }

    @Test
    void structureSplitsBottomInAndTopOut() {
        var stream = LargeElectrolyzerProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/"
                        + "large_electrolyzer.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject palette = document.getAsJsonObject("palette");
        int energyInPorts = 0;
        int outPorts = 0;
        int controllers = 0;
        for (var row : document.getAsJsonArray("structure")) {
            JsonObject predicate = palette.getAsJsonObject(
                    row.getAsJsonObject().get("predicate").getAsString());
            if ("controller".equals(predicate.get("type").getAsString())) {
                controllers++;
            } else if (PortType.ITEM_FLUID_ENERGY_IN.serializedName().equals(
                    predicate.get("port").getAsString())) {
                energyInPorts++;
            } else if (PortType.ITEM_FLUID_OUT.serializedName().equals(
                    predicate.get("port").getAsString())) {
                outPorts++;
            }
        }
        assertEquals(8, energyInPorts);
        assertEquals(9, outPorts);
        assertEquals(1, controllers);
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
