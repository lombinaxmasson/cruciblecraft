package com.masson.cruciblecraft.air;

public final class AirOutputModel {
    public static final float STEEL_STEAM_ENGINE_NOMINAL = 16.0F;
    public static final long BELLOWS_AIR_PER_TICK = 16L;
    public static final int BELLOWS_STROKE_TICKS = 40;
    public static final long MAX_STORED_AIR = 1_200L;

    private AirOutputModel() {}

    public static float gt6SteamEngineOutput(float nominalOutput, int state) {
        if (!Float.isFinite(nominalOutput) || nominalOutput < 0.0F) {
            throw new IllegalArgumentException("Nominal output must be finite and non-negative");
        }
        if (state < 0 || state > 31) {
            throw new IllegalArgumentException("GT6 steam engine state must be between 0 and 31");
        }
        return nominalOutput * (state + 1) / 16.0F;
    }

    public static long bellowsStrokeTotal() {
        return BELLOWS_AIR_PER_TICK * BELLOWS_STROKE_TICKS;
    }

    public static long addToBuffer(long storedAir, long incomingAir) {
        long safeStoredAir = clampStoredAir(storedAir);
        if (incomingAir <= 0L) {
            return safeStoredAir;
        }
        return incomingAir >= MAX_STORED_AIR - safeStoredAir
                ? MAX_STORED_AIR
                : safeStoredAir + incomingAir;
    }

    public static long consumeProcessingTick(long storedAir) {
        long safeStoredAir = clampStoredAir(storedAir);
        return safeStoredAir >= 1L ? safeStoredAir - 1L : safeStoredAir;
    }

    public static long clampStoredAir(long storedAir) {
        return Math.max(0L, Math.min(MAX_STORED_AIR, storedAir));
    }
}
