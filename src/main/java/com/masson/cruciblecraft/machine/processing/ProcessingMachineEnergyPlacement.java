package com.masson.cruciblecraft.machine.processing;

import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.Direction;

/** Production placement contract for adjacent-energy configured machines. */
public final class ProcessingMachineEnergyPlacement {
    private ProcessingMachineEnergyPlacement() {}

    public static Connection connection(ProcessingMachineSpec spec, Direction front) {
        if (spec == ModProcessingMachines.SMELTER) {
            return new Connection(Direction.DOWN, Direction.UP);
        }
        Direction provider = front.getOpposite();
        return new Connection(provider, provider.getOpposite());
    }

    public record Connection(Direction providerOffset, Direction providerFace) {}
}
