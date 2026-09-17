package com.masson.cruciblecraft.steam;

/**
 * GT6 {@code MultiTileEntityEngineSteam} {@code tOutput=(mOutput*(mState+1))/16}.
 *
 * <p>Bronze nominal packet is 12 KU. Inclusive half-to-double is 6–24 when the
 * active gate uses {@code tOutput*2 >= nominal}.
 */
public final class SteamEngineKuCurve {
    private SteamEngineKuCurve() {}

    public static int visualState(long stored, long capacity) {
        if (capacity <= 0L || stored <= 0L) {
            return 0;
        }
        long scaled = stored * 32L / capacity;
        if (scaled <= 0L) {
            return 0;
        }
        return (int) Math.min(31L, scaled);
    }

    public static long outputKu(long nominal, int state) {
        if (nominal <= 0L) {
            throw new IllegalArgumentException("Nominal KU must be positive");
        }
        if (state < 0 || state > 31) {
            throw new IllegalArgumentException(
                    "GT6 steam engine state must be between 0 and 31");
        }
        return (nominal * (state + 1L)) / 16L;
    }

    public static boolean activelyEmitting(
            long stored, long tOutput, long nominal) {
        return stored > tOutput && tOutput * 2L >= nominal;
    }

    public static long maximumKu(long nominal) {
        return outputKu(nominal, 31);
    }
}
