package com.masson.cruciblecraft.machine.component;

import com.masson.cruciblecraft.machine.CheckpointDecisions;

/** Mutable dirty/checkpoint state; world mutation remains owned by the host BE. */
public final class CheckpointTracker {
    private boolean dirty;
    private boolean syncPending;

    public void markDirty() {
        dirty = true;
        syncPending = true;
    }

    public void markSyncPending() {
        syncPending = true;
    }

    public boolean dirty() {
        return dirty;
    }

    public boolean shouldCheckpoint(long gameTime, long phaseKey, int interval) {
        return CheckpointDecisions.shouldCheckpoint(dirty, gameTime, phaseKey, interval);
    }

    public boolean shouldSync(
            boolean active,
            long gameTime,
            long phaseKey,
            int interval) {
        return CheckpointDecisions.shouldSync(
                active || syncPending,
                gameTime,
                phaseKey,
                interval);
    }

    public void checkpointed() {
        dirty = false;
    }

    public void synced() {
        syncPending = false;
    }
}
