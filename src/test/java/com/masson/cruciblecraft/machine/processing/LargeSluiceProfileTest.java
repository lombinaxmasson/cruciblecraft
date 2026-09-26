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
import com.masson.cruciblecraft.content.block.LargeSluiceGeometry;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeSluiceProfileTest {
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
                ModMultiblockControllers.LARGE_SLUICE_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:large_sluice_profile",
                profile.tierBandId().toString());
        assertEquals(
                "cruciblecraft:large_sluice",
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
    void specExposesWaterInputNineOutputsAndBottomFluidOutput() {
        ProcessingMachineSpec spec = ModProcessingMachines.LARGE_SLUICE;
        assertEquals(10, spec.items().slotCount());
        assertEquals(List.of(0), spec.items().inputs());
        assertEquals(
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
                spec.items().outputs());
        assertEquals(0, spec.fluids().inputs().getFirst().index());
        assertEquals(1, spec.fluids().outputs().getFirst().index());
        assertEquals(4_000, spec.fluids().inputs().getFirst().capacity());
        assertEquals(
                ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT,
                spec.fluids().outputs().getFirst().capacity());

        var items = spec.sidedIo().itemsChannel();
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                items.resolve(Direction.NORTH, Direction.NORTH));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                items.resolve(Direction.NORTH, Direction.DOWN));
        assertEquals(
                Direction.DOWN,
                items.autoOutputWorld(Direction.SOUTH).orElseThrow());
        assertTrue(items.autoInputWorld(Direction.SOUTH).isEmpty());

        var fluids = spec.sidedIo().fluidsChannel();
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                fluids.resolve(Direction.NORTH, Direction.DOWN));
        assertEquals(
                Direction.DOWN,
                fluids.autoOutputWorld(Direction.SOUTH).orElseThrow());
        assertTrue(fluids.autoInputWorld(Direction.SOUTH).isEmpty());
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                spec.sidedIo().energyChannel().resolve(
                        Direction.NORTH, Direction.DOWN));
    }

    @Test
    void cheapPolicyAcceptsSluiceFluidOutput() {
        MachineKindSpec kind = ModMultiblockControllers.LARGE_SLUICE_KIND;
        TierProfile profile =
                ModMultiblockControllers.LARGE_SLUICE_VARIANT.tierBand();
        assertSame(ModProcessingMachines.LARGE_SLUICE, kind.behavior());
        assertEquals(
                MachineKindSpec.OverclockPolicy.CHEAP,
                kind.overclockPolicy());
        assertTrue(kind.parallelDuration());

        GTRecipe recipe = new GTRecipe(
                List.of(Ingredient.of(Items.COBBLESTONE)),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(new ItemStack(Items.GRAVEL)),
                List.of(new FluidStack(Fluids.WATER, 250)),
                List.of(new FluidStack(Fluids.WATER, 250)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                240,
                4_096L,
                0L,
                true,
                Optional.empty());
        assertTrue(MachineExecutionPlan.create(
                recipe, kind, profile, 64).isPresent());
        assertFalse(MachineExecutionPlan.create(
                recipe, kind, profile, 65).isPresent());
    }

    @Test
    void structureIsThreeBySevenByThreeWithSourceExactCounts() {
        var stream = LargeSluiceProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/large_sluice.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject palette = document.getAsJsonObject("palette");
        int inPorts = 0;
        int outPorts = 0;
        int energy = 0;
        int controllers = 0;
        int cells = 0;
        int walls = 0;
        int parts = 0;
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
            if ("cruciblecraft:titanium/wall".equals(
                    predicate.get("block").getAsString())) {
                walls++;
            }
            if ("cruciblecraft:multiblock/sluice_part".equals(
                    predicate.get("block").getAsString())) {
                parts++;
            }
        }
        assertEquals(63, cells);
        assertEquals(3, inPorts);
        assertEquals(2, outPorts);
        assertEquals(2, energy);
        assertEquals(1, controllers);
        assertEquals(41, walls);
        assertEquals(21, parts);
    }

    @Test
    void adjacentRuSourcesSitOutsideTheEnergyHoles() {
        BlockPos controller = BlockPos.ZERO;
        var neighbors = LargeSluiceGeometry.adjacentEnergySources(
                controller, Direction.NORTH);
        assertEquals(2, neighbors.size());
        assertEquals(new BlockPos(-2, 1, 5), neighbors.get(0).position());
        assertEquals(Direction.EAST, neighbors.get(0).face());
        assertEquals(new BlockPos(2, 1, 5), neighbors.get(1).position());
        assertEquals(Direction.WEST, neighbors.get(1).face());

        assertEquals(0, LargeSluiceGeometry.partDesign(Direction.NORTH, false));
        assertEquals(4, LargeSluiceGeometry.partDesign(Direction.NORTH, true));
        assertEquals(3, LargeSluiceGeometry.partDesign(Direction.EAST, false));
        assertEquals(7, LargeSluiceGeometry.partDesign(Direction.EAST, true));
    }
}
