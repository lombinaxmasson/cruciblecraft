package com.masson.cruciblecraft.energy;

import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;

/**
 * Source row 10111 RU-to-EU conversion state.
 *
 * <p>The pinned GT6 converter accepts a 16/32/64 RU window, computes output at
 * the exact 22/32 ratio and has {@code NBT_WASTE_ENERGY=true}. Consequently a
 * blocked output consumes the pending RU instead of retaining a local EU
 * buffer. Audit counters make both successful and blocked conservation
 * equations explicit.
 */
public final class BronzeDynamoEnergy {
    public static final long INPUT_MINIMUM = 16L;
    public static final long INPUT_NOMINAL = 32L;
    public static final long INPUT_MAXIMUM = 64L;
    public static final long OUTPUT_MINIMUM = 11L;
    public static final long OUTPUT_NOMINAL = 22L;
    public static final long OUTPUT_MAXIMUM = 44L;
    public static final long BUFFER_CAPACITY = INPUT_MAXIMUM;

    private final MachineEnergyBuffer kinetic =
            new MachineEnergyBuffer(BUFFER_CAPACITY, INPUT_MAXIMUM);
    private long kineticConsumed;
    private long electricExtracted;
    private long conversionLoss;
    private boolean overloaded;

    public long insertKinetic(long size, long amount, boolean simulate) {
        long magnitude = EnergyPackets.magnitude(size);
        if (amount <= 0L || magnitude < INPUT_MINIMUM) {
            return 0L;
        }
        if (magnitude > INPUT_MAXIMUM) {
            if (!simulate) {
                overloaded = true;
                kinetic.restore(0L);
            }
            return amount;
        }
        return kinetic.insert(magnitude, amount, simulate);
    }

    /**
     * Current source-sized EU offer.
     *
     * <p>Minimum-window packets are accepted into the 64 RU source capacitor
     * and accumulate to the nominal 32 RU conversion quantum. This preserves
     * the exact 22/32 conversion instead of rounding each 16 RU packet.
     */
    public long outputSize() {
        long input = kinetic.stored();
        if (input < INPUT_NOMINAL) {
            return 0L;
        }
        return Math.min(
                OUTPUT_MAXIMUM,
                input * OUTPUT_NOMINAL / INPUT_NOMINAL);
    }

    public long extractElectric(long size, long maxAmount, boolean simulate) {
        long offered = outputSize();
        if (offered == 0L || size != offered || maxAmount <= 0L) {
            return 0L;
        }
        if (!simulate) {
            commitConversion(offered);
        }
        return 1L;
    }

    /**
     * Applies the source waste policy after a blocked emission attempt.
     *
     * @return whether an eligible RU window was dissipated
     */
    public boolean wasteBlockedInput() {
        if (outputSize() == 0L) {
            return false;
        }
        commitConversion(0L);
        return true;
    }

    private void commitConversion(long output) {
        long input = kinetic.stored();
        if (input < INPUT_NOMINAL
                || output < 0L
                || output > outputSize()) {
            throw new IllegalStateException(
                    "Dynamo conversion changed after simulation");
        }
        if (!kinetic.consume(input)) {
            throw new IllegalStateException(
                    "Simulated dynamo kinetic window disappeared");
        }
        kineticConsumed = EnergyPackets.add(kineticConsumed, input);
        electricExtracted = EnergyPackets.add(electricExtracted, output);
        conversionLoss = EnergyPackets.add(
                conversionLoss, input - output);
    }

    public State snapshot() {
        return new State(
                kinetic.stored(),
                kineticConsumed,
                electricExtracted,
                conversionLoss,
                overloaded);
    }

    public void restore(State state) {
        if (state == null) {
            throw new NullPointerException("state");
        }
        kinetic.restore(state.kinetic());
        kineticConsumed = Math.max(0L, state.kineticConsumed());
        electricExtracted = Math.max(0L, state.electricExtracted());
        conversionLoss = Math.max(0L, state.conversionLoss());
        overloaded = state.overloaded();
    }

    public long kineticStored() {
        return kinetic.stored();
    }

    public long kineticConsumed() {
        return kineticConsumed;
    }

    public long electricExtracted() {
        return electricExtracted;
    }

    public long conversionLoss() {
        return conversionLoss;
    }

    public boolean overloaded() {
        return overloaded;
    }

    public record State(
            long kinetic,
            long kineticConsumed,
            long electricExtracted,
            long conversionLoss,
            boolean overloaded) {}
}
