package com.masson.cruciblecraft.energy.vondagraagg;

/**
 * GT6 {@code UT.Code.bind8(min(energy, 4096) / 16)}: unsigned byte 0–255.
 */
public final class VonDaGraaggLogic {
    public static final long CAPACITY = 4096L;
    public static final long DRAIN = 4096L;
    public static final int MAX_RANGE = 255;

    private VonDaGraaggLogic() {}

    public static int range(long energy, boolean formed) {
        if (!formed) {
            return 0;
        }
        long scaled = Math.min(Math.max(0L, energy), CAPACITY) / 16L;
        return (int) Math.max(0L, Math.min(MAX_RANGE, scaled));
    }

    public static long afterDrain(long energy) {
        return Math.max(0L, energy - DRAIN);
    }
}
