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
    /**
     * Folded compatibility identity still used by machine kinds explicitly
     * classified as fixed/deferred in the T12 25+12 boundary ledger. New
     * source-classified machines must use KINETIC_ROTATION or KINETIC_PUSH.
     */
    @Deprecated
    KINETIC,
    KINETIC_ROTATION,
    KINETIC_PUSH,
    AIR,
    ELECTRIC,
    /**
     * GT6 TIME / TU identity used by Autoclave, Bath, Coagulator and
     * Generifier. This is not a material-tier matrix.
     */
    TIME
}
