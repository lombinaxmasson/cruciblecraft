package com.masson.cruciblecraft.energy.remainder;

/** GT6 {@code UT.Code.bind3} and {@code TileEntityBase10EnergyBatBox} bands. */
public final class BatBoxMath {
    private BatBoxMath() {}

    public static int bind3(long value) {
        if (value < 0L) {
            return 0;
        }
        if (value > 7L) {
            return 7;
        }
        return (int) value;
    }

    public static int band(long buffer, long input, int slots) {
        if (input <= 0L || slots <= 0) {
            return 0;
        }
        return bind3(buffer / (input * 40L * slots));
    }

    /** Positive extracts that many packets; negative injects. */
    public static int packets(int band) {
        return switch (band) {
            case 0 -> 40;
            case 1 -> 20;
            case 6 -> -20;
            case 7 -> -40;
            default -> 0;
        };
    }

    public static int emitCount(int mode, int batteryCount) {
        if (batteryCount <= 0) {
            return 0;
        }
        if (mode <= 0) {
            return batteryCount;
        }
        return Math.min(mode, batteryCount);
    }

    /** GT6 {@code mInput * 300 * invsize} steady-active texture. */
    public static final long FULL_VISUAL_FACTOR = 300L;

    /** Texture index. Inventory bit {@code | 4} does not change this. */
    public static int visual(long buffer, long input, long output, int slots) {
        if (buffer < output) {
            return 0;
        }
        if (buffer >= input * FULL_VISUAL_FACTOR * slots) {
            return 1;
        }
        return 2;
    }
}
