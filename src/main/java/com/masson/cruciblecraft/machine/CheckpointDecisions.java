package com.masson.cruciblecraft.machine;

/** Shared position-phased cadence decisions for block-entity persistence and sync. */
public final class CheckpointDecisions {
    private CheckpointDecisions() {}

    public static boolean shouldCheckpoint(
            boolean dirty,
            long gameTime,
            long positionKey,
            int interval) {
        return dirty && onPositionPhase(gameTime, positionKey, interval);
    }

    public static boolean shouldSync(
            boolean active,
            long gameTime,
            long positionKey,
            int interval) {
        return active && onPositionPhase(gameTime, positionKey, interval);
    }

    public static boolean onPositionPhase(long gameTime, long positionKey, int interval) {
        return interval > 0
                && Math.floorMod(gameTime, interval) == Math.floorMod(positionKey, interval);
    }
}
