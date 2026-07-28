package com.masson.cruciblecraft.heat;

/**
 * Pure heat-storage model used by the firebox.
 */
public final class FireboxHeatBuffer {
    public static final int MAX_EQUIVALENT_TICKS = 12_000;

    private double storedHeat;
    private float outputRate;
    private String fuelId;

    public FireboxHeatBuffer() {
        this(0.0, 0.0F, "");
    }

    public FireboxHeatBuffer(double storedHeat, float outputRate, String fuelId) {
        if (!Double.isFinite(storedHeat)
                || storedHeat <= 0.0
                || !Float.isFinite(outputRate)
                || outputRate <= 0.0F) {
            clear();
            return;
        }
        this.outputRate = outputRate;
        this.storedHeat = storedHeat;
        this.fuelId = fuelId == null ? "" : fuelId;
    }

    public static FireboxHeatBuffer migrateLegacy(
            int burnTicks,
            float energyPerTick,
            String fuelId) {
        if (burnTicks <= 0) {
            return new FireboxHeatBuffer();
        }
        return new FireboxHeatBuffer(
                (double) burnTicks * energyPerTick,
                energyPerTick,
                fuelId);
    }

    public boolean deposit(FuelDefinition fuel) {
        if (hasHeat() && Float.compare(outputRate, fuel.energyPerTick()) != 0) {
            return false;
        }
        float depositRate = hasHeat() ? outputRate : fuel.energyPerTick();
        double accepted = Math.min(fuel.totalEnergy(), capacity(depositRate) - storedHeat);
        if (accepted <= 0.0) {
            return false;
        }
        if (!hasHeat()) {
            outputRate = depositRate;
            fuelId = fuel.id();
        }
        storedHeat += accepted;
        return true;
    }

    public double extract(double maxAmount, boolean simulate) {
        if (!Double.isFinite(maxAmount) || maxAmount <= 0.0 || !hasHeat()) {
            return 0.0;
        }
        double extracted = Math.min(Math.min(maxAmount, outputRate), storedHeat);
        if (!simulate) {
            storedHeat -= extracted;
            if (storedHeat <= 0.0) {
                clear();
            }
        }
        return extracted;
    }

    public double storedHeat() {
        return storedHeat;
    }

    public float outputRate() {
        return hasHeat() ? outputRate : 0.0F;
    }

    public String fuelId() {
        return fuelId;
    }

    public boolean hasHeat() {
        return storedHeat > 0.0 && outputRate > 0.0F;
    }

    public int equivalentTicks() {
        return hasHeat() ? (int) Math.ceil(storedHeat / outputRate) : 0;
    }

    public int equivalentSeconds() {
        return (int) Math.ceil(equivalentTicks() / 20.0);
    }

    public static double capacity(float outputRate) {
        return (double) outputRate * MAX_EQUIVALENT_TICKS;
    }

    private void clear() {
        storedHeat = 0.0;
        outputRate = 0.0F;
        fuelId = "";
    }
}
