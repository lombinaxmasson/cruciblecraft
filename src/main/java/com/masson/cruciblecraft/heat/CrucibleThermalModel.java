package com.masson.cruciblecraft.heat;

public final class CrucibleThermalModel {
    public static final double GRAMS_PER_ENERGY = 200.0;
    public static final int HOT_BUFFER_TICKS = 100;
    public static final int PASSIVE_DRIFT_INTERVAL = 10;

    private CrucibleThermalModel() {}

    /**
     * Mirrors GT6's accumulator: incoming energy is retained, converted to
     * whole degrees by thermal mass, and any remainder stays for later ticks.
     */
    public static StepResult step(
            float temperature,
            double storedEnergy,
            int cooldownTicks,
            float incomingEnergy,
            double weightGrams,
            float ambientTemperature) {
        long requiredEnergyPerDegree =
                1L + (long) Math.max(0.0, weightGrams / GRAMS_PER_ENERGY);
        storedEnergy += Math.max(0.0f, incomingEnergy);
        long conversions = (long) (storedEnergy / requiredEnergyPerDegree);

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
        if (conversions != 0L) {
            storedEnergy -= conversions * requiredEnergyPerDegree;
            temperature += conversions;
            cooldownTicks = HOT_BUFFER_TICKS;
        }
        if (cooldownTicks <= 0) {
            cooldownTicks = PASSIVE_DRIFT_INTERVAL;
            if (temperature > ambientTemperature) {
                temperature--;
            } else if (temperature < ambientTemperature) {
                temperature++;
            }
        }

        // Exact GT6 crucible behavior (MultiTileEntityCrucible lines 193/365):
        // ambient may establish a floor, but that floor is capped at 200 °C.
        temperature = Math.max(temperature, Math.min(200.0f, ambientTemperature));
        return new StepResult(temperature, storedEnergy, cooldownTicks);
    }

    public static float mixTemperature(
            float existingTemperature,
            double existingWeight,
            float addedTemperature,
            double addedWeight) {
        double totalWeight = existingWeight + addedWeight;
        if (totalWeight <= 0.0) {
            return existingTemperature;
        }
        return (float) ((existingTemperature * existingWeight
                + addedTemperature * addedWeight) / totalWeight);
    }

    /** GT6 boiling removes the complete material entry at its boiling point. */
    public static boolean shouldBoil(float temperature, double boilingPoint) {
        return Float.isFinite(temperature)
                && Double.isFinite(boilingPoint)
                && temperature >= boilingPoint;
    }

    /**
     * Display-only interpolation. It cannot alter authoritative temperature,
     * stored energy, cooldown, composition, or process state.
     */
    public static float interpolateDisplay(float displayed, float target, int ticksRemaining) {
        return ticksRemaining <= 0 ? target : displayed + (target - displayed) / ticksRemaining;
    }

    public record StepResult(float temperature, double storedEnergy, int cooldownTicks) {}
}
