package com.masson.cruciblecraft.machine.processing;

/**
 * Bounded menu progress derived from exact long-valued machine work.
 *
 * <p>The synchronized value is intentionally presentation-only. Exact progress,
 * duration, persistence, and diagnostics remain owned by the block entity.
 */
public final class ProcessingProgressSync {
    public static final int COMPLETE_PERMILLE = 1_000;

    private String recipeId = "";
    private int permille;
    private boolean completed;
    private long completedWork;
    private long requiredWork;

    public int update(
            String activeRecipeId,
            long completedWork,
            long requiredWork,
            boolean completedThisTick) {
        String nextRecipeId = activeRecipeId == null ? "" : activeRecipeId;
        if (completedThisTick) {
            recipeId = nextRecipeId;
            permille = COMPLETE_PERMILLE;
            completed = true;
            this.completedWork = Math.max(0L, completedWork);
            this.requiredWork = Math.max(0L, requiredWork);
            return permille;
        }
        if (nextRecipeId.isBlank() || requiredWork <= 0L) {
            reset();
            return permille;
        }

        int next = permille(completedWork, requiredWork);
        long boundedCompletedWork = Math.max(
                0L, Math.min(completedWork, requiredWork));
        boolean newCycle = completed
                || !nextRecipeId.equals(recipeId)
                || this.requiredWork != requiredWork
                || boundedCompletedWork < this.completedWork;
        if (newCycle) {
            recipeId = nextRecipeId;
            permille = next;
        } else {
            permille = Math.max(permille, next);
        }
        completed = false;
        this.completedWork = boundedCompletedWork;
        this.requiredWork = requiredWork;
        return permille;
    }

    public int value() {
        return permille;
    }

    public void reset() {
        recipeId = "";
        permille = 0;
        completed = false;
        completedWork = 0L;
        requiredWork = 0L;
    }

    /**
     * Returns floor(completed / required * 1000) without overflowing long.
     */
    public static int permille(long completedWork, long requiredWork) {
        if (completedWork <= 0L || requiredWork <= 0L) {
            return 0;
        }
        if (completedWork >= requiredWork) {
            return COMPLETE_PERMILLE;
        }

        int low = 0;
        int high = COMPLETE_PERMILLE - 1;
        while (low < high) {
            int candidate = (low + high + 1) >>> 1;
            if (completedWork >= minimumWorkFor(candidate, requiredWork)) {
                low = candidate;
            } else {
                high = candidate - 1;
            }
        }
        return low;
    }

    private static long minimumWorkFor(int permille, long requiredWork) {
        long thousands = requiredWork / COMPLETE_PERMILLE;
        long remainder = requiredWork % COMPLETE_PERMILLE;
        long partial = remainder * permille;
        return thousands * permille
                + partial / COMPLETE_PERMILLE
                + (partial % COMPLETE_PERMILLE == 0L ? 0L : 1L);
    }
}
