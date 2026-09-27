package com.masson.cruciblecraft.steam;

/**
 * GT6 {@code MultiTileEntityEngineSteam} {@code tOutput=(mOutput*(mState+1))/16}.
 *
 * <p>The output range scales with the tier's nominal packet. The active gate
 * uses the strict GT6 condition {@code tOutput*2 > nominal}. {@code mState}
 * itself is {@code min(31, UT.Code.scale(mEnergy, mCapacity, 32, false))}
 * and is only sampled once per second.
 */
public final class SteamEngineKuCurve {
    private SteamEngineKuCurve() {}

    /**
     * GT6 {@code UT.Code.scale(stored, capacity, 32, false)}, then clamped
     * to the 0–31 engine state. A full buffer scales to 32 and lands on 31.
     */
    public static int visualState(long stored, long capacity) {
        if (capacity <= 0L || stored <= 0L) {
            return 0;
        }
        long scaled = stored >= capacity
                ? 32L
                : 1L + (stored * 31L) / capacity;
        return (int) Math.min(31L, scaled);
    }

    /**
     * GT6 vents only when the latched state is already above 30. A buffer
     * that has just reached capacity still reports state 31 if resampled,
     * so the stop check must use the previous sample.
     */
    public static boolean ventsWhenFull(int state) {
        if (state < 0 || state > 31) {
            throw new IllegalArgumentException(
                    "GT6 steam engine state must be between 0 and 31");
        }
        return state > 30;
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
        return stored > tOutput && tOutput * 2L > nominal;
    }

    public static long maximumKu(long nominal) {
        return outputKu(nominal, 31);
    }
}
