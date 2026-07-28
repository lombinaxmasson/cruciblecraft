package com.masson.cruciblecraft.api.air;

public interface IAirSource {
    float outputRate();

    /**
     * Pulls at most {@code maxAmount}. Implementations must make simulation
     * side-effect free and enforce their shared per-tick output budget.
     */
    float extractAir(float maxAmount, boolean simulate);
}
