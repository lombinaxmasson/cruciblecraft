package com.masson.cruciblecraft.energy.drive;

/**
 * GT6 {@code TE_Behavior_Energy_Converter.doBipolar} for rotation engines:
 * {@code tOutput = units(stored, inputRec, outputRec)}, emit when
 * {@code tOutput >= outputMin}, then waste {@code units(inputMax, 16, 16-mode)}.
 */
public final class RotationEngineConversion {
    private RotationEngineConversion() {}

    public record Tick(
            boolean canEmit,
            boolean overloaded,
            long outputSize,
            long storedAfterWaste) {}

    public static Tick tick(
            long stored,
            RotationEngineCatalog.Profile profile,
            int mode) {
        if (profile == null) {
            throw new IllegalArgumentException("rotation engine profile");
        }
        int boundedMode = Math.max(0, Math.min(15, mode));
        long tOutput = units(stored, profile.inputRec(), profile.outputRec(), false);
        if (boundedMode > 0) {
            tOutput = Math.min(
                    tOutput,
                    units(profile.outputMax(), 16L, 16L - boundedMode, false));
        }
        boolean canEmit = tOutput >= profile.outputMin();
        if (canEmit && tOutput > profile.outputMax()) {
            return new Tick(false, true, 0L, 0L);
        }
        long waste = units(profile.inputMax(), 16L, 16L - boundedMode, true);
        long remaining = Math.max(0L, stored - waste);
        return new Tick(canEmit, false, canEmit ? tOutput : 0L, remaining);
    }

    /** GT6 {@code UT.Code.units}. */
    public static long units(
            long amount,
            long originalUnit,
            long targetUnit,
            boolean roundUp) {
        if (targetUnit == 0L) {
            return 0L;
        }
        if (originalUnit == targetUnit || originalUnit == 0L) {
            return Math.max(0L, amount);
        }
        long source = originalUnit;
        long target = targetUnit;
        if (source % target == 0L) {
            source /= target;
            target = 1L;
        } else if (target % source == 0L) {
            target /= source;
            source = 1L;
        }
        long product = amount * target;
        long converted = product / source;
        if (roundUp && product % source > 0L) {
            converted++;
        }
        return Math.max(0L, converted);
    }
}
