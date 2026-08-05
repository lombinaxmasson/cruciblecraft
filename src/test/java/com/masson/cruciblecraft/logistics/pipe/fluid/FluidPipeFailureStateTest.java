package com.masson.cruciblecraft.logistics.pipe.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.logistics.pipe.fluid
        .FluidPipeFailureState.Failure;

class FluidPipeFailureStateTest {
    @Test
    void hazardousFluidFailsOnFourthObservableEvent() {
        FluidPipeFailureState state = new FluidPipeFailureState();
        for (int event = 1;
                event < FluidPipeFailureState.FAILURE_LIMIT;
                event++) {
            state.record(Failure.OVER_TEMPERATURE, 100);
            assertEquals(Failure.NONE, state.pendingFailure());
        }
        state.record(Failure.OVER_TEMPERATURE, 100);
        assertEquals(
                Failure.OVER_TEMPERATURE, state.pendingFailure());
        assertEquals(
                FluidPipeFailureState.FAILURE_LIMIT,
                state.count(Failure.OVER_TEMPERATURE));
    }

    @Test
    void backpressureIsMeasuredWithoutDestroyingPipe() {
        FluidPipeFailureState state = new FluidPipeFailureState();
        state.record(Failure.BACKPRESSURE, 750);
        state.record(Failure.BACKPRESSURE, 250);
        assertEquals(1_000L, state.backpressureAmount());
        assertEquals(Failure.NONE, state.pendingFailure());
    }

    @Test
    void persistedSnapshotRestoresEveryFailureCounter() {
        FluidPipeFailureState source = new FluidPipeFailureState();
        source.record(Failure.CORROSION, 1);
        source.record(Failure.GAS_LEAK, 1);
        source.record(Failure.BACKPRESSURE, 42);

        FluidPipeFailureState restored = new FluidPipeFailureState();
        restored.restore(source.snapshot());

        assertEquals(source.snapshot(), restored.snapshot());
    }

    @Test
    void malformedPersistedFieldsDegradeIndependently() {
        FluidPipeFailureState.DecodeResult decoded =
                FluidPipeFailureState.decodeSnapshot(
                        3,
                        -1,
                        2,
                        -10L,
                        "garbage");

        assertTrue(decoded.recoveredInvalidData());
        assertEquals(3, decoded.rejectedFields());
        assertEquals(
                new FluidPipeFailureState.Snapshot(
                        3,
                        0,
                        2,
                        0L,
                        Failure.NONE),
                decoded.snapshot());
    }
}
