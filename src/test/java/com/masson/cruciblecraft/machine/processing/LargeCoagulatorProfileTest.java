package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeCoagulatorProfileTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void controllerProfileIsSourceBackedTu() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_COAGULATOR_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:large_coagulator_profile",
                profile.tierBandId().toString());
        assertEquals("cruciblecraft:large_coagulator", profile.materialId());
        assertEquals(EnergyType.TIME, profile.energyType());
        assertEquals(1L, profile.inputMinimum());
        assertEquals(1L, profile.inputNominal());
        assertEquals(16L, profile.inputMaximum());
        assertEquals(16L, profile.energyCapacity());
        assertEquals(64, profile.parallelLimit());
        assertEquals(10_000, profile.efficiency());
    }

    @Test
    void standardPolicyWithoutCheapOverclockOrParallelDuration() {
        MachineKindSpec kind = ModMultiblockControllers.LARGE_COAGULATOR_KIND;
        assertSame(ModProcessingMachines.COAGULATOR, kind.behavior());
        assertEquals(
                MachineKindSpec.OverclockPolicy.STANDARD,
                kind.overclockPolicy());
        assertFalse(kind.parallelDuration());
    }

    @Test
    void controllerPushesItemsAndFluidsDown() {
        ProcessingMachineSpec spec =
                ModMultiblockControllers.LARGE_COAGULATOR_VARIANT.runtimeSpec();
        Direction front = Direction.NORTH;
        assertEquals(Direction.DOWN, ProcessingMachineIoFaces.itemOutput(spec, front));
        assertEquals(Direction.DOWN, ProcessingMachineIoFaces.fluidOutput(spec, front));
        assertTrue(spec.sidedIo().itemsChannel().autoInputWorld(front).isEmpty());
        assertTrue(spec.sidedIo().fluidsChannel().autoInputWorld(front).isEmpty());
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                spec.sidedIo().itemsChannel().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                spec.sidedIo().fluidsChannel().resolve(front, Direction.DOWN));
    }

    @Test
    void structureIsFiveByFiveByTwoItemFluidWalls() {
        var stream = LargeCoagulatorProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/"
                        + "large_coagulator.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8)).getAsJsonObject();
        var structure = document.getAsJsonArray("structure");
        assertEquals(50, structure.size());
        JsonObject palette = document.getAsJsonObject("palette");
        int itemFluid = 0;
        int controllers = 0;
        for (var element : structure) {
            String key = element.getAsJsonObject().get("predicate").getAsString();
            JsonObject predicate = palette.getAsJsonObject(key);
            if ("controller".equals(predicate.get("type").getAsString())) {
                controllers++;
            } else {
                assertEquals(
                        PortType.ITEM_FLUID.serializedName(),
                        predicate.get("port").getAsString());
                itemFluid++;
            }
        }
        assertEquals(49, itemFluid);
        assertEquals(1, controllers);
    }
}
