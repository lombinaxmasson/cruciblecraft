package com.masson.cruciblecraft.energy;

/** Overflow-safe arithmetic for GT6-style energy packets. */
public final class EnergyPackets {
    private EnergyPackets() {}

    /** Returns {@code abs(size)}, saturating the unrepresentable Long.MIN_VALUE case. */
    public static long magnitude(long size) {
        return size == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(size);
    }

    /** Returns the energy magnitude of the packets, saturating on overflow. */
    public static long units(long size, long amount) {
        long packetSize = magnitude(size);
        if (packetSize == 0L || amount <= 0L) {
            return 0L;
        }
        if (amount > Long.MAX_VALUE / packetSize) {
            return Long.MAX_VALUE;
        }
        return packetSize * amount;
    }

    /** Maximum whole packets of {@code size} that fit in {@code availableUnits}. */
    public static long packetsForUnits(long size, long availableUnits) {
        long packetSize = magnitude(size);
        return packetSize == 0L || availableUnits <= 0L
                ? 0L
                : availableUnits / packetSize;
    }

    /** Saturating addition for non-negative energy magnitudes. */
    public static long add(long first, long second) {
        long safeFirst = Math.max(0L, first);
        long safeSecond = Math.max(0L, second);
        return safeFirst > Long.MAX_VALUE - safeSecond
                ? Long.MAX_VALUE
                : safeFirst + safeSecond;
    }
}
