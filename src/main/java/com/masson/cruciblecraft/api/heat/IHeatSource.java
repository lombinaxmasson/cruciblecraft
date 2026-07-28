package com.masson.cruciblecraft.api.heat;

/**
 * Pull-based heat source exposed to adjacent machines.
 *
 * <p>Consumers must extract heat explicitly. Simulation reports what the same
 * request could extract without mutating the source.
 */
public interface IHeatSource {
    /**
     * Extracts at most {@code maxAmount} HU, subject to this source's per-call
     * output rate and stored heat.
     *
     * @param maxAmount maximum requested HU
     * @param simulate {@code true} to query without changing the source
     * @return HU made available to the consumer
     */
    double extractHeat(double maxAmount, boolean simulate);

    /** Maximum HU this source can provide to one consumer tick. */
    float outputRate();

    /** Heat currently buffered by the source, in HU. */
    double storedHeat();

    default boolean hasHeat() {
        return storedHeat() > 0.0 && outputRate() > 0.0F;
    }
}
