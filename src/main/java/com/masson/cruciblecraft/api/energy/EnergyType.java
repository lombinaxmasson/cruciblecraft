package com.masson.cruciblecraft.api.energy;

/**
 * Closed set of energy kinds supported by CrucibleCraft machines.
 *
 * <p>All kinds use GT6 packet semantics: {@code size} is the signed strength
 * of one packet and {@code amount} is the number of packets. The transferred
 * energy magnitude is therefore {@code abs(size) * amount}.
 *
 * <p>GT6 {@code TD.Energy.ALL_SIZE_IRRELEVANT} includes HU, CU, QU and TU.
 * Those kinds emit {@code size = ±1} and put the energy in {@code amount}.
 * KU, RU and EU keep a meaningful packet size (voltage / stroke / torque).
 */
public enum EnergyType {
    /** GT6 HU. Size-irrelevant: emitters push {@code size = 1}. */
    HEAT,
    /**
     * Folded compatibility identity still used by machine kinds explicitly
     * classified as fixed/deferred in the kinetic-kind boundary ledger. New
     * source-classified machines must use KINETIC_ROTATION or KINETIC_PUSH.
     */
    @Deprecated
    KINETIC,
    KINETIC_ROTATION,
    KINETIC_PUSH,
    AIR,
    ELECTRIC,
    /**
     * GT6 LU / LIGHT identity used by Energium crystals. This is not
     * ELECTRIC and must not be folded into EU cables or lockers.
     */
    LU,
    /**
     * GT6 TIME / TU identity used by Autoclave, Bath, Coagulator and
     * Generifier. This is not a material-tier matrix.
     */
    TIME,
    /**
     * GT6 QU / Quantum identity. Never folded into EU or LU cables.
     */
    QUANTUM,
    /**
     * GT6 CU / Cryo identity used by Freezer and Cryo Mixer.
     */
    CU,
    /**
     * GT6 MU / Magnetic identity used by Polarizer and Magnetic Separator.
     */
    MU;

    /**
     * GT6 {@code TD.Energy.ALL_SIZE_IRRELEVANT}: packet size is not a voltage.
     * HU/CU/QU/TU emit {@code size = 1} and carry energy in {@code amount}.
     */
    public boolean sizeIrrelevant() {
        return this == HEAT || this == CU || this == QUANTUM || this == TIME;
    }

    /**
     * GT6 {@code TD.Energy.ALL_NEGATIVE_ALLOWED}. HU is not on that list.
     */
    public boolean allowsNegativeSize() {
        return this == KINETIC
                || this == KINETIC_ROTATION
                || this == KINETIC_PUSH
                || this == ELECTRIC
                || this == QUANTUM
                || this == MU;
    }

    /** Packet size used when this kind is pushed into a neighbor. */
    public long emitPacketSize(long recommendedSize) {
        return sizeIrrelevant() ? 1L : recommendedSize;
    }
}
