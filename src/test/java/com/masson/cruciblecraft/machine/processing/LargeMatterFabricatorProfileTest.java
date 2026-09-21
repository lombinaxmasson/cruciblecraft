package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Direction;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeMatterFabricatorProfileTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void controllerIoMatchesGt6Registration() {
        var spec = ModProcessingMachines.LARGE_MATTER_FABRICATOR;
        var items = spec.sidedIo().itemsChannel();
        var fluids = spec.sidedIo().fluidsChannel();
        var energy = spec.sidedIo().energyChannel();

        assertTrue(items.anySideInput());
        assertTrue(items.anySideOutput());
        assertTrue(fluids.anySideInput());
        assertTrue(fluids.anySideOutput());
        assertTrue(energy.anySideInput());
        assertTrue(items.autoInputWorld(Direction.NORTH).isEmpty());
        assertTrue(fluids.autoInputWorld(Direction.NORTH).isEmpty());
        assertEquals(
                Direction.DOWN,
                items.autoOutputWorld(Direction.NORTH).orElseThrow());
        assertEquals(
                Direction.DOWN,
                fluids.autoOutputWorld(Direction.NORTH).orElseThrow());
        assertEquals(EnergyType.QUANTUM, spec.energy().type());
    }

    @Test
    void controllerProfileKeepsGt6QuantumWindow() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_MATTER_FABRICATOR_VARIANT
                        .tierBand();
        assertEquals(1L, profile.inputMinimum());
        assertEquals(1L, profile.inputNominal());
        assertEquals(2_097_152L, profile.inputMaximum());
        assertEquals(2_097_152L, profile.energyCapacity());
        assertEquals(64, profile.parallelLimit());
        assertEquals(10_000, profile.efficiency());
    }
}
