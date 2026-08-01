package com.masson.cruciblecraft.machine.processing;

import java.util.List;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Pure status/tank projection used by configured menu synchronization. */
public final class ProcessingMachineDisplayData {
    private ProcessingMachineDisplayData() {}

    public static int statusIndex(ProcessingMachineSpec spec, String status) {
        String visible = status == null || status.isEmpty() ? "running" : status;
        return Math.max(0, spec.ui().statuses().indexOf(visible));
    }

    public static int tankAmount(List<FluidTank> tanks) {
        return tanks.stream().mapToInt(FluidTank::getFluidAmount).sum();
    }

    public static int tankCapacity(List<FluidTank> tanks) {
        return tanks.stream().mapToInt(FluidTank::getCapacity).sum();
    }
}
