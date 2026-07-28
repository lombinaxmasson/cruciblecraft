package com.masson.cruciblecraft.air;

public final class AirOutputModel {
    public static final float STEEL_STEAM_ENGINE_NOMINAL = 16.0F;
    public static final float BELLOWS_AIR_PER_TICK = STEEL_STEAM_ENGINE_NOMINAL;
    public static final int BELLOWS_STROKE_TICKS = 40;
    public static final float MAX_STORED_AIR = 1_200.0F;

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

    public static float bellowsStrokeTotal() {
        return BELLOWS_AIR_PER_TICK * BELLOWS_STROKE_TICKS;
    }

    public static float addToBuffer(float storedAir, float incomingAir) {
        float safeStoredAir = clampStoredAir(storedAir);
        if (!Float.isFinite(incomingAir) || incomingAir <= 0.0F) {
            return safeStoredAir;
        }
        return Math.min(MAX_STORED_AIR, safeStoredAir + incomingAir);
    }

    public static float consumeProcessingTick(float storedAir) {
        float safeStoredAir = clampStoredAir(storedAir);
        return safeStoredAir >= 1.0F ? safeStoredAir - 1.0F : safeStoredAir;
    }

    public static float clampStoredAir(float storedAir) {
        if (!Float.isFinite(storedAir)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(MAX_STORED_AIR, storedAir));
    }
}
