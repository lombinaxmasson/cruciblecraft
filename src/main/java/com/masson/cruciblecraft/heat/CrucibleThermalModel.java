package com.masson.cruciblecraft.heat;

import com.masson.cruciblecraft.energy.EnergyPackets;

public final class CrucibleThermalModel {
    public static final double GRAMS_PER_ENERGY = 200.0;
    public static final int HOT_BUFFER_TICKS = 100;
    public static final int PASSIVE_DRIFT_INTERVAL = 10;
    public static final float AMBIENT_EPSILON = 0.01F;

    private CrucibleThermalModel() {}

    /**
     * Mirrors GT6's accumulator: incoming energy is retained, converted to
     * whole degrees by thermal mass, and any remainder stays for later ticks.
     */
    public static StepResult step(
            float temperature,
            long storedEnergy,
            int cooldownTicks,
            long incomingEnergy,
            double weightGrams,
            float ambientTemperature) {
        long requiredEnergyPerDegree =
                1L + (long) Math.max(0.0, weightGrams / GRAMS_PER_ENERGY);
        long acceptedIncoming = Math.max(0L, incomingEnergy);
        storedEnergy = EnergyPackets.add(storedEnergy, acceptedIncoming);
        long conversions = (long) (storedEnergy / requiredEnergyPerDegree);

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
        if (conversions != 0L) {
            storedEnergy -= conversions * requiredEnergyPerDegree;
            temperature += conversions;
            cooldownTicks = HOT_BUFFER_TICKS;
        }
        if (acceptedIncoming == 0L && conversions == 0L
                && storedEnergy < requiredEnergyPerDegree) {
            storedEnergy = 0L;
        }

        if (atAmbient(temperature, ambientTemperature)) {
            temperature = ambientTemperature;
            cooldownTicks = 0;
        } else if (cooldownTicks <= 0) {
            temperature = driftToward(temperature, ambientTemperature);
            if (atAmbient(temperature, ambientTemperature)) {
                temperature = ambientTemperature;
                cooldownTicks = 0;
            } else {
                cooldownTicks = PASSIVE_DRIFT_INTERVAL;
            }
        }

        // Exact GT6 crucible behavior (MultiTileEntityCrucible lines 193/365):
        // ambient may establish a floor, but that floor is capped at 200 °C.
        temperature = Math.max(temperature, Math.min(200.0f, ambientTemperature));
        return new StepResult(temperature, storedEnergy, cooldownTicks);
    }

    /** Whether authoritative thermal state has fully settled and can stop ticking. */
    public static boolean isQuiescent(
            float temperature,
            long storedEnergy,
            int cooldownTicks,
            long incomingEnergy,
            float ambientTemperature) {
        return atAmbient(temperature, ambientTemperature)
                && storedEnergy == 0L
                && cooldownTicks == 0
                && incomingEnergy == 0L;
    }

    public static boolean atAmbient(float temperature, float ambientTemperature) {
        return Float.isFinite(temperature)
                && Float.isFinite(ambientTemperature)
                && Math.abs(temperature - ambientTemperature) <= AMBIENT_EPSILON;
    }

    private static float driftToward(float temperature, float ambientTemperature) {
        if (!Float.isFinite(temperature) || !Float.isFinite(ambientTemperature)) {
            return ambientTemperature;
        }
        float difference = ambientTemperature - temperature;
        return temperature + Math.copySign(Math.min(1.0F, Math.abs(difference)), difference);
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

    public record StepResult(float temperature, long storedEnergy, int cooldownTicks) {}
}
