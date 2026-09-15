package com.masson.cruciblecraft.machine.generation;

import com.masson.cruciblecraft.energy.EnergyPackets;

/**
 * Unit-buffered generator output with an independent source packet size.
 *
 * <p>Recipe power is stored as units, so a recipe whose {@code abs(eut)} is
 * larger than the emitted packet never loses the remainder.
 */
public final class FuelGeneratorEnergy {
    private final long packetSize;
    private final long capacity;
    private long stored;
    private long generated;
    private long extracted;

    public FuelGeneratorEnergy(long packetSize, long capacity) {
        if (packetSize <= 0L || capacity < packetSize) {
            throw new IllegalArgumentException(
                    "Generator packet and capacity must be positive");
        }
        this.packetSize = packetSize;
        this.capacity = capacity;
    }

    public boolean canGenerate(long units) {
        return units > 0L && units <= capacity - stored;
    }

    public void generate(long units) {
        if (!canGenerate(units)) {
            throw new IllegalStateException(
                    "Simulated generator energy room disappeared");
        }
        stored += units;
        generated = EnergyPackets.add(generated, units);
    }

    public long extract(long size, long maximum, boolean simulate) {
        if (size != packetSize || maximum <= 0L) {
            return 0L;
        }
        long packets = Math.min(
                maximum,
                EnergyPackets.packetsForUnits(packetSize, stored));
        if (!simulate && packets > 0L) {
            long units = EnergyPackets.units(packetSize, packets);
            stored -= units;
            extracted = EnergyPackets.add(extracted, units);
        }
        return packets;
    }

    /**
     * GT6 burning-box / hot-fluid emit: subtract offered units even when the
     * neighbor accepted fewer packets (heat dumped into air or overheat).
     */
    public long discardUnits(long units) {
        if (units <= 0L || stored <= 0L) {
            return 0L;
        }
        long removed = Math.min(stored, units);
        stored -= removed;
        extracted = EnergyPackets.add(extracted, removed);
        return removed;
    }

    public void restore(State state) {
        if (state == null) {
            throw new NullPointerException("state");
        }
        stored = Math.max(0L, Math.min(capacity, state.stored()));
        generated = Math.max(0L, state.generated());
        extracted = Math.max(0L, state.extracted());
    }

    public State snapshot() {
        return new State(stored, generated, extracted);
    }

    public long packetSize() {
        return packetSize;
    }

    public long capacity() {
        return capacity;
    }

    public long stored() {
        return stored;
    }

    public long generated() {
        return generated;
    }

    public long extracted() {
        return extracted;
    }

    public record State(long stored, long generated, long extracted) {}
}
