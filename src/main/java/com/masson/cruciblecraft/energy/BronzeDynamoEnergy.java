package com.masson.cruciblecraft.energy;

import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;

/**
 * Fixed bronze conversion state. Both sides are buffered so persistence can
 * snapshot conversion without inferring energy from an in-flight tick.
 */
public final class BronzeDynamoEnergy {
    public static final long PACKET_SIZE = 24L;
    public static final long BUFFER_CAPACITY = 1_024L;

    private final MachineEnergyBuffer kinetic =
            new MachineEnergyBuffer(BUFFER_CAPACITY, PACKET_SIZE);
    private final MachineEnergyBuffer electric =
            new MachineEnergyBuffer(BUFFER_CAPACITY, PACKET_SIZE);

    public long insertKinetic(long size, long amount, boolean simulate) {
        return EnergyPackets.magnitude(size) == PACKET_SIZE
                ? kinetic.insert(size, amount, simulate)
                : 0L;
    }

    /** Converts at most one complete fixed-size packet per server tick. */
    public boolean convertOnePacket() {
        if (!kinetic.canConsume(PACKET_SIZE)
                || electric.capacity() - electric.stored() < PACKET_SIZE) {
            return false;
        }
        if (!kinetic.consume(PACKET_SIZE)) {
            throw new IllegalStateException("Simulated dynamo kinetic packet disappeared");
        }
        long accepted = electric.insert(PACKET_SIZE, 1L, false);
        if (accepted != 1L) {
            throw new IllegalStateException("Dynamo electric buffer rejected converted packet");
        }
        return true;
    }

    public long extractElectric(long size, long maxAmount, boolean simulate) {
        if (size != PACKET_SIZE || maxAmount <= 0L) {
            return 0L;
        }
        long extracted = Math.min(
                maxAmount,
                EnergyPackets.packetsForUnits(PACKET_SIZE, electric.stored()));
        if (!simulate && extracted > 0L
                && !electric.consume(EnergyPackets.units(PACKET_SIZE, extracted))) {
            throw new IllegalStateException("Simulated dynamo electric packet disappeared");
        }
        return extracted;
    }

    public State snapshot() {
        return new State(kinetic.stored(), electric.stored());
    }

    public void restore(State state) {
        if (state == null) {
            throw new NullPointerException("state");
        }
        kinetic.restore(state.kinetic());
        electric.restore(state.electric());
    }

    public long kineticStored() {
        return kinetic.stored();
    }

    public long electricStored() {
        return electric.stored();
    }

    public record State(long kinetic, long electric) {}
}
