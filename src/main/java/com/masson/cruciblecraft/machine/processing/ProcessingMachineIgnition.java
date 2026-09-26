package com.masson.cruciblecraft.machine.processing;

/**
 * GT6 {@code NBT_NEEDS_IGNITION} for basic machines.
 *
 * <p>{@code MultiTileEntityBasicMachine} sets {@code mIgnited = 40} from an
 * igniter and will not apply a new recipe unless that timer is positive or
 * the machine is already active. A successful output refreshes the timer.
 */
public final class ProcessingMachineIgnition {
    /** gt6-source: MultiTileEntityBasicMachine.java onToolClick2 TOOL_igniter. */
    public static final int IGNITION_TICKS = 40;

    private ProcessingMachineIgnition() {}

    public static boolean requires(ProcessingMachineSpec spec) {
        return "burn_mixer".equals(spec.recipeMapId().getPath());
    }
}
