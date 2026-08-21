package com.masson.cruciblecraft.logistics.hopper;

import java.util.ArrayList;
import java.util.List;

/**
 * One Hopper-family menu geometry for arbitrary 1–36 slots.
 *
 * <p>1–9 one row, 10–18 two, 19–27 three, 28–36 four. The last row is not
 * padded with ghost slots. Player inventory shifts down with machine rows.
 */
public final class HopperMenuLayout {
    public static final int SLOT = 18;
    public static final int MACHINE_X = 8;
    public static final int MACHINE_Y = 18;
    public static final int PLAYER_GAP = 12;
    public static final int HOTBAR_GAP = 58;
    public static final int HOTBAR_BOTTOM_PAD = 24;
    public static final int IMAGE_WIDTH = 176;
    public static final int PLAYER_SLOTS = 36;

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

    public static int rows(int slots) {
        requireSlots(slots);
        return (slots + 8) / 9;
    }

    public static int lastRowCount(int slots) {
        requireSlots(slots);
        int remainder = slots % 9;
        return remainder == 0 ? 9 : remainder;
    }

    public static int playerInventoryY(int slots) {
        return MACHINE_Y + rows(slots) * SLOT + PLAYER_GAP;
    }

    public static int hotbarY(int slots) {
        return playerInventoryY(slots) + HOTBAR_GAP;
    }

    public static int imageHeight(int slots) {
        return hotbarY(slots) + HOTBAR_BOTTOM_PAD;
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
        requireSlots(slots);
        ArrayList<SlotPos> positions = new ArrayList<>(slots);
        int fullRows = slots / 9;
        int last = lastRowCount(slots);
        int index = 0;
        for (int row = 0; row < fullRows; row++) {
            for (int column = 0; column < 9; column++) {
                positions.add(new SlotPos(
                        index,
                        MACHINE_X + column * SLOT,
                        MACHINE_Y + row * SLOT,
                        role(queue, index, slots)));
                index++;
            }
        }
        if (fullRows * 9 < slots) {
            for (int column = 0; column < last; column++) {
                positions.add(new SlotPos(
                        index,
                        MACHINE_X + column * SLOT,
                        MACHINE_Y + fullRows * SLOT,
                        role(queue, index, slots)));
                index++;
            }
        }
        if (positions.size() != slots) {
            throw new IllegalStateException("Hopper layout drifted from slot count");
        }
        return List.copyOf(positions);
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

    private static void requireSlots(int slots) {
        if (slots < 1 || slots > 36) {
            throw new IllegalArgumentException("Hopper GUI slots must be 1..36");
        }
    }
}
