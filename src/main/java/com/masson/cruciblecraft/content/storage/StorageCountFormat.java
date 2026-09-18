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
}
