package com.masson.cruciblecraft.steam;

/** Loader-independent sided capability policy used by regression tests. */
public final class CapabilitySideRules {
    public enum Face { DOWN, UP, NORTH, SOUTH, WEST, EAST, UNSIDED }

    private CapabilitySideRules() {}

    public static boolean boilerAcceptsWater(Face side) {
        return side != Face.UNSIDED && side != Face.UP;
    }

    public static boolean boilerExposesSteam(Face side) {
        return side == Face.UP;
    }

    public static boolean engineAcceptsSteam(Face front, Face side) {
        return side == Face.UNSIDED || side != front;
    }

    /**
     * GT6 {@code FACING_SIDES}: every face except front and back. DistW is
     * pushed here; the back remains steam-only in {@code kinds.json}.
     */
    public static boolean engineExposesExhaust(Face front, Face side) {
        return side != Face.UNSIDED
                && side != front
                && side != opposite(front);
    }

    public static boolean engineExposesKinetic(Face front, Face side) {
        return side != Face.UNSIDED && side == front;
    }

    public static boolean crusherExtractsItems(Face front, Face side) {
        return side != Face.UNSIDED && side == front;
    }

    public static boolean crusherAcceptsKinetic(Face front, Face side) {
        return side != Face.UNSIDED && side == opposite(front);
    }

    private static Face opposite(Face face) {
        return switch (face) {
            case DOWN -> Face.UP;
            case UP -> Face.DOWN;
            case NORTH -> Face.SOUTH;
            case SOUTH -> Face.NORTH;
            case WEST -> Face.EAST;
            case EAST -> Face.WEST;
            case UNSIDED -> Face.UNSIDED;
        };
    }
}
