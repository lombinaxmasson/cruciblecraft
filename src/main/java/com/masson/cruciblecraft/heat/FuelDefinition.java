package com.masson.cruciblecraft.heat;

import com.masson.cruciblecraft.energy.EnergyPackets;

/**
 * A snapshot of one firebox fuel profile. Energy is accumulated by the
 * crucible and converted to whole degrees using its current thermal mass,
 * matching GT6's HU accumulator model.
 */
public record FuelDefinition(String id, long energyPerTick, int burnTicks) {
    /**
     * One charcoal supplies enough total energy for a normal bronze batch, but
     * not enough to melt an empty clay crucible or a cold iron ingot.
     */
    public static final FuelDefinition CHARCOAL =
            new FuelDefinition("charcoal", 8L, 1_600);
    /**
     * Coal coke carries twice the total fuel value of charcoal and burns at the
     * 16 HU/t rate required by source-projected T5 smelter recipes.
     */
    public static final FuelDefinition COAL_COKE =
            new FuelDefinition("coal_coke", 16L, 1_600);

    public FuelDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Fuel id must not be blank");
        }
        if (energyPerTick <= 0L) {
            throw new IllegalArgumentException("Fuel energy_per_tick must be positive");
        }
        if (burnTicks <= 0) {
            throw new IllegalArgumentException("Fuel burn_ticks must be positive");
        }
    }

    public long totalEnergy() {
        return EnergyPackets.units(energyPerTick, burnTicks);
    }
}
