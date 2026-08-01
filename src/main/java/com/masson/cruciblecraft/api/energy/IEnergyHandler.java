package com.masson.cruciblecraft.api.energy;

import net.minecraft.core.Direction;

/**
 * Sided, packet-based energy endpoint shared by producers, consumers and
 * converters.
 *
 * <p>{@code size} is the signed strength of one packet and {@code amount} is
 * a non-negative packet count. Implementations may use the sign for directional
 * energy such as kinetic or rotational power.
 *
 * <p><strong>Simulation invariant:</strong> a call with {@code simulate=true}
 * must have no side effects and must return the same result as an immediately
 * following call with identical arguments and {@code simulate=false}, provided
 * no intervening state change occurs.
 */
public interface IEnergyHandler {
    /** Whether this side participates in transfers of {@code type}. */
    default boolean handles(EnergyType type, Direction side) {
        return false;
    }

    /** Signed strength of packets currently emitted from this side; zero means none. */
    default long outputSize(EnergyType type, Direction side) {
        return 0L;
    }

    /**
     * Extracts at most {@code maxAmount} packets of exactly {@code size}.
     *
     * @return the non-negative number of packets extracted
     */
    default long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        return 0L;
    }

    /**
     * Offers {@code amount} packets of {@code size} to this side.
     *
     * @return the non-negative number of packets accepted
     */
    default long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        return 0L;
    }

    /** Stored energy magnitude in the base unit of {@code type}. */
    default long stored(EnergyType type) {
        return 0L;
    }

    /** Storage capacity in the base unit of {@code type}. */
    default long capacity(EnergyType type) {
        return 0L;
    }
}
