package com.masson.cruciblecraft.machine.processing;

import java.util.Optional;

import net.minecraft.core.Direction;

/**
 * GT6 {@code FACING_TO_SIDE} relative faces. Left/right are the player's
 * left/right while looking at the front, not {@code Direction.getCounterClockWise()}.
 */
public enum MachineRelativeFace {
    BOTTOM(1),
    TOP(2),
    LEFT(4),
    FRONT(8),
    RIGHT(16),
    BACK(32);

    public static final int ANY_SIDE_BIT = 64;
    public static final int ALL_FACES_MASK = 63;
    public static final int ANY_MASK = 127;

    private static final int[][] FACING_TO_WORLD = {
            {0, 1, 2, 3, 4, 5},
            {0, 1, 2, 3, 4, 5},
            {0, 1, 5, 2, 4, 3},
            {0, 1, 4, 3, 5, 2},
            {0, 1, 2, 4, 3, 5},
            {0, 1, 3, 5, 2, 4}
    };
    private static final int[][] WORLD_TO_RELATIVE = {
            {0, 1, 2, 3, 4, 5},
            {0, 1, 2, 3, 4, 5},
            {0, 1, 3, 5, 4, 2},
            {0, 1, 5, 3, 2, 4},
            {0, 1, 2, 4, 3, 5},
            {0, 1, 4, 2, 5, 3}
    };

    private final int bit;

    MachineRelativeFace(int bit) {
        this.bit = bit;
    }

    public int bit() {
        return bit;
    }

    public Direction toWorld(Direction front) {
        if (front == null || !front.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Processing machines face horizontally");
        }
        return Direction.from3DDataValue(FACING_TO_WORLD[front.get3DDataValue()][ordinal()]);
    }

    public static MachineRelativeFace fromWorld(Direction front, Direction side) {
        if (front == null || side == null || !front.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Relative face requires a horizontal front");
        }
        return values()[WORLD_TO_RELATIVE[front.get3DDataValue()][side.get3DDataValue()]];
    }

    public static Optional<MachineRelativeFace> fromIndex(int index) {
        if (index < 0 || index >= values().length) {
            return Optional.empty();
        }
        return Optional.of(values()[index]);
    }

    public static boolean connected(int mask, MachineRelativeFace face) {
        return (mask & face.bit) != 0;
    }
}
