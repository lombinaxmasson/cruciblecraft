package com.masson.cruciblecraft.logistics.hopper;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6 {@code ContainerCommon.useDefaultSlots()} hopper-family geometry.
 *
 * <p>Counts 1–9, 12, 14, 15, 16, 18, 27, and 36 use the centered GT6 maps and
 * matching {@code gui/chests/N.png} backgrounds. Player inventory stays at y=84
 * in a 176×166 GUI. Other 1–36 counts keep a centered leftover-row fallback so
 * the menu cannot spawn ghost {@link net.minecraft.world.inventory.Slot}
 * objects.
 */
public final class HopperMenuLayout {
    public static final int SLOT = 18;
    public static final int MACHINE_X = 8;
    public static final int PLAYER_INVENTORY_Y = 84;
    public static final int HOTBAR_GAP = 58;
    public static final int HOTBAR_BOTTOM_PAD = 24;
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 166;
    public static final int PLAYER_SLOTS = 36;
    public static final int TITLE_LABEL_Y = 4;
    public static final int INVENTORY_LABEL_Y = IMAGE_HEIGHT - 94;
    private static final int HIDDEN_LABEL_Y = -10_000;

    public enum SlotRole {
        HOPPER,
        QUEUE_INPUT,
        QUEUE_BUFFER,
        QUEUE_OUTPUT
    }

    public record SlotPos(int index, int x, int y, SlotRole role) {
        public SlotPos {
            if (index < 0 || x < 0 || y < 0 || role == null) {
                throw new IllegalArgumentException("Invalid hopper slot position");
            }
        }
    }

    public record Ranges(
            int machineStart,
            int machineEnd,
            int playerStart,
            int playerEnd) {
        public boolean inMachine(int index) {
            return index >= machineStart && index < machineEnd;
        }

        public boolean inPlayer(int index) {
            return index >= playerStart && index < playerEnd;
        }
    }

    private HopperMenuLayout() {}

    public static boolean hasChestBackground(int slots) {
        return switch (slots) {
            case 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 14, 15, 16, 18, 27, 36 -> true;
            default -> false;
        };
    }

    public static int playerInventoryY(int slots) {
        requireSlots(slots);
        return PLAYER_INVENTORY_Y;
    }

    public static int hotbarY(int slots) {
        return playerInventoryY(slots) + HOTBAR_GAP;
    }

    public static int imageHeight(int slots) {
        requireSlots(slots);
        return IMAGE_HEIGHT;
    }

    public static int titleLabelY(int slots) {
        requireSlots(slots);
        return slots != 16 && slots <= 27 ? TITLE_LABEL_Y : HIDDEN_LABEL_Y;
    }

    public static int playerInventoryLabelY(int slots) {
        int lowest = 0;
        for (int[] coord : coords(slots)) {
            lowest = Math.max(lowest, coord[1]);
        }
        if (lowest + 16 > INVENTORY_LABEL_Y) {
            return HIDDEN_LABEL_Y;
        }
        return INVENTORY_LABEL_Y;
    }

    public static Ranges ranges(int slots) {
        requireSlots(slots);
        return new Ranges(0, slots, slots, slots + PLAYER_SLOTS);
    }

    public static List<SlotPos> hopperSlots(int slots) {
        return machineSlots(slots, false);
    }

    public static List<SlotPos> queueSlots(int slots) {
        return machineSlots(slots, true);
    }

