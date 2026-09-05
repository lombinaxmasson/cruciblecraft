package com.masson.cruciblecraft.logistics.displaycpu;

/**
 * SOURCE_BACKED used/capacity mapping from GT6 MultiTileEntityLogisticsCore
 * Display writeback.
 */
public final class DisplayCpuLevels {
    public static final int MAX_REDSTONE = 15;
    public static final int MAX_VISUAL = 10;

    private DisplayCpuLevels() {}

    public static int redstone(int used, int capacity) {
        if (used <= 0 || capacity <= 0) {
            return 0;
        }
        if (used >= capacity) {
            return MAX_REDSTONE;
        }
        int remainder = clamp(
                (int) (((long) capacity - used) * 14L / capacity),
                0,
                13);
        return 14 - remainder;
    }

    public static int visual(int used, int capacity) {
        if (used <= 0 || capacity <= 0) {
            return 0;
        }
        if (used >= capacity) {
            return MAX_VISUAL;
        }
        int remainder = clamp(
                (int) (((long) capacity - used) * 9L / capacity),
                0,
                8);
        return 9 - remainder;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
