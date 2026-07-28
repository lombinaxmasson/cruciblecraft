package com.masson.cruciblecraft.air;

/** Pure request planner used before any air source is simulated or executed. */
public final class AirIntakeCoordinator {
    private AirIntakeCoordinator() {}

    public static float request(
            boolean topOpen,
            boolean chargeCanAcceptAir,
            float bufferRoom,
            float sourceRate) {
        if (!topOpen || !chargeCanAcceptAir
                || !Float.isFinite(bufferRoom) || !Float.isFinite(sourceRate)
                || bufferRoom <= 0.0F || sourceRate <= 0.0F) {
            return 0.0F;
        }
        return Math.min(bufferRoom, sourceRate);
    }
}
