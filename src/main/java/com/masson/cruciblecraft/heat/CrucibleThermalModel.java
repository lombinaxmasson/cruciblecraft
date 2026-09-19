package com.masson.cruciblecraft.heat;

public final class CrucibleThermalModel {
    /** GT6 {@code KG_PER_ENERGY}: 1 HU raises 100 kg of {@code getWeight} by 1 K. */
    public static final double KG_PER_ENERGY = 100.0;
    public static final int HOT_BUFFER_TICKS = 100;
    public static final int PASSIVE_DRIFT_INTERVAL = 10;
    public static final float AMBIENT_EPSILON = 0.01F;

    private CrucibleThermalModel() {}

    /**
     * Mirrors GT6's accumulator: incoming energy is retained, converted to
     * whole degrees by thermal mass, and any remainder stays for later ticks.
     *
     * @param thermalMass GT6 {@code OreDictMaterial.getWeight} kilograms
     *                    (casing plus contents)
     */
    public static StepResult step(
            float temperature,
            long storedEnergy,
            int cooldownTicks,
            long incomingEnergy,
            double thermalMass,
            float ambientTemperature) {
        long requiredEnergyPerDegree = requiredEnergyPerDegree(thermalMass);
        long acceptedIncoming = incomingEnergy;
        storedEnergy = addSignedEnergy(storedEnergy, acceptedIncoming);
        long conversions = requiredEnergyPerDegree == 0L
                ? 0L
                : storedEnergy / requiredEnergyPerDegree;

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
        if (conversions != 0L) {
            storedEnergy -= conversions * requiredEnergyPerDegree;
            temperature += conversions;
            cooldownTicks = HOT_BUFFER_TICKS;
        }
        if (acceptedIncoming == 0L && conversions == 0L
                && storedEnergy > 0L
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

    /**
     * GT6 {@code addMaterialStacks} mix:
     * {@code aTemperature + sign * units(|mT-aT|, w1+w2, w1)}.
     */
    public static float mixTemperature(
            float existingTemperature,
            double existingWeight,
            float addedTemperature,
            double addedWeight) {
        double totalWeight = existingWeight + addedWeight;
        if (!(totalWeight > 0.0)
                || !Float.isFinite(existingTemperature)
                || !Float.isFinite(addedTemperature)) {
            return existingTemperature;
        }
        long totalMass = (long) totalWeight;
        long existingMass = (long) existingWeight;
        if (totalMass <= 0L) {
            return existingTemperature;
        }
        long delta = (long) Math.abs(
                (double) existingTemperature - (double) addedTemperature);
        long retained = gt6Units(delta, totalMass, existingMass);
        float sign = existingTemperature > addedTemperature ? 1.0F : -1.0F;
        return addedTemperature + sign * retained;
    }

    /** {@code 1 + (long)(thermalMass / 100)} from GT6 crucible ticks. */
    public static long requiredEnergyPerDegree(double thermalMass) {
        if (!Double.isFinite(thermalMass) || thermalMass <= 0.0) {
            return 1L;
        }
        return 1L + (long) (thermalMass / KG_PER_ENERGY);
    }

    /** GT6 {@code UT.Code.units} without round-up. */
    private static long gt6Units(long amount, long originalUnit, long targetUnit) {
        if (targetUnit == 0L) {
            return 0L;
        }
        if (originalUnit == targetUnit || originalUnit == 0L) {
            return amount;
        }
        long source = originalUnit;
        long target = targetUnit;
        if (source % target == 0L) {
            source /= target;
            target = 1L;
        } else if (target % source == 0L) {
            target /= source;
            source = 1L;
        }
        if (target != 0L && amount > Long.MAX_VALUE / Math.abs(target)) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, (amount * target) / source);
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

    /** Signed accumulator so GT6 CU can drive negative conversions. */
    public static long addSignedEnergy(long first, long second) {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException overflow) {
            return second >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE + 1L;
        }
    }
}
