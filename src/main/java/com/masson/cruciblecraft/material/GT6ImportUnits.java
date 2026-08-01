package com.masson.cruciblecraft.material;

/**
 * Pure unit conversions for GT6 import boundaries.
 *
 * <p>CrucibleCraft stores material temperatures in degrees Celsius and material
 * quantities in 144 units per ingot. GT6 stores absolute temperatures in
 * Kelvin and quantities in 648,648,000 U per ingot.
 */
public final class GT6ImportUnits {
    public static final long GT6_U_PER_INGOT = 648_648_000L;
    public static final long CC_UNITS_PER_INGOT = 144L;
    public static final long GT6_U_PER_CC_UNIT = GT6_U_PER_INGOT / CC_UNITS_PER_INGOT;
    public static final double KELVIN_OFFSET = 273.15;

    private GT6ImportUnits() {}

    public static double kelvinToCelsius(double kelvin) {
        requireFinite(kelvin);
        if (kelvin < 0.0) {
            throw new IllegalArgumentException("Kelvin temperature must not be negative");
        }
        return kelvin - KELVIN_OFFSET;
    }

    public static double celsiusToKelvin(double celsius) {
        requireFinite(celsius);
        double kelvin = celsius + KELVIN_OFFSET;
        if (!Double.isFinite(kelvin) || kelvin < 0.0) {
            throw new IllegalArgumentException(
                    "Celsius temperature must convert to finite, non-negative Kelvin");
        }
        return kelvin;
    }

    /** NeoForge fluid temperatures are integral Kelvin. */
    public static int celsiusToRoundedKelvin(double celsius) {
        long rounded = Math.round(celsiusToKelvin(celsius));
        return Math.toIntExact(rounded);
    }

    public static long gt6UToCcMaterialUnits(long gt6U) {
        requireNonNegative(gt6U, "GT6 U");
        long remainder = gt6U % GT6_U_PER_CC_UNIT;
        if (remainder != 0L) {
            throw new ArithmeticException(
                    "GT6 U amount is not an integral CrucibleCraft material unit: " + gt6U);
        }
        return gt6U / GT6_U_PER_CC_UNIT;
    }

    public static long ccMaterialUnitsToGt6U(long materialUnits) {
        requireNonNegative(materialUnits, "CrucibleCraft material units");
        return Math.multiplyExact(materialUnits, GT6_U_PER_CC_UNIT);
    }

    private static void requireFinite(double temperature) {
        if (!Double.isFinite(temperature)) {
            throw new IllegalArgumentException("Temperature must be finite");
        }
    }

    private static void requireNonNegative(long value, String unit) {
        if (value < 0L) {
            throw new IllegalArgumentException(unit + " must not be negative");
        }
    }
}
