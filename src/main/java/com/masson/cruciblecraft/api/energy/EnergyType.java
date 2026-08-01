package com.masson.cruciblecraft.api.energy;

/**
 * Closed set of energy kinds supported by CrucibleCraft machines.
 *
 * <p>All kinds use GT6 packet semantics: {@code size} is the signed strength
 * of one packet and {@code amount} is the number of packets. The transferred
 * energy magnitude is therefore {@code abs(size) * amount}.
 */
public enum EnergyType {
    HEAT,
    KINETIC,
    AIR,
    ELECTRIC,
    ROTATION,
    MAGNETIC,
    COOLING
}
