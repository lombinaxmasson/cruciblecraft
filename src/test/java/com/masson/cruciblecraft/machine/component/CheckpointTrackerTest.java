package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CheckpointTrackerTest {
    @Test
    void checkpointsOnlyDirtyStateOnItsPositionPhase() {
        CheckpointTracker tracker = new CheckpointTracker();
        assertFalse(tracker.shouldCheckpoint(7, 7, 20));

        tracker.markDirty();
        assertFalse(tracker.shouldCheckpoint(6, 7, 20));
        assertTrue(tracker.shouldCheckpoint(7, 7, 20));
        assertTrue(tracker.shouldSync(false, 7, 7, 20));
        tracker.checkpointed();
        assertFalse(tracker.dirty());
    }

    @Test
    void syncCadenceDoesNotDependOnDirtyState() {
        CheckpointTracker tracker = new CheckpointTracker();
        assertTrue(tracker.shouldSync(true, 27, 7, 20));
        assertFalse(tracker.shouldSync(false, 27, 7, 20));

        tracker.markSyncPending();
        assertTrue(tracker.shouldSync(false, 27, 7, 20));
        tracker.synced();
        assertFalse(tracker.shouldSync(false, 27, 7, 20));
    }
}
