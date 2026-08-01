package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.core.Direction;

/** Pure interaction gating shared by the production block and unit tests. */
public final class ProcessingMachineInteractions {
    private ProcessingMachineInteractions() {}

    public enum FluidTransfer {
        NONE,
        FILL,
        DRAIN
    }

    public static FluidTransfer fluidTransfer(
            ProcessingMachineSpec spec,
            Direction front,
            Direction clickedSide,
            boolean sneaking,
            boolean hasFluidContainer,
            boolean containerHasFluid) {
        if (sneaking
                || !hasFluidContainer
                || spec.fluids().inputs().isEmpty()
                || spec.sidedIo().fluids().resolve(front, clickedSide)
                        != ProcessingMachineSpec.CapabilityAccess.INPUT) {
            return FluidTransfer.NONE;
        }
        return containerHasFluid ? FluidTransfer.FILL : FluidTransfer.DRAIN;
    }

    public static boolean shouldTransferFluid(
            ProcessingMachineSpec spec,
            Direction front,
            Direction clickedSide,
            boolean sneaking,
            boolean hasFluidContainer) {
        return fluidTransfer(
                spec, front, clickedSide, sneaking, hasFluidContainer, true)
                != FluidTransfer.NONE;
    }
}
