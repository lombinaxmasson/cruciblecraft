package com.masson.cruciblecraft.recipe;

public final class CrusherProgress {
    private CrusherProgress() {}
    public static int advance(int progress, int duration, boolean powered, boolean outputAvailable) {
        if (duration <= 0) throw new IllegalArgumentException("Duration must be positive");
        if (!powered || !outputAvailable) return Math.max(0, Math.min(progress, duration));
        return Math.min(duration, Math.max(0, progress) + 1);
    }
}
