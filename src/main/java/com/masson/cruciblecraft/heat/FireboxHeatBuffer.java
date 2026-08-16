package com.masson.cruciblecraft.heat;

import com.masson.cruciblecraft.energy.EnergyPackets;

/**
 * Pure heat-storage model used by the firebox.
 */
public final class FireboxHeatBuffer {
    public static final int MAX_EQUIVALENT_TICKS = 12_000;

    private long storedHeat;
    private long outputRate;
    private String fuelId;

    public FireboxHeatBuffer() {
        this(0L, 0L, "");
    }

    public FireboxHeatBuffer(long storedHeat, long outputRate, String fuelId) {
        if (storedHeat <= 0L || outputRate <= 0L) {
            clear();
            return;
        }
        this.outputRate = outputRate;
        this.storedHeat = Math.min(storedHeat, capacity(outputRate));
        this.fuelId = fuelId == null ? "" : fuelId;
    }

    public static FireboxHeatBuffer migrateLegacy(
            int burnTicks,
            float energyPerTick,
            String fuelId) {
        if (burnTicks <= 0 || !Float.isFinite(energyPerTick) || energyPerTick <= 0.0F) {
            return new FireboxHeatBuffer();
        }
        long rate = Math.max(1L, (long) Math.floor(energyPerTick));
        return new FireboxHeatBuffer(
                Math.max(0L, (long) Math.floor((double) burnTicks * energyPerTick)),
                rate,
                fuelId);
    }

    public boolean deposit(FuelDefinition fuel) {
        return deposit(fuel, fuel.energyPerTick(), 10_000);
    }

    public boolean deposit(
            FuelDefinition fuel,
            long configuredOutputRate,
            int efficiencyBps) {
        if (configuredOutputRate <= 0L
                || efficiencyBps <= 0
                || efficiencyBps > 10_000) {
            throw new IllegalArgumentException(
                    "Firebox profile output and efficiency are invalid");
        }
        if (hasHeat() && outputRate != configuredOutputRate) {
            return false;
        }
        long depositRate = hasHeat() ? outputRate : configuredOutputRate;
        long efficientFuel = Math.multiplyExact(
                fuel.totalEnergy(), efficiencyBps) / 10_000L;
        long accepted = Math.min(
                efficientFuel, capacity(depositRate) - storedHeat);
        if (accepted <= 0L) {
            return false;
        }
        if (!hasHeat()) {
            outputRate = depositRate;
            fuelId = fuel.id();
        }
        storedHeat += accepted;
        return true;
    }

    public long extract(long maxAmount, boolean simulate) {
        if (maxAmount <= 0L || !hasHeat()) {
            return 0L;
        }
        long extracted = Math.min(Math.min(maxAmount, outputRate), storedHeat);
        if (!simulate) {
            storedHeat -= extracted;
            if (storedHeat <= 0L) {
                clear();
            }
        }
        return extracted;
    }

    public long storedHeat() {
        return storedHeat;
    }

    public long outputRate() {
        return hasHeat() ? outputRate : 0L;
    }

    public String fuelId() {
        return fuelId;
    }

    public boolean hasHeat() {
        return storedHeat > 0L && outputRate > 0L;
    }

    public int equivalentTicks() {
        return hasHeat() ? (int) Math.min(
                Integer.MAX_VALUE,
                1L + (storedHeat - 1L) / outputRate) : 0;
    }

    public int equivalentSeconds() {
        return (int) Math.ceil(equivalentTicks() / 20.0);
    }

    public static long capacity(long outputRate) {
        return EnergyPackets.units(outputRate, MAX_EQUIVALENT_TICKS);
    }

    private void clear() {
        storedHeat = 0L;
        outputRate = 0L;
        fuelId = "";
    }
}
