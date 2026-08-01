package com.masson.cruciblecraft.steam;

import net.minecraft.core.Direction;

/** Central sided-I/O rules shared by capabilities and direct bucket interaction. */
public final class MachineSideRules {
    private MachineSideRules() {}

    public static boolean boilerAcceptsWater(Direction side) {
        return CapabilitySideRules.boilerAcceptsWater(face(side));
    }

    public static boolean boilerExposesSteam(Direction side) {
        return CapabilitySideRules.boilerExposesSteam(face(side));
    }

    public static boolean engineAcceptsSteam(Direction front, Direction side) {
        return CapabilitySideRules.engineAcceptsSteam(face(front), face(side));
    }

    public static boolean engineExposesKinetic(Direction front, Direction side) {
        return CapabilitySideRules.engineExposesKinetic(face(front), face(side));
    }

    public static boolean crusherExtractsItems(Direction front, Direction side) {
        return CapabilitySideRules.crusherExtractsItems(face(front), face(side));
    }

    public static boolean crusherAcceptsKinetic(Direction front, Direction side) {
        return CapabilitySideRules.crusherAcceptsKinetic(face(front), face(side));
    }

    private static CapabilitySideRules.Face face(Direction direction) {
        return direction == null
                ? CapabilitySideRules.Face.UNSIDED
                : CapabilitySideRules.Face.valueOf(direction.name());
    }
}
