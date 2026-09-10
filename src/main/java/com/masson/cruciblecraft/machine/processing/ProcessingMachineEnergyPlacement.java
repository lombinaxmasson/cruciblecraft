package com.masson.cruciblecraft.machine.processing;

import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.Direction;

/** Production placement contract for adjacent-energy configured machines. */
public final class ProcessingMachineEnergyPlacement {
    private ProcessingMachineEnergyPlacement() {}

    public static Connection connection(ProcessingMachineSpec spec, Direction front) {
        if (isHeatMachine(spec)) {
            return new Connection(Direction.DOWN, Direction.UP);
        }
        Direction provider = front.getOpposite();
        return new Connection(provider, provider.getOpposite());
    }

    private static boolean isHeatMachine(ProcessingMachineSpec spec) {
        return spec.recipeMapId().equals(
                        ModProcessingMachines.DISTILLERY.recipeMapId())
                || spec.recipeMapId().equals(
                        ModProcessingMachines.DRYING.recipeMapId())
                || spec.recipeMapId().equals(
                        ModProcessingMachines.SMELTER.recipeMapId())
                || spec.recipeMapId().equals(
                        ModProcessingMachines.MELTER.recipeMapId())
                || spec.recipeMapId().equals(
                        ModProcessingMachines.ROASTER.recipeMapId());
    }

    public record Connection(Direction providerOffset, Direction providerFace) {}
}
