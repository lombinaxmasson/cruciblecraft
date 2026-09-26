package com.masson.cruciblecraft.energy.remainder;

/**
 * GT6 {@code MultiTileEntitySolarPanelElectric.generateEnergy}.
 * Twilight Forest half-output is absent here; that dimension is not loaded.
 */
public final class SolarGeneration {
    private SolarGeneration() {}

    public static long offer(
            long output,
            boolean sky,
            boolean thunder,
            boolean day,
            boolean rain,
            boolean rainfall) {
        if (!sky || thunder || output <= 0L) {
            return 0L;
        }
        if (day) {
            if (rain && rainfall) {
                return output / 8L;
            }
            return output;
        }
        if (rain && rainfall) {
            return 0L;
        }
        return output / 8L;
    }

    public static boolean active(long offer, long output) {
        return output > 0L && offer >= output / 8L && offer > 0L;
    }
}
