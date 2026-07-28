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

    public static boolean engineExposesKinetic(Face front, Face side) {
        return side != Face.UNSIDED && side == front;
    }

    public static boolean crusherExtractsItems(Face front, Face side) {
        return side != Face.UNSIDED && side == front;
    }
}
