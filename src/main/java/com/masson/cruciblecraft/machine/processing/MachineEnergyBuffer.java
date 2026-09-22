package com.masson.cruciblecraft.machine.processing;

import com.masson.cruciblecraft.energy.EnergyPackets;

/** Overflow-safe buffered energy state with simulation-first consumption. */
public final class MachineEnergyBuffer {
    private final long capacity;
    private final long minimumPacket;
    private final long maxPacket;
    private long stored;

    public MachineEnergyBuffer(long capacity, long maxPacket) {
        this(capacity, 1L, maxPacket);
    }

    public MachineEnergyBuffer(
            long capacity,
            long minimumPacket,
            long maxPacket) {
        if (capacity <= 0L || maxPacket <= 0L) {
            throw new IllegalArgumentException("Energy limits must be positive");
        }
        if (minimumPacket <= 0L || minimumPacket > maxPacket) {
            throw new IllegalArgumentException(
                    "Energy packet window is invalid");
        }
        this.capacity = capacity;
        this.minimumPacket = minimumPacket;
        this.maxPacket = maxPacket;
    }

    public long insert(long size, long packets, boolean simulate) {
        long magnitude = EnergyPackets.magnitude(size);
        if (magnitude < minimumPacket
                || magnitude > maxPacket
                || packets <= 0L) {
            return 0L;
        }
        long accepted = Math.min(
                packets, EnergyPackets.packetsForUnits(size, capacity - stored));
        if (!simulate && accepted > 0L) {
            stored += EnergyPackets.units(size, accepted);
        }
        return accepted;
    }

    public boolean canConsume(long units) {
        return units >= 0L && stored >= units;
    }

    public boolean consume(long units) {
        if (!canConsume(units)) {
            return false;
        }
        stored -= units;
        return true;
    }

    public void restore(long value) {
        stored = Math.max(0L, Math.min(capacity, value));
    }

    public long stored() {
        return stored;
    }

    public long capacity() {
        return capacity;
    }
}
