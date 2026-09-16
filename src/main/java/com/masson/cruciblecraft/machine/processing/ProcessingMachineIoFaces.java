package com.masson.cruciblecraft.machine.processing;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.core.Direction;

/** World faces for GT6 auto I/O, hoppers, and GameTest energy injection. */
public final class ProcessingMachineIoFaces {
    private ProcessingMachineIoFaces() {}

    public static Direction itemInput(ProcessingMachineSpec spec, Direction front) {
        IoChannel channel = spec.sidedIo().itemsChannel();
        return channel.autoInputWorld(front)
                .or(() -> channel.firstInputWorld(front))
                .orElseThrow(() -> new IllegalStateException(
                        spec.id() + " has no item input face"));
    }

    public static Direction itemOutput(ProcessingMachineSpec spec, Direction front) {
        IoChannel channel = spec.sidedIo().itemsChannel();
        return channel.autoOutputWorld(front)
                .or(() -> channel.firstOutputWorld(front))
                .orElseThrow(() -> new IllegalStateException(
                        spec.id() + " has no item output face"));
    }

    public static Direction fluidInput(ProcessingMachineSpec spec, Direction front) {
        IoChannel channel = spec.sidedIo().fluidsChannel();
        return channel.autoInputWorld(front)
                .or(() -> channel.firstInputWorld(front))
                .orElseThrow(() -> new IllegalStateException(
                        spec.id() + " has no fluid input face"));
    }

    public static Direction fluidOutput(ProcessingMachineSpec spec, Direction front) {
        IoChannel channel = spec.sidedIo().fluidsChannel();
        return channel.autoOutputWorld(front)
                .or(() -> channel.firstOutputWorld(front))
                .orElseThrow(() -> new IllegalStateException(
                        spec.id() + " has no fluid output face"));
    }

    public static Direction energy(ProcessingMachineSpec spec, Direction front) {
        if (spec.energy().mode() == ProcessingMachineSpec.EnergyMode.ADJACENT
                && spec.energy().type() == EnergyType.HEAT) {
            return ProcessingMachineEnergyPlacement.connection(spec, front)
                    .providerOffset();
        }
        return spec.sidedIo().energyChannel()
                .firstInputWorld(front)
                .orElseThrow(() -> new IllegalStateException(
                        spec.id() + " has no energy face"));
    }
}
