package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ImplosionCompressorProfileTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void controllerProfileMatchesGt6() {
        TierProfile profile =
                ModMultiblockControllers.IMPLOSION_COMPRESSOR_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:implosion_compressor_profile",
                profile.tierBandId().toString());
        assertEquals(EnergyType.TIME, profile.energyType());
        assertEquals(1L, profile.inputMinimum());
        assertEquals(1L, profile.inputNominal());
        assertEquals(16L, profile.inputMaximum());
        assertEquals(16L, profile.energyCapacity());
        assertEquals(64, profile.parallelLimit());
        assertEquals(10_000, profile.efficiency());
    }

    @Test
    void specHasThreeInputsAndThreeOutputs() {
        ProcessingMachineSpec spec =
                ModProcessingMachines.IMPLOSION_COMPRESSOR;
        assertEquals(6, spec.items().slotCount());
        assertEquals(List.of(0, 1, 2), spec.items().inputs());
        assertEquals(List.of(3, 4, 5), spec.items().outputs());
        assertTrue(spec.fluids().inputs().isEmpty());
        assertTrue(spec.fluids().outputs().isEmpty());
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                spec.sidedIo().itemsChannel().resolve(
                        net.minecraft.core.Direction.NORTH,
                        net.minecraft.core.Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                spec.sidedIo().itemsChannel().resolve(
                        net.minecraft.core.Direction.NORTH,
                        net.minecraft.core.Direction.NORTH));
    }

    @Test
    void structureIsThreeByThreeByThreeWithTwentyFiveWalls() {
        var stream = ImplosionCompressorProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/implosion_compressor.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject palette = document.getAsJsonObject("palette");
        int ports = 0;
        int air = 0;
        int controllers = 0;
        int walls = 0;
        for (var row : document.getAsJsonArray("structure")) {
            JsonObject predicate = palette.getAsJsonObject(
                    row.getAsJsonObject().get("predicate").getAsString());
            if ("controller".equals(predicate.get("type").getAsString())) {
                controllers++;
            } else if ("air".equals(predicate.get("type").getAsString())) {
                air++;
            } else if (predicate.has("port")
                    && PortType.ITEM_FLUID_ENERGY.serializedName().equals(
                            predicate.get("port").getAsString())) {
                ports++;
            }
            if (predicate.has("block")
                    && "cruciblecraft:multiblock/dense_tungstensteel_wall".equals(
                            predicate.get("block").getAsString())) {
                walls++;
            }
        }
        assertEquals(27, document.getAsJsonArray("structure").size());
        assertEquals(25, ports);
        assertEquals(25, walls);
        assertEquals(1, air);
        assertEquals(1, controllers);
    }
}
