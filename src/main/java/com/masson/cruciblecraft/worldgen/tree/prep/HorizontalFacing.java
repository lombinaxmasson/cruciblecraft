package com.masson.cruciblecraft.worldgen.tree.prep;

/**
 * GT6 {@code ALL_SIDES_HORIZONTAL = {2,3,4,5}} (north, south, west, east).
 * Unregistered prep helper; not a blockstate.
 */
public enum HorizontalFacing {
    NORTH,
    SOUTH,
    WEST,
    EAST;

    public static HorizontalFacing fromGtIndex(int index) {
        return values()[Math.floorMod(index, values().length)];
    }
}
