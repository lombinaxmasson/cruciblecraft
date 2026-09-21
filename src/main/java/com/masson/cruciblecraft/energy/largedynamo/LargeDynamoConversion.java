package com.masson.cruciblecraft.energy.largedynamo;

import com.masson.cruciblecraft.energy.drive.RotationEngineConversion;

/**
 * GT6 {@code TE_Behavior_Energy_Converter} with {@code NBT_WASTE_ENERGY}:
 * {@code tOutput = units(stored, inputRec, outputRec)}, emit when
 * {@code tOutput >= outputMin}, overload above {@code outputMax}, then waste
 * {@code inputMax}.
 */
public final class LargeDynamoConversion {
    private LargeDynamoConversion() {}

    public record Tick(
            boolean canEmit,
            boolean overloaded,
            long packetSize,
            long storedAfterWaste) {}

    public static Tick emit(long stored, LargeDynamoCatalog.Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("large dynamo profile");
        }
        long tOutput = RotationEngineConversion.units(
                stored, profile.inputRec(), profile.outputRec(), false);
        boolean canEmit = tOutput >= profile.outputMin();
        if (canEmit && tOutput > profile.outputMax()) {
            return new Tick(false, true, tOutput, 0L);
        }
        long remaining = Math.max(0L, stored - profile.inputMax());
        return new Tick(canEmit, false, canEmit ? tOutput : 0L, remaining);
    }
}