    public static List<SlotPos> playerSlots(int slots) {
        requireSlots(slots);
        ArrayList<SlotPos> positions = new ArrayList<>(PLAYER_SLOTS);
        int invY = playerInventoryY(slots);
        int index = slots;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                positions.add(new SlotPos(
                        index++,
                        MACHINE_X + column * SLOT,
                        invY + row * SLOT,
                        SlotRole.HOPPER));
            }
        }
        int barY = hotbarY(slots);
        for (int column = 0; column < 9; column++) {
            positions.add(new SlotPos(
                    index++,
                    MACHINE_X + column * SLOT,
                    barY,
                    SlotRole.HOPPER));
        }
        return List.copyOf(positions);
    }

    public static int quickMoveDestination(
            int slots, int sourceIndex, boolean sourceEmpty) {
        Ranges range = ranges(slots);
        if (sourceEmpty || sourceIndex < 0 || sourceIndex >= range.playerEnd()) {
            return -1;
        }
        if (range.inMachine(sourceIndex)) {
            return range.playerStart();
        }
        return range.machineStart();
    }

    private static List<SlotPos> machineSlots(int slots, boolean queue) {
        int[][] positions = coords(slots);
        ArrayList<SlotPos> result = new ArrayList<>(positions.length);
        for (int index = 0; index < positions.length; index++) {
            result.add(new SlotPos(
                    index,
                    positions[index][0],
                    positions[index][1],
                    role(queue, index, positions.length)));
        }
        return List.copyOf(result);
    }

    private static SlotRole role(boolean queue, int index, int slots) {
        if (!queue) {
            return SlotRole.HOPPER;
        }
        if (index == 0) {
            return SlotRole.QUEUE_INPUT;
        }
        if (index == slots - 1) {
            return SlotRole.QUEUE_OUTPUT;
        }
        return SlotRole.QUEUE_BUFFER;
    }

    private static int[][] coords(int slots) {
        requireSlots(slots);
        return switch (slots) {
            case 1 -> xy(80, 35);
            case 2 -> xy(71, 35, 89, 35);
            case 3 -> xy(62, 35, 80, 35, 98, 35);
            case 4 -> xy(71, 26, 89, 26, 71, 44, 89, 44);
            case 5 -> xy(44, 35, 62, 35, 80, 35, 98, 35, 116, 35);
            case 6 -> xy(
                    62, 26, 80, 26, 98, 26, 62, 44, 80, 44, 98, 44);
            case 7 -> xy(
                    26, 35, 44, 35, 62, 35, 80, 35, 98, 35, 116, 35, 134, 35);
            case 8 -> xy(
                    53, 26, 71, 26, 89, 26, 107, 26,
                    53, 44, 71, 44, 89, 44, 107, 44);
            case 9 -> xy(
                    62, 17, 80, 17, 98, 17,
                    62, 35, 80, 35, 98, 35,
                    62, 53, 80, 53, 98, 53);
            case 12 -> xy(
                    35, 26, 53, 26, 71, 26, 89, 26, 107, 26, 125, 26,
                    35, 44, 53, 44, 71, 44, 89, 44, 107, 44, 125, 44);
            case 14 -> xy(
                    26, 26, 44, 26, 62, 26, 80, 26, 98, 26, 116, 26, 134, 26,
                    26, 44, 44, 44, 62, 44, 80, 44, 98, 44, 116, 44, 134, 44);
            case 15 -> xy(
                    44, 17, 62, 17, 80, 17, 98, 17, 116, 17,
                    44, 35, 62, 35, 80, 35, 98, 35, 116, 35,
                    44, 53, 62, 53, 80, 53, 98, 53, 116, 53);
            case 16 -> xy(
                    53, 8, 71, 8, 89, 8, 107, 8,
                    53, 26, 71, 26, 89, 26, 107, 26,
                    53, 44, 71, 44, 89, 44, 107, 44,
                    53, 62, 71, 62, 89, 62, 107, 62);
            case 18 -> xy(
                    8, 26, 26, 26, 44, 26, 62, 26, 80, 26, 98, 26, 116, 26,
                    134, 26, 152, 26,
                    8, 44, 26, 44, 44, 44, 62, 44, 80, 44, 98, 44, 116, 44,
                    134, 44, 152, 44);
            case 27 -> xy(
                    8, 17, 26, 17, 44, 17, 62, 17, 80, 17, 98, 17, 116, 17,
                    134, 17, 152, 17,
                    8, 35, 26, 35, 44, 35, 62, 35, 80, 35, 98, 35, 116, 35,
                    134, 35, 152, 35,
                    8, 53, 26, 53, 44, 53, 62, 53, 80, 53, 98, 53, 116, 53,
                    134, 53, 152, 53);
            case 36 -> xy(
                    8, 8, 26, 8, 44, 8, 62, 8, 80, 8, 98, 8, 116, 8, 134, 8,
                    152, 8,
                    8, 26, 26, 26, 44, 26, 62, 26, 80, 26, 98, 26, 116, 26,
                    134, 26, 152, 26,
                    8, 44, 26, 44, 44, 44, 62, 44, 80, 44, 98, 44, 116, 44,
                    134, 44, 152, 44,
                    8, 62, 26, 62, 44, 62, 62, 62, 80, 62, 98, 62, 116, 62,
                    134, 62, 152, 62);
            default -> fallbackCoords(slots);
        };
    }

    private static int[][] fallbackCoords(int slots) {
        int fullRows = slots / 9;
        int last = slots % 9 == 0 ? 9 : slots % 9;
        int visualRows = (slots + 8) / 9;
        int y0 = switch (visualRows) {
            case 1 -> 35;
            case 2 -> 26;
            case 3 -> 17;
            default -> 8;
        };
        int[][] out = new int[slots][2];
        int index = 0;
        for (int row = 0; row < fullRows; row++) {
            for (int column = 0; column < 9; column++) {
                out[index][0] = MACHINE_X + column * SLOT;
                out[index][1] = y0 + row * SLOT;
                index++;
            }
        }
        if (fullRows * 9 < slots) {
            int startX = MACHINE_X + ((9 - last) * SLOT) / 2;
            for (int column = 0; column < last; column++) {
                out[index][0] = startX + column * SLOT;
                out[index][1] = y0 + fullRows * SLOT;
                index++;
            }
        }
        return out;
    }

    private static int[][] xy(int... values) {
        if (values.length == 0 || values.length % 2 != 0) {
            throw new IllegalArgumentException("Hopper slot coords must be pairs");
        }
        int[][] out = new int[values.length / 2][2];
        for (int index = 0; index < out.length; index++) {
            out[index][0] = values[index * 2];
            out[index][1] = values[index * 2 + 1];
        }
        return out;
    }

    private static void requireSlots(int slots) {
        if (slots < 1 || slots > 36) {
            throw new IllegalArgumentException("Hopper GUI slots must be 1..36");
        }
    }
}
