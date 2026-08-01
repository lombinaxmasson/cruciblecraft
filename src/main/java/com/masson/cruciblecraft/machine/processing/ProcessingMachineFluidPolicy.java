package com.masson.cruciblecraft.machine.processing;

import net.neoforged.neoforge.fluids.FluidStack;

/** Production input-tank acceptance policy for configured processing specs. */
public final class ProcessingMachineFluidPolicy {
    private ProcessingMachineFluidPolicy() {}

    public static boolean accepts(
            ProcessingMachineSpec spec, int tank, FluidStack stack) {
        boolean inputTank = spec.fluids().inputs().stream()
                .anyMatch(value -> value.index() == tank);
        if (!inputTank || stack.isEmpty()) return false;
        return spec.requireRecipeMap().recipes().stream()
                .flatMap(recipe -> recipe.fluidInputs().stream())
                .anyMatch(input -> FluidStack.isSameFluidSameComponents(input, stack));
    }
}
