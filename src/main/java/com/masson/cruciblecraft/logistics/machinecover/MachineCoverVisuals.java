package com.masson.cruciblecraft.logistics.machinecover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 visual-bit layout for remainder covers. Renderer and tick share this. */
public final class MachineCoverVisuals {
    public static final int DISPLAY_STYLE_SHIFT = 10;
    public static final int DISPLAY_STATUS_MASK = 1023;
    public static final int DISPLAY_STYLE_COUNT = 2;
    public static final int BUTTON_STYLE_SHIFT = 4;
    public static final int BUTTON_MODE_MASK = 15;
    public static final int BUTTON_STYLE_MASK = 7;
    public static final int BUTTON_VISUAL_MASK = 127;

    private static final int B0 = 1;
    private static final int B1 = 2;
    private static final int B2 = 4;
    private static final int B3 = 8;
    private static final int B5 = 32;
    private static final int B6 = 64;
    private static final int B7 = 128;
    private static final int B8 = 256;
    private static final int PRESENT_BITS = B5 | B6 | B7 | B8;
    private static final String[] BUTTON_UNDERLAYS = {
            "underlay",
            "underlay_0_to_15",
            "underlay_0_to_f",
            "underlay_1_to_16",
            "underlay_16_1_to_15",
            "underlay_keypad_1_to_9",
            "underlay_keypad_9_to_1",
            "underlay_bits"
    };

    private MachineCoverVisuals() {}

    public static int displayStyle(int visual) {
        return Math.floorMod(visual >>> DISPLAY_STYLE_SHIFT, DISPLAY_STYLE_COUNT);
    }

    public static String displaySkin(int visual) {
        return displayStyle(visual) == 0 ? "bottom" : "top";
    }

    public static boolean displayLightOn(int visual, int light) {
        return (visual & (1 << light)) != 0;
    }

    public static boolean displayLightPresent(int visual, int light) {
        return (visual & (1 << (5 + light))) != 0;
    }

    public static int encodeDisplay(
            boolean possible,
            boolean passively,
            boolean actively,
            boolean on,
            int style) {
        int visual = (Math.floorMod(style, DISPLAY_STYLE_COUNT)
                << DISPLAY_STYLE_SHIFT)
                | PRESENT_BITS;
        if (possible) {
            visual |= B0;
        }
        if (passively) {
            visual |= B1;
        }
        if (actively) {
            visual |= B2;
        }
        if (on) {
            visual |= B3;
        }
        return visual;
    }

    public static int cycleDisplayStyle(int visual) {
        int nextStyle = (displayStyle(visual) + 1) % DISPLAY_STYLE_COUNT;
        return (visual & DISPLAY_STATUS_MASK)
                | (nextStyle << DISPLAY_STYLE_SHIFT);
    }

    /**
     * GT6 {@code CoverRedstoneEmitter.onCoverClickedRight} hotspots. Returns
     * {@code -1} when the click missed the minus/plus corners and the four
     * bit toggles.
     */
    public static int emitterClick(int current, double u, double v) {
        int mode = Math.floorMod(current, 16);
        if (v >= PX(1) && v <= PX(4)) {
            if (u >= PX(1) && u <= PX(4)) {
                return mode == 0 ? 15 : mode - 1;
            }
            if (u >= NX(4) && u <= NX(1)) {
                return mode == 15 ? 0 : mode + 1;
            }
        }
        if (v >= NX(7) && v <= NX(4) && u >= PX(2) && u <= NX(2)) {
            if (u <= PX(5)) {
                return mode ^ 8;
            }
            if (u <= PX(8)) {
                return mode ^ 4;
            }
            if (u <= NX(5)) {
                return mode ^ 2;
            }
            return mode ^ 1;
        }
        return -1;
    }

    public static double[] faceUv(
            BlockPos pos, BlockHitResult hit, Direction side) {
        double x = hit.getLocation().x - pos.getX();
        double y = hit.getLocation().y - pos.getY();
        double z = hit.getLocation().z - pos.getZ();
        return switch (side) {
            case NORTH -> new double[] {1.0 - x, y};
            case SOUTH -> new double[] {x, y};
            case WEST -> new double[] {z, y};
            case EAST -> new double[] {1.0 - z, y};
            case UP -> new double[] {x, 1.0 - z};
            case DOWN -> new double[] {x, z};
        };
    }

    private static double PX(int pixels) {
        return pixels / 16.0;
    }

    private static double NX(int pixels) {
        return 1.0 - pixels / 16.0;
    }

    public static boolean displaySwitchHotspot(
            int style,
            double horizontal,
            double vertical) {
        if (horizontal < 10.0 / 16.0) {
            return false;
        }
        if (Math.floorMod(style, DISPLAY_STYLE_COUNT) == 0) {
            return vertical >= 12.0 / 16.0;
        }
        return vertical <= 4.0 / 16.0;
    }

    public static int buttonMode(int visual) {
        return visual & BUTTON_MODE_MASK;
    }

    public static int buttonStyle(int visual) {
        return (visual >> BUTTON_STYLE_SHIFT) & BUTTON_STYLE_MASK;
    }

    public static String buttonUnderlay(int visual) {
        return BUTTON_UNDERLAYS[buttonStyle(visual)];
    }

    public static int withButtonMode(int visual, int mode) {
        return (visual & ~BUTTON_MODE_MASK)
                | (mode & BUTTON_MODE_MASK);
    }

    public static int cycleButtonStyle(int visual) {
        return (visual & ~BUTTON_VISUAL_MASK)
                | ((visual + 16) & BUTTON_VISUAL_MASK);
    }

    public static String ventFront() {
        return "front";
    }

    public static String ventBack() {
        return "back";
    }

    public static String ventSides() {
        return "sides";
    }
}
