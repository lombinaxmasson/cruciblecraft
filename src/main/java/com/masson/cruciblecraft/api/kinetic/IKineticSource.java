package com.masson.cruciblecraft.api.kinetic;

public interface IKineticSource {
    KineticType type();

    /**
     * Extracts a magnitude-only kinetic packet. Simulation must be side-effect free.
     * The current push/pull stroke is exposed separately through {@link #strokeSign()}.
     */
    long extract(long maxAmount, boolean simulate);

    long outputRate();

    long stored();

    /** Current piston stroke direction: +1 pushing, -1 returning. */
    int strokeSign();

    default long signedAvailable() {
        return strokeSign() * Math.min(outputRate(), stored());
    }
}
