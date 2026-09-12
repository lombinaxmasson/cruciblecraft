package com.masson.cruciblecraft.content.sensor;

/** GT6 MultiTileEntitySensorTE MODE_COUNT = 8. */
public enum SensorMode {
    DISPLAY,
    PERCENT,
    GREATER,
    EQUAL,
    SMALLER,
    SCALE,
    FULL,
    NOT_FULL;

    public static final int COUNT = values().length;

    public SensorMode next() {
        return values()[(ordinal() + 1) % COUNT];
    }

    public static SensorMode fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= COUNT) {
            return DISPLAY;
        }
        return values()[ordinal];
    }

    public boolean usesSetNumber() {
        return this == GREATER || this == EQUAL || this == SMALLER || this == SCALE;
    }
}
