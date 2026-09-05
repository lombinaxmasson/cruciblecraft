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

    private final long inputMinimum;
    private final long inputNominal;
    private final long inputMaximum;
    private final long outputNominal;
    private final long outputMaximum;
    private final MachineEnergyBuffer kinetic;
    private long kineticConsumed;
    private long electricExtracted;
    private long conversionLoss;
    private boolean overloaded;

    public BronzeDynamoEnergy() {
        this(INPUT_MINIMUM, INPUT_NOMINAL, INPUT_MAXIMUM, OUTPUT_NOMINAL);
    }

    public BronzeDynamoEnergy(
            long inputMinimum,
            long inputNominal,
            long inputMaximum,
            long outputNominal) {
        if (inputMinimum <= 0L
                || inputMinimum > inputNominal
                || inputNominal > inputMaximum
                || outputNominal <= 0L) {
            throw new IllegalArgumentException("Dynamo window is invalid");
        }
        this.inputMinimum = inputMinimum;
        this.inputNominal = inputNominal;
        this.inputMaximum = inputMaximum;
        this.outputNominal = outputNominal;
        this.outputMaximum = outputNominal * inputMaximum / inputNominal;
        this.kinetic = new MachineEnergyBuffer(inputMaximum, inputMaximum);
    }

    public long insertKinetic(long size, long amount, boolean simulate) {
        long magnitude = EnergyPackets.magnitude(size);
        if (amount <= 0L || magnitude < inputMinimum) {
            return 0L;
        }
        if (magnitude > inputMaximum) {
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
        if (input < inputNominal) {
            return 0L;
        }
        return Math.min(
                outputMaximum,
                input * outputNominal / inputNominal);
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
        if (input < inputNominal
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
