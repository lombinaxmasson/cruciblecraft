package com.masson.cruciblecraft.machine.component;

import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;

/** Crucible thermal state; the host supplies mass and casing limits explicitly. */
public final class ThermalComponent {
    private final float ambientTemperature;
    private float temperature;
    private long pendingHeat;
    private long storedEnergy;
    private int cooldownTicks = CrucibleThermalModel.HOT_BUFFER_TICKS;
    private float displayTemperature;
    private float displayTargetTemperature;
    private int displayInterpolationTicks;
    private boolean displayInitialized;

    public ThermalComponent(float ambientTemperature) {
        if (!Float.isFinite(ambientTemperature)) {
            throw new IllegalArgumentException("Ambient temperature must be finite");
        }
        this.ambientTemperature = ambientTemperature;
        this.temperature = ambientTemperature;
        this.displayTemperature = ambientTemperature;
        this.displayTargetTemperature = ambientTemperature;
    }

    public long queueHeat(long size, long amount, boolean simulate) {
        return queueEnergy(size, amount, false, simulate);
    }

    public long queueCooling(long size, long amount, boolean simulate) {
        return queueEnergy(size, amount, true, simulate);
    }

    private long queueEnergy(long size, long amount, boolean cooling, boolean simulate) {
        if (size == 0L || amount <= 0L) {
            return 0L;
        }
        long units = EnergyPackets.units(size, amount);
        if (units <= 0L) {
            return 0L;
        }
        if (!simulate) {
            pendingHeat = cooling
                    ? CrucibleThermalModel.addSignedEnergy(pendingHeat, -units)
                    : CrucibleThermalModel.addSignedEnergy(pendingHeat, units);
        }
        return amount;
    }

    public long takePendingHeat() {
        long pending = pendingHeat;
        pendingHeat = 0L;
        return pending;
    }

    public void advance(long incomingEnergy, double thermalMass) {
        CrucibleThermalModel.StepResult result = CrucibleThermalModel.step(
                temperature,
                storedEnergy,
                cooldownTicks,
                incomingEnergy,
                thermalMass,
                ambientTemperature);
        temperature = result.temperature();
        storedEnergy = result.storedEnergy();
        cooldownTicks = result.cooldownTicks();
    }

    public void mixWith(float inputTemperature, double existingWeight, double addedWeight) {
        float safeInput = Float.isFinite(inputTemperature)
                ? inputTemperature
                : ambientTemperature;
        temperature = CrucibleThermalModel.mixTemperature(
                temperature,
                existingWeight,
                safeInput,
                addedWeight);
    }

    public boolean isQuiescent() {
        return CrucibleThermalModel.isQuiescent(
                temperature,
                storedEnergy,
                cooldownTicks,
                pendingHeat,
                ambientTemperature);
    }

    public void clientTick() {
        if (displayInterpolationTicks > 0) {
            displayTemperature = CrucibleThermalModel.interpolateDisplay(
                    displayTemperature,
                    displayTargetTemperature,
                    displayInterpolationTicks);
            displayInterpolationTicks--;
        } else {
            displayTemperature = displayTargetTemperature;
        }
    }

    public void restore(
            float savedTemperature,
            long savedPendingHeat,
            long savedStoredEnergy,
            int savedCooldownTicks,
            boolean clientSide) {
        temperature = Float.isFinite(savedTemperature)
                ? savedTemperature
                : ambientTemperature;
        pendingHeat = savedPendingHeat;
        storedEnergy = savedStoredEnergy;
        cooldownTicks = Math.max(
                0,
                Math.min(CrucibleThermalModel.HOT_BUFFER_TICKS, savedCooldownTicks));
        displayTargetTemperature = temperature;
        if (!displayInitialized || !clientSide) {
            displayTemperature = temperature;
            displayInterpolationTicks = 0;
            displayInitialized = true;
        } else {
            displayInterpolationTicks = 20;
        }
    }

    public float temperature(boolean clientSide) {
        return clientSide ? displayTemperature : temperature;
    }

    public float authoritativeTemperature() {
        return temperature;
    }

    public long pendingHeat() {
        return pendingHeat;
    }

    public long storedEnergy() {
        return storedEnergy;
    }

    public long totalStoredHeat() {
        return Math.max(0L, CrucibleThermalModel.addSignedEnergy(storedEnergy, pendingHeat));
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }
}
