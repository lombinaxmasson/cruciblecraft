package com.masson.cruciblecraft.energy.cable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.ElectricalProperties;

class CableLoadStateTest {
    private static final ElectricalProperties TIN_WIRE =
            new ElectricalProperties(32L, 1L, 1L, false, true);

    @Test
    void rollsCurrentWattageIntoExactlyTheFollowingTick() {
        CableLoadState state = new CableLoadState();
        state.record(10L, 31L, 1L, false);

        assertEquals(31L, state.snapshot(10L).wattageThisTick());
        assertEquals(31L, state.snapshot(11L).wattageLast());
        assertEquals(0L, state.snapshot(12L).wattageLast());
        assertEquals(0L, state.snapshot(12L).amperesThisTick());
    }

    @Test
    void idleTickBookkeepingDoesNotRequestPersistenceOrClientSync() {
        CableLoadState state = new CableLoadState();

        assertEquals(
                new CableLoadState.Change(false, false),
                state.advance(10L));
        assertEquals(
                new CableLoadState.Change(false, false),
                state.advance(11L));
    }

    @Test
    void advanceReportsOnlyObservableTelemetryAndPersistentBurnChanges() {
        CableLoadState state = new CableLoadState();
        state.record(10L, 31L, 1L, false);

        assertEquals(
                new CableLoadState.Change(false, true),
                state.advance(11L));
        assertEquals(
                new CableLoadState.Change(false, true),
                state.advance(12L));
        assertEquals(
                new CableLoadState.Change(false, false),
                state.advance(13L));

        state.record(20L, 33L, 1L, true);
        assertEquals(
                new CableLoadState.Change(true, true),
                state.advance(532L));
    }

    @Test
    void postLossVoltageAndSameTickAmperageDriveOverload() {
        CableLoadState state = new CableLoadState();
        assertFalse(state.wouldOverload(4L, 32L, 1L, TIN_WIRE));
        assertTrue(state.wouldOverload(4L, 33L, 1L, TIN_WIRE));

        state.record(4L, 31L, 1L, false);
        assertTrue(state.wouldOverload(4L, 31L, 1L, TIN_WIRE));
        assertFalse(state.wouldOverload(5L, 31L, 1L, TIN_WIRE));
    }

    @Test
    void burnCounterAndDecayPhaseRoundTripWithoutUnloadReset() {
        CableLoadState state = new CableLoadState();
        state.record(100L, 33L, 1L, true);
        CableLoadState.Snapshot persisted = state.persistedSnapshot();

        CableLoadState restored = new CableLoadState();
        restored.restore(persisted);
        assertEquals(1, restored.burnCounter(100L));
        assertEquals(612L, restored.snapshot(100L).nextDecayTick());
        assertEquals(0, restored.burnCounter(612L));
    }

    @Test
    void sixteenthHitSchedulesObservableNextTickBurnout() {
        CableLoadState state = new CableLoadState();
        for (int hit = 0; hit < CableLoadState.BURN_LIMIT; hit++) {
            state.record(20L, 33L, 1L, true);
        }

        assertEquals(16, state.burnCounter(20L));
        assertFalse(state.shouldBurn(20L));
        assertTrue(state.shouldBurn(21L));

        CableLoadState restored = new CableLoadState();
        restored.restore(state.persistedSnapshot());
        assertTrue(restored.shouldBurn(21L));
    }

    @Test
    void pinnedGt6TierMaxContactDamageUsesCompleteVoltageTable() {
        assertEquals(0.0F, GT6VoltageTiers.contactDamage(8L));
        assertEquals(4.0F, GT6VoltageTiers.contactDamage(32L));
        assertEquals(8.0F, GT6VoltageTiers.contactDamage(128L));
        assertEquals(64.0F, GT6VoltageTiers.contactDamage(Long.MAX_VALUE));
    }

    @Test
    void segmentLossPreservesSignAndStopsAtTheExactCutoff() {
        assertEquals(
                31L,
                CableNetworkTraversal.applySegmentLoss(32L, 1L));
        assertEquals(
                -31L,
                CableNetworkTraversal.applySegmentLoss(-32L, 1L));
        assertEquals(
                0L,
                CableNetworkTraversal.applySegmentLoss(1L, 1L));
        assertEquals(
                Long.MIN_VALUE + 1L,
                CableNetworkTraversal.applySegmentLoss(
                        Long.MIN_VALUE, 1L));
    }

    @Test
    void zeroLossPreservesSignedPacketsAndNegativeLossIsRejected() {
        assertEquals(
                32L,
                CableNetworkTraversal.applySegmentLoss(32L, 0L));
        assertEquals(
                -32L,
                CableNetworkTraversal.applySegmentLoss(-32L, 0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> CableNetworkTraversal.applySegmentLoss(32L, -1L));

        ElectricalProperties zeroLoss =
                new ElectricalProperties(32L, 1L, 0L, false, true);
        assertEquals(0L, zeroLoss.lossPerMeter());
        assertThrows(
                IllegalArgumentException.class,
                () -> new ElectricalProperties(32L, 1L, -1L, false, true));
    }
}
