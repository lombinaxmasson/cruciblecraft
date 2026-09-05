package com.masson.cruciblecraft.energy.battery;

import com.masson.cruciblecraft.energy.EnergyPackets;

/** GT6 TileEntityBase08Battery packet window and stored charge. */
public final class BatteryEnergyStore {
    private final EnergyBatteryProfile profile;
    private long energy;

    public BatteryEnergyStore(EnergyBatteryProfile profile) {
        this.profile = profile;
    }

    public EnergyBatteryProfile profile() {
        return profile;
    }

    public long stored() {
        return energy;
    }

    public long capacity() {
        return profile.capacity();
    }

    public void restore(long stored) {
        energy = Math.max(0L, Math.min(profile.capacity(), stored));
    }

    public boolean acceptsSize(long size) {
        long magnitude = EnergyPackets.magnitude(size);
        return magnitude >= profile.sizeMin() && magnitude <= profile.sizeMax();
    }

    public long insert(long size, long amount, boolean simulate) {
        if (amount < 1L || !acceptsSize(size) || energy >= profile.capacity()) {
            return 0L;
        }
        long magnitude = EnergyPackets.magnitude(size);
        long packets = Math.min(profile.inputSize(), amount);
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
        if (amount < 1L || !acceptsSize(size)) {
            return 0L;
        }
        long magnitude = EnergyPackets.magnitude(size);
        if (energy < magnitude) {
            return 0L;
        }
        long packets = Math.min(profile.inputSize(), amount);
        while (packets > 1L
                && energy < EnergyPackets.units(magnitude, packets)) {
            packets--;
        }
        long removed = EnergyPackets.units(magnitude, packets);
        if (removed <= 0L || energy < removed) {
            return 0L;
        }
        if (!simulate) {
            energy -= removed;
        }
        return packets;
    }

    public long outputSize() {
        return energy >= profile.inputSize() ? profile.inputSize() : 0L;
    }
}
