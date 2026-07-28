package com.masson.cruciblecraft.air;

/** Mutable, Minecraft-independent shared extraction budget for one air source. */
public final class PerTickAirLimiter {
    private long extractionTick = Long.MIN_VALUE;
    private float extractedThisTick;

    public float extract(long gameTime, float outputRate, float maxAmount, boolean simulate) {
        if (!Float.isFinite(outputRate) || !Float.isFinite(maxAmount)
                || outputRate <= 0.0F || maxAmount <= 0.0F) {
            return 0.0F;
        }
        float alreadyExtracted = gameTime == extractionTick ? extractedThisTick : 0.0F;
        float extracted = Math.min(maxAmount, Math.max(0.0F, outputRate - alreadyExtracted));
        if (extracted > 0.0F && !simulate) {
            if (gameTime != extractionTick) {
                extractionTick = gameTime;
                extractedThisTick = 0.0F;
            }
            extractedThisTick += extracted;
        }
        return extracted;
    }
}
