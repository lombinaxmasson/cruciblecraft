package com.masson.cruciblecraft.energy.transformer;

import com.masson.cruciblecraft.energy.EnergyPackets;

/** GT6 TileEntityBase11Bidirectional packet windows and lossless EU buffer. */
public final class TransformerEnergyStore {
    private final EnergyTransformerProfile profile;
    private boolean reversed;
    private long energy;

    public TransformerEnergyStore(EnergyTransformerProfile profile) {
        this.profile = profile;
    }

    public EnergyTransformerProfile profile() {
        return profile;
    }

    public boolean reversed() {
        return reversed;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public long stored() {
        return energy;
    }

    public long capacity() {
        return profile.capacity();
    }

    public void restore(long stored, boolean reversed) {
        this.reversed = reversed;
        energy = Math.max(0L, Math.min(profile.capacity(), stored));
    }

    public void clear() {
        energy = 0L;
    }

    public boolean acceptsSize(long size) {
        long magnitude = EnergyPackets.magnitude(size);
        return magnitude >= profile.acceptMin(reversed)
                && magnitude <= profile.acceptMax(reversed);
    }

    public long insert(long size, long amount, boolean simulate) {
        if (amount < 1L || !acceptsSize(size) || energy >= profile.capacity()) {
            return 0L;
        }
        long magnitude = EnergyPackets.magnitude(size);
        long packets = amount;
        while (packets > 1L
                && energy + EnergyPackets.units(magnitude, packets)
                        > profile.capacity()) {
            packets--;
        }
        long added = EnergyPackets.units(magnitude, packets);
        if (energy + added > profile.capacity()) {
            return 0L;
        }
        if (!simulate) {
            energy += added;
        }
        return packets;
    }

    public long extract(long size, long amount, boolean simulate) {
        long emit = profile.emitRec(reversed);
        if (amount < 1L || EnergyPackets.magnitude(size) != emit) {
            return 0L;
        }
        if (energy < emit) {
            return 0L;
        }
        long packets = Math.min(profile.packetMultiplier(reversed), amount);
        while (packets > 1L
                && energy < EnergyPackets.units(emit, packets)) {
            packets--;
        }
        long removed = EnergyPackets.units(emit, packets);
        if (removed <= 0L || energy < removed) {
            return 0L;
        }
        if (!simulate) {
            energy -= removed;
        }
        return packets;
    }

    public long outputSize() {
        long emit = profile.emitRec(reversed);
        return energy >= emit ? emit : 0L;
    }
}
