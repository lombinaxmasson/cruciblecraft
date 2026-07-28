package com.masson.cruciblecraft.recipe;

public final class CokeOvenProcess {
    private CokeOvenProcess() {}

    public static boolean hasItemCapacity(
            int currentCount,
            int maximumCount,
            int addedCount,
            boolean compatible) {
        return currentCount >= 0
                && addedCount > 0
                && (currentCount == 0 || compatible)
                && currentCount + addedCount <= maximumCount;
    }

    public static boolean hasFluidCapacity(
            int currentAmount,
            int capacity,
            int addedAmount) {
        return currentAmount >= 0
                && addedAmount > 0
                && currentAmount + addedAmount <= capacity;
    }

    public static int advance(int progress, int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        return Math.min(duration, Math.max(0, progress) + 1);
    }
}
