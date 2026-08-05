package com.masson.cruciblecraft.logistics.pipe.fluid;

import java.util.Locale;

/** Persistent deterministic fluid-pipe failure and backpressure counters. */
public final class FluidPipeFailureState {
    public static final int FAILURE_LIMIT = 4;

    private int overTemperatureEvents;
    private int corrosionEvents;
    private int gasLeakEvents;
    private long backpressureAmount;
    private Failure pendingFailure = Failure.NONE;

    public Change record(Failure failure, long amount) {
        if (failure == null || failure == Failure.NONE || amount <= 0L) {
            return Change.NONE;
        }
        switch (failure) {
            case OVER_TEMPERATURE -> overTemperatureEvents++;
            case CORROSION -> corrosionEvents++;
            case GAS_LEAK -> gasLeakEvents++;
            case BACKPRESSURE -> backpressureAmount =
                    Math.addExact(backpressureAmount, amount);
            case NONE -> {
                return Change.NONE;
            }
        }
        if (failure != Failure.BACKPRESSURE
                && count(failure) >= FAILURE_LIMIT) {
            pendingFailure = failure;
        }
        return new Change(true, true);
    }

    public int count(Failure failure) {
        return switch (failure) {
            case OVER_TEMPERATURE -> overTemperatureEvents;
            case CORROSION -> corrosionEvents;
            case GAS_LEAK -> gasLeakEvents;
            case BACKPRESSURE -> (int) Math.min(
                    Integer.MAX_VALUE, backpressureAmount);
            case NONE -> 0;
        };
    }

    public long backpressureAmount() {
        return backpressureAmount;
    }

    public Failure pendingFailure() {
        return pendingFailure;
    }

    public Snapshot snapshot() {
        return new Snapshot(
                overTemperatureEvents,
                corrosionEvents,
                gasLeakEvents,
                backpressureAmount,
                pendingFailure);
    }

    public void restore(Snapshot snapshot) {
        if (snapshot == null) {
            snapshot = Snapshot.EMPTY;
        }
        overTemperatureEvents = snapshot.overTemperatureEvents();
        corrosionEvents = snapshot.corrosionEvents();
        gasLeakEvents = snapshot.gasLeakEvents();
        backpressureAmount = snapshot.backpressureAmount();
        pendingFailure = snapshot.pendingFailure();
    }

    public enum Failure {
        NONE,
        OVER_TEMPERATURE,
        CORROSION,
        GAS_LEAK,
        BACKPRESSURE;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Failure decode(String value) {
            if (value == null || value.isBlank()) {
                return NONE;
            }
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    public record Change(
            boolean persistenceChanged, boolean observableChanged) {
        private static final Change NONE = new Change(false, false);
    }

    public static DecodeResult decodeSnapshot(
            int overTemperatureEvents,
            int corrosionEvents,
            int gasLeakEvents,
            long backpressureAmount,
            String pendingFailure) {
        int rejectedFields = 0;
        if (overTemperatureEvents < 0) {
            overTemperatureEvents = 0;
            rejectedFields++;
        }
        if (corrosionEvents < 0) {
            corrosionEvents = 0;
            rejectedFields++;
        }
        if (gasLeakEvents < 0) {
            gasLeakEvents = 0;
            rejectedFields++;
        }
        if (backpressureAmount < 0L) {
            backpressureAmount = 0L;
            rejectedFields++;
        }
        Failure decodedFailure;
        try {
            decodedFailure = Failure.decode(pendingFailure);
        } catch (IllegalArgumentException ignored) {
            decodedFailure = Failure.NONE;
            rejectedFields++;
        }
        return new DecodeResult(
                new Snapshot(
                        overTemperatureEvents,
                        corrosionEvents,
                        gasLeakEvents,
                        backpressureAmount,
                        decodedFailure),
                rejectedFields);
    }

    public record DecodeResult(
            Snapshot snapshot, int rejectedFields) {
        public DecodeResult {
            if (snapshot == null || rejectedFields < 0) {
                throw new IllegalArgumentException(
                        "Invalid fluid-pipe decode result");
            }
        }

        public boolean recoveredInvalidData() {
            return rejectedFields > 0;
        }
    }

    public record Snapshot(
            int overTemperatureEvents,
            int corrosionEvents,
            int gasLeakEvents,
            long backpressureAmount,
            Failure pendingFailure) {
        public static final Snapshot EMPTY =
                new Snapshot(0, 0, 0, 0L, Failure.NONE);

        public Snapshot {
            if (overTemperatureEvents < 0
                    || corrosionEvents < 0
                    || gasLeakEvents < 0
                    || backpressureAmount < 0L
                    || pendingFailure == null) {
                throw new IllegalArgumentException(
                        "Invalid fluid-pipe failure snapshot");
            }
        }
    }
}
