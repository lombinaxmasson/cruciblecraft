package com.masson.cruciblecraft.content.storage;

/** Compact face count, matching the vanilla overlay when the number is small. */
public final class StorageCountFormat {
    private StorageCountFormat() {}

    public static String format(int stored) {
        if (stored < 10_000) {
            return Integer.toString(stored);
        }
        if (stored < 1_000_000) {
            return (stored / 1_000) + "k";
        }
        return (stored / 1_000_000) + "M";
    }

    /**
     * GT6 barrel/box fronts print the raw count (100% in red when full).
     * Standard mass storage keeps the compact overlay string.
     */
    public static String face(int stored, int capacity, String modelProfile) {
        if (capacity > 0 && stored >= capacity) {
            return "100%";
        }
        if ("mass_storage_barrel".equals(modelProfile)
                || "mass_storage_box".equals(modelProfile)) {
            return Integer.toString(stored);
        }
        return format(stored);
    }
}
