package com.masson.cruciblecraft.machine.processing;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyPackets;

import net.minecraft.core.Direction;

/** Simulation/commit helper for adjacent packet energy sources such as HEAT. */
public final class AdjacentEnergyConsumer {
    private AdjacentEnergyConsumer() {}

    public static boolean consume(
            IEnergyHandler source,
            EnergyType type,
            Direction sourceSide,
            long units,
            boolean simulate) {
        if (source == null || units <= 0L || !source.handles(type, sourceSide)) {
            return false;
        }
        long size = source.outputSize(type, sourceSide);
        long magnitude = EnergyPackets.magnitude(size);
        if (magnitude == 0L) {
            return false;
        }
        long packets = 1L + (units - 1L) / magnitude;
        if (source.extract(type, size, packets, sourceSide, true) != packets) {
            return false;
        }
        return simulate
                || source.extract(type, size, packets, sourceSide, false) == packets;
    }
}
