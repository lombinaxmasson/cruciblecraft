package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.core.Direction;

/** Pure interaction gating shared by the production block and unit tests. */
public final class ProcessingMachineInteractions {
    private ProcessingMachineInteractions() {}

    public enum FluidTransfer {
        NONE,
        FILL_INPUT,
        DRAIN_INPUT,
        DRAIN_OUTPUT
    }

    public static FluidTransfer fluidTransfer(
            ProcessingMachineSpec spec,
            Direction front,
            Direction clickedSide,
            boolean sneaking,
            boolean hasFluidContainer,
            boolean containerHasFluid) {
        return fluidTransfer(
                spec,
                front,
                clickedSide,
                sneaking,
                hasFluidContainer,
                containerHasFluid,
                !containerHasFluid);
    }

    public static FluidTransfer fluidTransfer(
            ProcessingMachineSpec spec,
            Direction front,
            Direction clickedSide,
            boolean sneaking,
            boolean hasFluidContainer,
            boolean containerHasFluid,
            boolean containerCanAcceptFluid) {
        if (sneaking
                || !hasFluidContainer) {
            return FluidTransfer.NONE;
        }
        ProcessingMachineSpec.CapabilityAccess access =
                spec.sidedIo().fluids().resolve(front, clickedSide);
        if (access == ProcessingMachineSpec.CapabilityAccess.INPUT
                && !spec.fluids().inputs().isEmpty()) {
            if (containerHasFluid) {
                return FluidTransfer.FILL_INPUT;
            }
            if (containerCanAcceptFluid) {
                return FluidTransfer.DRAIN_INPUT;
            }
            return FluidTransfer.NONE;
        }
        return containerCanAcceptFluid
                && access == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                && !spec.fluids().outputs().isEmpty()
                ? FluidTransfer.DRAIN_OUTPUT
                : FluidTransfer.NONE;
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
