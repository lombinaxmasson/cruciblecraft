package com.masson.cruciblecraft.energy.flux;

/** GT6 {@code UT.Code.units} used by flux converters and coolers. */
public final class FluxMath {
    private FluxMath() {}

    public static long convertUnits(
            long amount,
            long originalUnit,
            long targetUnit,
            boolean roundUp) {
        if (targetUnit == 0L) {
            return 0L;
        }
        if (originalUnit == targetUnit || originalUnit == 0L) {
            return amount;
        }
        long original = originalUnit;
        long target = targetUnit;
        if (original % target == 0L) {
            original /= target;
            target = 1L;
        } else if (target % original == 0L) {
            target /= original;
            original = 1L;
        }
        long product = amount * target;
        long result = product / original;
        if (roundUp && product % original > 0L) {
            result++;
        }
        return Math.max(0L, result);
    }

    public static long ceilDiv(long numerator, long denominator) {
        if (denominator <= 0L) {
            throw new IllegalArgumentException("denominator");
        }
        if (numerator <= 0L) {
            return 0L;
        }
        return (numerator / denominator)
                + (numerator % denominator == 0L ? 0L : 1L);
    }
}
