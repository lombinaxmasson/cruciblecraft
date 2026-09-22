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
import com.masson.cruciblecraft.content.block.LargeShredderGeometry;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeShredderProfileTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void controllerProfileIsSourceBackedRu() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_SHREDDER_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:large_shredder_profile",
                profile.tierBandId().toString());
        assertEquals(
                "cruciblecraft:large_shredder",
                profile.materialId());
        assertEquals(EnergyType.KINETIC_ROTATION, profile.energyType());
        assertEquals(512L, profile.inputMinimum());
        assertEquals(512L, profile.inputNominal());
        assertEquals(4_096L, profile.inputMaximum());
        assertEquals(4_096L, profile.energyCapacity());
        assertEquals(64, profile.parallelLimit());
        assertEquals(5_000, profile.efficiency());
    }

    @Test
    void specExposesOneInputTwelveOutputsAndBottomOnlyAutomaticOutput() {
        ProcessingMachineSpec spec = ModProcessingMachines.LARGE_SHREDDER;
        assertEquals(13, spec.items().slotCount());
        assertEquals(List.of(0), spec.items().inputs());
        assertEquals(
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
                spec.items().outputs());
        assertTrue(spec.fluids().inputs().isEmpty());
        assertTrue(spec.fluids().outputs().isEmpty());

        var items = spec.sidedIo().itemsChannel();
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                items.resolve(Direction.NORTH, Direction.NORTH));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                items.resolve(Direction.NORTH, Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                items.resolve(Direction.NORTH, Direction.UP));
        assertEquals(
                Direction.DOWN,
                items.autoOutputWorld(Direction.SOUTH).orElseThrow());
        assertTrue(items.autoInputWorld(Direction.SOUTH).isEmpty());

        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                spec.sidedIo().fluidsChannel().resolve(
                        Direction.NORTH, Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                spec.sidedIo().energyChannel().resolve(
                        Direction.NORTH, Direction.DOWN));
    }

    @Test
    void cheapPolicyAcceptsFullEutAndKeepsParallelDurationBehavior() {
        MachineKindSpec kind = ModMultiblockControllers.LARGE_SHREDDER_KIND;
        TierProfile profile =
                ModMultiblockControllers.LARGE_SHREDDER_VARIANT.tierBand();
        assertSame(ModProcessingMachines.LARGE_SHREDDER, kind.behavior());
        assertEquals(
                MachineKindSpec.OverclockPolicy.CHEAP,
                kind.overclockPolicy());
        assertTrue(kind.parallelDuration());

        assertTrue(MachineExecutionPlan.create(
                recipe(4_096L, 40), kind, profile, 1).isPresent());
        assertFalse(MachineExecutionPlan.create(
                recipe(4_097L, 40), kind, profile, 1).isPresent());

        MachineExecutionPlan parallel = MachineExecutionPlan.create(
                recipe(16L, 100), kind, profile, 64).orElseThrow();
        assertEquals(64, parallel.operations());
        assertEquals(204_800L, parallel.totalWork());
        assertEquals(400, parallel.effectiveDuration());
    }

    @Test
    void structureIsFiveByFiveBasinWithBlades() {
        var stream = LargeShredderProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/large_shredder.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject palette = document.getAsJsonObject("palette");
        int inPorts = 0;
        int outPorts = 0;
        int energy = 0;
        int controllers = 0;
        int cells = 0;
        for (var row : document.getAsJsonArray("structure")) {
            cells++;
            JsonObject predicate = palette.getAsJsonObject(
                    row.getAsJsonObject().get("predicate").getAsString());
            if ("controller".equals(predicate.get("type").getAsString())) {
                controllers++;
            } else if (predicate.has("port")
                    && PortType.ITEM_FLUID_IN.serializedName().equals(
                            predicate.get("port").getAsString())) {
                inPorts++;
            } else if (predicate.has("port")
                    && PortType.ITEM_FLUID_OUT.serializedName().equals(
                            predicate.get("port").getAsString())) {
                outPorts++;
            } else if (predicate.has("port")
                    && PortType.ENERGY_INPUT.serializedName().equals(
                            predicate.get("port").getAsString())) {
                energy++;
            }
        }
        assertEquals(75, cells);
        assertEquals(9, inPorts);
        assertEquals(24, outPorts);
        assertEquals(2, energy);
        assertEquals(1, controllers);
    }

    @Test
    void adjacentRuSourcesSitOutsideTheEnergyHoles() {
        BlockPos controller = BlockPos.ZERO;
        var neighbors = LargeShredderGeometry.adjacentEnergySources(
                controller, Direction.NORTH);
        assertEquals(2, neighbors.size());
        assertEquals(new BlockPos(-3, 1, 2), neighbors.get(0).position());
        assertEquals(Direction.EAST, neighbors.get(0).face());
        assertEquals(new BlockPos(3, 1, 2), neighbors.get(1).position());
        assertEquals(Direction.WEST, neighbors.get(1).face());

        var east = LargeShredderGeometry.adjacentEnergySources(
                controller, Direction.EAST);
        assertEquals(new BlockPos(-2, 1, -3), east.get(0).position());
        assertEquals(Direction.SOUTH, east.get(0).face());
        assertEquals(new BlockPos(-2, 1, 3), east.get(1).position());
        assertEquals(Direction.NORTH, east.get(1).face());
    }

    @Test
    void bladeWalkAabbMatchesGt6CenterMinusOneToPlusTwo() {
        BlockPos controller = BlockPos.ZERO;
        assertTrue(LargeShredderGeometry.insideBladeWalk(
                controller, Direction.NORTH, new net.minecraft.world.phys.Vec3(0.5, 1.5, 2.5)));
        assertTrue(LargeShredderGeometry.insideBladeWalk(
                controller, Direction.NORTH, new net.minecraft.world.phys.Vec3(-1.0, 1.0, 1.0)));
        assertTrue(LargeShredderGeometry.insideBladeWalk(
                controller, Direction.NORTH, new net.minecraft.world.phys.Vec3(2.0, 2.9, 4.0)));
        assertTrue(LargeShredderGeometry.insideBladeWalk(
                controller, Direction.NORTH, new net.minecraft.world.phys.Vec3(0.5, 0.5, 2.5)));
        assertFalse(LargeShredderGeometry.insideBladeWalk(
                controller, Direction.NORTH, new net.minecraft.world.phys.Vec3(-1.1, 1.5, 2.5)));
        assertEquals(0, LargeShredderGeometry.bladeDesign(Direction.NORTH, false));
        assertEquals(1, LargeShredderGeometry.bladeDesign(Direction.SOUTH, true));
        assertEquals(2, LargeShredderGeometry.bladeDesign(Direction.EAST, false));
        assertEquals(3, LargeShredderGeometry.bladeDesign(Direction.WEST, true));
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
