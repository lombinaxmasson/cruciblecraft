package com.masson.cruciblecraft.machine.processing;

import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyPackets;

import net.minecraft.core.Direction;

/** Simulation/commit helper for adjacent packet energy sources such as HEAT. */
public final class AdjacentEnergyConsumer {
    private AdjacentEnergyConsumer() {}

    /**
     * Captures the exact handler and packet request that accepted simulation.
     * Executing this plan therefore cannot silently switch to another adjacent
     * capability between the two phases.
     */
    public static Optional<Plan> plan(
            IEnergyHandler source,
            EnergyType type,
            Direction sourceSide,
            long units) {
        if (source == null || units <= 0L || !source.handles(type, sourceSide)) {
            return Optional.empty();
        }
        long size = source.outputSize(type, sourceSide);
        long magnitude = EnergyPackets.magnitude(size);
        if (magnitude == 0L) {
            return Optional.empty();
        }
        long packets = 1L + (units - 1L) / magnitude;
        if (source.extract(type, size, packets, sourceSide, true) != packets) {
            return Optional.empty();
        }
        return Optional.of(new Plan(source, type, sourceSide, size, packets));
    }

    public static boolean consume(
            IEnergyHandler source,
            EnergyType type,
            Direction sourceSide,
            long units,
            boolean simulate) {
        Optional<Plan> plan = plan(source, type, sourceSide, units);
        return plan.isPresent() && (simulate || plan.get().execute());
    }

    public static Optional<WindowPlan> planWindow(
            IEnergyHandler source,
            EnergyType type,
            Direction sourceSide,
            long minimumUnits,
            long maximumUnits) {
        if (source == null
                || minimumUnits <= 0L
                || maximumUnits < minimumUnits
                || !source.handles(type, sourceSide)) {
            return Optional.empty();
        }
        long size = source.outputSize(type, sourceSide);
        long magnitude = EnergyPackets.magnitude(size);
        if (magnitude == 0L || magnitude > maximumUnits) {
            return Optional.empty();
        }
        long maximumPackets = maximumUnits / magnitude;
        long availablePackets = source.extract(
                type,
                size,
                maximumPackets,
                sourceSide,
                true);
        if (availablePackets <= 0L
                || availablePackets > maximumPackets) {
            return Optional.empty();
        }
        long units = EnergyPackets.units(
                magnitude, availablePackets);
        if (units < minimumUnits) {
            return Optional.empty();
        }
        return Optional.of(new WindowPlan(
                new Plan(
                        source,
                        type,
                        sourceSide,
                        size,
                        availablePackets),
                units));
    }

    public record Plan(
            IEnergyHandler source,
            EnergyType type,
            Direction sourceSide,
            long packetSize,
            long packetCount) {
        public Plan {
            if (source == null || type == null || sourceSide == null
                    || packetSize == 0L || packetCount <= 0L) {
                throw new IllegalArgumentException("Invalid adjacent energy plan");
            }
        }

        public boolean execute() {
            return source.extract(
                    type, packetSize, packetCount, sourceSide, false) == packetCount;
        }
    }

    public record WindowPlan(Plan plan, long units) {
        public WindowPlan {
            if (plan == null || units <= 0L) {
                throw new IllegalArgumentException(
                        "Invalid adjacent energy window plan");
            }
        }

        public boolean execute() {
            return plan.execute();
        }
    }
}
