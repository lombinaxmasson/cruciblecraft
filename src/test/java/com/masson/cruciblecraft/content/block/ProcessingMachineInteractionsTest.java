package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.Direction;

class ProcessingMachineInteractionsTest {
    @Test
    void fullAndPartialContainersPreferFillingInputSides() {
        for (boolean canAcceptFluid : new boolean[] {false, true}) {
            assertTransfer(
                    ProcessingMachineInteractions.FluidTransfer.FILL_INPUT,
                    ModProcessingMachines.BATH,
                    Direction.EAST,
                    true,
                    canAcceptFluid);
            assertTransfer(
                    ProcessingMachineInteractions.FluidTransfer.NONE,
                    ModProcessingMachines.BATH,
                    Direction.NORTH,
                    true,
                    false);
        }
    }

    @Test
    void emptyContainersDrainInputAndOutputSides() {
        assertTransfer(
                ProcessingMachineInteractions.FluidTransfer.DRAIN_INPUT,
                ModProcessingMachines.BATH,
                Direction.EAST,
                false,
                true);
        assertTransfer(
                ProcessingMachineInteractions.FluidTransfer.DRAIN_OUTPUT,
                ModProcessingMachines.BATH,
                Direction.DOWN,
                false,
                true);
    }

    @Test
    void partialContainersCanStillDrainOutputSides() {
        assertTransfer(
                ProcessingMachineInteractions.FluidTransfer.DRAIN_OUTPUT,
                ModProcessingMachines.BATH,
                Direction.DOWN,
                true,
                true);
    }

    @Test
    void outputOnlyFluidMachineStillSupportsPlayerDrain() {
        assertTransfer(
                ProcessingMachineInteractions.FluidTransfer.DRAIN_OUTPUT,
                ModProcessingMachines.SMELTER,
                Direction.WEST,
                false,
                true);
        assertTransfer(
                ProcessingMachineInteractions.FluidTransfer.NONE,
                ModProcessingMachines.SMELTER,
                Direction.NORTH,
                false,
                true);
    }

    @Test
    void sneakingAndNonContainersFallThrough() {
        assertEquals(
                ProcessingMachineInteractions.FluidTransfer.NONE,
                ProcessingMachineInteractions.fluidTransfer(
                        ModProcessingMachines.BATH,
                        Direction.NORTH,
                        Direction.WEST,
                        true,
                        true,
                        true,
                        false));
        assertEquals(
                ProcessingMachineInteractions.FluidTransfer.NONE,
                ProcessingMachineInteractions.fluidTransfer(
                        ModProcessingMachines.BATH,
                        Direction.NORTH,
                        Direction.WEST,
                        false,
                        false,
                        true,
                        false));
    }

    private static void assertTransfer(
            ProcessingMachineInteractions.FluidTransfer expected,
            com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec spec,
            Direction clickedSide,
            boolean hasFluid,
            boolean canAcceptFluid) {
        assertEquals(
                expected,
                ProcessingMachineInteractions.fluidTransfer(
                        spec,
                        Direction.NORTH,
                        clickedSide,
                        false,
                        true,
                        hasFluid,
                        canAcceptFluid));
    }
}
