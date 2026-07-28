package com.masson.cruciblecraft.heat;

/**
 * A snapshot of one firebox fuel profile. Energy is accumulated by the
 * crucible and converted to whole degrees using its current thermal mass,
 * matching GT6's HU accumulator model.
 */
public record FuelDefinition(String id, float energyPerTick, int burnTicks) {
    /**
     * One charcoal supplies enough total energy for a normal bronze batch, but
     * not enough to melt an empty clay crucible or a cold iron ingot.
     */
    public static final FuelDefinition CHARCOAL =
            new FuelDefinition("charcoal", 8.0f, 1_600);
    /**
     * GT6 coal coke carries twice the fuel value of charcoal. Firebox output
     * remains unchanged, so the extra energy is delivered as a longer burn.
     */
    public static final FuelDefinition COAL_COKE =
            new FuelDefinition("coal_coke", 8.0f, 3_200);

    public FuelDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Fuel id must not be blank");
        }
        if (!Float.isFinite(energyPerTick) || energyPerTick <= 0.0f) {
            throw new IllegalArgumentException("Fuel energy_per_tick must be finite and positive");
        }
        if (burnTicks <= 0) {
            throw new IllegalArgumentException("Fuel burn_ticks must be positive");
        }
    }

    public double totalEnergy() {
        return (double) energyPerTick * burnTicks;
    }
}
