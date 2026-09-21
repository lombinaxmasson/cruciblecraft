package com.masson.cruciblecraft.content.explosive;

import net.minecraft.world.level.material.MapColor;

/**
 * The three GT6 explosive stick variants.  They share the same block entity
 * but differ in blast resistance threshold, fortune, and display tint.
 */
public enum DynamiteType {
    BOOMSTICK("boomstick", "Boomstick", 10.0F, 3, 0xFFFF8000, MapColor.COLOR_ORANGE),
    DYNAMITE("dynamite", "Dynamite", 10.0F, 5, 0xFFFF0000, MapColor.COLOR_RED),
    STRONG_DYNAMITE(
            "strong_dynamite",
            "Strong Dynamite",
            40.0F,
            5,
            0xFF800080,
            MapColor.COLOR_PURPLE);

    private final String registryPath;
    private final String displayName;
    private final float blastResistance;
    private final int fortune;
    private final int color;
    private final MapColor mapColor;

    DynamiteType(
            String registryPath,
            String displayName,
            float blastResistance,
            int fortune,
            int color,
            MapColor mapColor) {
        this.registryPath = registryPath;
        this.displayName = displayName;
        this.blastResistance = blastResistance;
        this.fortune = fortune;
        this.color = color;
        this.mapColor = mapColor;
    }

    public String registryPath() {
        return registryPath;
    }

    public String displayName() {
        return displayName;
    }

    public float blastResistance() {
        return blastResistance;
    }

    public int fortune() {
        return fortune;
    }

    public int color() {
        return color;
    }

    public MapColor mapColor() {
        return mapColor;
    }
}
