package com.masson.cruciblecraft.energy.steam;

/**
 * GT6 {@code MultiTileEntityLargeTurbineSteam.checkStructure2} wall bits.
 * Fluid-only, unlike {@code MultiTileEntityLargeTurbine}'s item+fluid gas hull.
 */
public enum SteamTurbineHatchRole {
    ENERGY_OUT,
    FLUID,
    FLUID_IN,
    FLUID_OUT,
    NOTHING;

    public boolean fill() {
        return this == FLUID || this == FLUID_IN;
    }

    public boolean drain() {
        return this == FLUID || this == FLUID_OUT;
    }

    public boolean energyOut() {
        return this == ENERGY_OUT;
    }
}
