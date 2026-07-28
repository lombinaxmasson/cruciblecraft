package com.masson.cruciblecraft.machine;

/** Shared pure cadence decisions for block-entity persistence and sync. */
public final class CheckpointDecisions {
    private CheckpointDecisions() {}

    public static boolean shouldCheckpoint(boolean dirty, long gameTime, int interval) {
        return dirty && interval > 0 && Math.floorMod(gameTime, interval) == 0;
    }

    public static boolean shouldSync(boolean active, long gameTime, int interval) {
        return active && interval > 0 && Math.floorMod(gameTime, interval) == 0;
    }
}
