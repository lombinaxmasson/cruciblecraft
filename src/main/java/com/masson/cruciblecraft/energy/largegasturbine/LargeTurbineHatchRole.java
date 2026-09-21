package com.masson.cruciblecraft.energy.largegasturbine;

/**
 * GT6 {@code MultiTileEntityMultiBlockPart} modes from
 * {@code MultiTileEntityLargeTurbine.checkStructure2}.
 */
public enum LargeTurbineHatchRole {
    ENERGY_OUT,
    ITEM_FLUID,
    ITEM_FLUID_IN,
    ITEM_FLUID_OUT,
    NOTHING;

    public boolean fill() {
        return this == ITEM_FLUID || this == ITEM_FLUID_IN;
    }

    public boolean drain() {
        return this == ITEM_FLUID || this == ITEM_FLUID_OUT;
    }

    public boolean energyOut() {
        return this == ENERGY_OUT;
    }
}
