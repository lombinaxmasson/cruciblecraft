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
        if (isSandingMachine(spec)) {
            return new Connection(Direction.UP, Direction.DOWN);
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
                        ModProcessingMachines.ROASTER.recipeMapId())
                || spec.recipeMapId().equals(
                        ModProcessingMachines.OVEN.recipeMapId());
    }

    private static boolean isSandingMachine(ProcessingMachineSpec spec) {
        return spec.recipeMapId().equals(
                ModProcessingMachines.SANDING.recipeMapId());
    }

    public record Connection(Direction providerOffset, Direction providerFace) {}
}
