package com.masson.cruciblecraft.material;

import java.util.regex.Pattern;

/** Common-side validation and parsing for material RGB colors. */
public final class MaterialColors {
    private static final Pattern RGB = Pattern.compile("#[0-9a-fA-F]{6}");

    private MaterialColors() {}

    public static boolean isValid(String color) {
        return color != null && RGB.matcher(color).matches();
    }

    public static String requireValid(String color) {
        if (!isValid(color)) {
            throw new IllegalArgumentException(
                    "Material color must use strict #RRGGBB format: " + color);
        }
        return color;
    }

    public static int parse(String color) {
        requireValid(color);
        return Integer.parseInt(color.substring(1), 16);
    }
}
