package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.masson.cruciblecraft.machine.processing.prep.ClusterMillPrepSpec;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class Gt6SidedIoTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void everyLiveHostHasAGt6Profile() {
        Stream.concat(
                        Stream.of(ModProcessingMachines.CRUSHER),
                        Stream.concat(
                                ModProcessingMachines.CONFIGURED_MACHINES.stream(),
                                ModProcessingMachines.MULTIBLOCK_MENU_HOSTS.stream()))
                .forEach(ProcessingMachineIoAssertions::assertMatchesProfile);
        ProcessingMachineIoAssertions.assertMatchesProfile(ClusterMillPrepSpec.SPEC);
    }

    @Test
    void crusherUsesTopInBottomOutAndBackEnergy() {
        ProcessingMachineSpec spec = ModProcessingMachines.CRUSHER;
        Direction front = Direction.NORTH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().items().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                spec.sidedIo().items().resolve(front, Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                spec.sidedIo().items().resolve(front, front));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                spec.sidedIo().items().resolve(front, null));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().energy().resolve(front, Direction.SOUTH));
        assertEquals(Direction.UP, ProcessingMachineIoFaces.itemInput(spec, front));
        assertEquals(Direction.DOWN, ProcessingMachineIoFaces.itemOutput(spec, front));
        assertEquals(Direction.SOUTH, ProcessingMachineIoFaces.energy(spec, front));
    }

    @Test
    void latheUsesPlayerLeftInRightOutAndBottomEnergy() {
        ProcessingMachineSpec spec = ModProcessingMachines.LATHE;
        Direction front = Direction.NORTH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().items().resolve(front, Direction.EAST));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                spec.sidedIo().items().resolve(front, Direction.WEST));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().energy().resolve(front, Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                spec.sidedIo().energy().resolve(front, Direction.SOUTH));
    }

    @Test
    void bathOverlappingSidesAreBoth() {
        ProcessingMachineSpec spec = ModProcessingMachines.BATH;
        Direction front = Direction.NORTH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().fluids().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                spec.sidedIo().fluids().resolve(front, Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().items().resolve(front, Direction.EAST));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                spec.sidedIo().items().resolve(front, Direction.WEST));
    }

    @Test
    void autoOutputFluidSideRefusesFillWhileEnabled() {
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                ProcessingMachineAutoIo.overlayFluidAccess(
                        ProcessingMachineSpec.CapabilityAccess.INPUT,
                        Direction.DOWN,
                        Direction.DOWN,
                        false));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                ProcessingMachineAutoIo.overlayFluidAccess(
                        ProcessingMachineSpec.CapabilityAccess.BOTH,
                        Direction.DOWN,
                        Direction.DOWN,
                        false));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                ProcessingMachineAutoIo.overlayFluidAccess(
                        ProcessingMachineSpec.CapabilityAccess.BOTH,
                        Direction.DOWN,
                        Direction.DOWN,
                        true));
    }

    @Test
    void crusherInventoryTooltipNamesAutoFaces() {
        List<Component> lines = ProcessingMachineIoTooltips.lines(
                ModProcessingMachines.CRUSHER);
        List<String> keys = lines.stream().map(Gt6SidedIoTest::key).toList();
        assertTrue(keys.contains("tooltip.cruciblecraft.machine.energy_in"));
        assertTrue(keys.contains("tooltip.cruciblecraft.machine.items_in"));
        assertTrue(keys.contains("tooltip.cruciblecraft.machine.items_out"));
        assertTrue(keys.contains("tooltip.cruciblecraft.machine.screwdriver"));
        assertTrue(keys.contains("tooltip.cruciblecraft.machine.monkey_wrench.auto_in"));
        assertTrue(keys.contains("tooltip.cruciblecraft.machine.monkey_wrench.auto_out"));
        assertFalse(keys.contains("tooltip.cruciblecraft.machine.fluids_in"));
        String itemsIn = flatten(lines.get(1));
        assertTrue(itemsIn.contains("tooltip.cruciblecraft.machine.face.top"));
        assertTrue(itemsIn.contains("tooltip.cruciblecraft.machine.auto"));
    }

    private static String key(Component component) {
        return component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable
                ? translatable.getKey()
                : component.getString();
    }

    private static String flatten(Component component) {
        StringBuilder text = new StringBuilder(key(component));
        for (Component sibling : component.getSiblings()) {
            text.append(' ').append(flatten(sibling));
        }
        return text.toString();
    }
}
