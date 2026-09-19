package com.masson.cruciblecraft.compat.jade.observation;

import java.util.Locale;

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

    /**
     * GT6 {@code UT.Code.displayUnits} in CC units (144 = 1 ingot). Trailing
     * zeros are stripped so 144 → {@code 1} and 16 → {@code 0.111}.
     */
    public static String formatIngotAmount(long units) {
        if (units < 0) {
            return "?.???";
        }
        long ingot = GT6ImportUnits.CC_UNITS_PER_INGOT;
        long whole = units / ingot;
        long digits = ((units % ingot) * 1000) / ingot;
        if (digits == 0) {
            return Long.toString(whole);
        }
        String fraction = String.format(Locale.ROOT, "%03d", digits);
        int end = fraction.length();
        while (end > 1 && fraction.charAt(end - 1) == '0') {
            end--;
        }
        return whole + "." + fraction.substring(0, end);
    }
}
