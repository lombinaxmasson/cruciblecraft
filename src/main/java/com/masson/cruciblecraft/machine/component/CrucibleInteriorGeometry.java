package com.masson.cruciblecraft.machine.component;

/**
 * GT6 {@code MultiTileEntitySmeltery}/{@code MultiTileEntityCrucible} interior
 * height: {@code UT.Code.scale(total, MAX_AMOUNT, 255)} then
 * {@code 0.125 + unsignedByte / (2048/7)} for the single block, or
 * {@code 1.125 + unsignedByte / 150} for the large crucible.
 */
public final class CrucibleInteriorGeometry {
    public static final int GRAY_64 = 0x404040;
    public static final float SMALL_WALL = 2.0f / 16.0f;
    public static final float SMALL_BASE_Y = 0.125f;
    public static final float SMALL_HEIGHT_DIVISOR = 2048.0f / 7.0f;
    public static final float LARGE_BASE_Y = 1.125f;
    public static final float LARGE_HEIGHT_DIVISOR = 150.0f;

    private CrucibleInteriorGeometry() {}

    public static int displayedHeight(int totalUnits, int maxUnits) {
        if (totalUnits <= 0 || maxUnits <= 0) {
            return 0;
        }
        if (totalUnits >= maxUnits) {
            return 255;
        }
        return (int) (1L + (totalUnits * 254L) / maxUnits);
    }

    public static float smallSurfaceY(int displayedHeight) {
        return SMALL_BASE_Y + unsignedByte(displayedHeight) / SMALL_HEIGHT_DIVISOR;
    }

    public static float largeSurfaceY(int displayedHeight) {
        return LARGE_BASE_Y + unsignedByte(displayedHeight) / LARGE_HEIGHT_DIVISOR;
    }

    public static int unsignedByte(int displayedHeight) {
        return displayedHeight & 0xFF;
    }
}
