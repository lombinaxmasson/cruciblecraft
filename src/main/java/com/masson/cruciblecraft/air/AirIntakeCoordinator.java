package com.masson.cruciblecraft.air;

/** Pure request limiter used after crucible top and charge checks pass. */
public final class AirIntakeCoordinator {
    private AirIntakeCoordinator() {}

    public static float request(float bufferRoom, float sourceRate) {
        if (!Float.isFinite(bufferRoom) || !Float.isFinite(sourceRate)
                || bufferRoom <= 0.0F || sourceRate <= 0.0F) {
            return 0.0F;
        }
        return Math.min(bufferRoom, sourceRate);
    }
}
