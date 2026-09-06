package com.masson.cruciblecraft.compat.jade.observation;

import com.masson.cruciblecraft.material.GT6ImportUnits;

/**
 * Display-layer conversions for Jade. Canonical runtime temperatures stay
 * Celsius; Kelvin is produced only here.
 */
public final class JadeDisplayUnits {
    private JadeDisplayUnits() {}

    public static double celsiusToKelvin(double celsius) {
        return GT6ImportUnits.celsiusToKelvin(celsius);
    }

    public static long celsiusToRoundedKelvin(double celsius) {
        return GT6ImportUnits.celsiusToRoundedKelvin(celsius);
    }

    public static int fillPercent(float fillFraction) {
        if (!Float.isFinite(fillFraction)) {
            throw new IllegalArgumentException("Fill fraction must be finite");
        }
        double clamped = Math.max(0.0d, Math.min(1.0d, fillFraction));
        return (int) Math.round(clamped * 100.0d);
    }
}
