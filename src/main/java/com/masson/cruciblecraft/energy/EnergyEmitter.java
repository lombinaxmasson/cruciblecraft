package com.masson.cruciblecraft.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.registry.ModCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Pushes one source's packet budget to adjacent consumers in stable side order. */
public final class EnergyEmitter {
    private EnergyEmitter() {}

    public static long emit(
            Level level,
            BlockPos position,
            IEnergyHandler source,
            EnergyType type,
            Direction side) {
        return emit(level, position, source, type, List.of(side));
    }

    /**
     * Probes demand, fairly allocates the source budget, then re-simulates and
     * commits each consumer serially. Source extraction remains first for each
     * commit, so shared downstream state and broken endpoints can only reduce
     * delivery or dissipate energy; they cannot escape through the server tick.
     */
    public static long emit(
            Level level,
            BlockPos position,
            IEnergyHandler source,
            EnergyType type,
            List<Direction> sides) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(sides, "sides");
        if (level.isClientSide || sides.isEmpty()) {
            return 0L;
        }
        try {
            return emitChecked(
                    level, position, source, type, sides);
        } catch (RuntimeException failure) {
            EnergyTransferDiagnostics.warnOnce(
                    "energy emission",
                    source,
                    "Unexpected failure at " + position,
                    failure);
            return 0L;
        }
    }

    private static long emitChecked(
            Level level,
            BlockPos position,
            IEnergyHandler source,
            EnergyType type,
            List<Direction> sides) {
        Direction extractionSide = null;
        long size = 0L;
        for (Direction side : sides) {
            if (side != null && source.handles(type, side)) {
                long candidate = source.outputSize(type, side);
                if (candidate != 0L) {
                    extractionSide = side;
                    size = candidate;
                    break;
                }
            }
        }
        if (extractionSide == null) {
            return 0L;
        }

        long budget = safeExtract(
                source,
                type,
                size,
                Long.MAX_VALUE,
                extractionSide,
                true,
                "source simulation",
                position);
        if (budget <= 0L) {
            return 0L;
        }

        List<IEnergyHandler> consumers = new ArrayList<>();
        List<BlockPos> consumerPositions = new ArrayList<>();
        List<Direction> consumerSides = new ArrayList<>();
        List<Long> demandList = new ArrayList<>();
        for (Direction side : sides) {
            if (side == null
                    || !source.handles(type, side)
                    || source.outputSize(type, side) != size) {
                continue;
            }
            Direction consumerSide = side.getOpposite();
            BlockPos targetPosition = position.relative(side);
            if (!level.hasChunkAt(targetPosition)) {
                continue;
            }
            IEnergyHandler target = level.getCapability(
                    ModCapabilities.ENERGY,
                    targetPosition,
                    consumerSide);
            if (target == null || !target.handles(type, consumerSide)) {
                continue;
            }
            long demand = safeInsert(
                    target,
                    type,
                    size,
                    budget,
                    consumerSide,
                    true,
                    "consumer simulation",
                    targetPosition);
            if (demand > 0L) {
                consumers.add(target);
                consumerPositions.add(targetPosition);
                consumerSides.add(consumerSide);
                demandList.add(demand);
            }
        }
        if (consumers.isEmpty()) {
            return 0L;
        }

        long[] demands = new long[demandList.size()];
        for (int index = 0; index < demands.length; index++) {
            demands[index] = demandList.get(index);
        }
        long[] plan = EnergyAllocation.distribute(budget, demands);
        return executePlan(
                position,
                source,
                type,
                size,
                extractionSide,
                consumers,
                consumerPositions,
                consumerSides,
                plan);
    }

    static long executePlan(
            BlockPos sourcePosition,
            IEnergyHandler source,
            EnergyType type,
            long size,
            Direction extractionSide,
            List<IEnergyHandler> consumers,
            List<BlockPos> consumerPositions,
            List<Direction> consumerSides,
            long[] plan) {
        Objects.requireNonNull(sourcePosition, "sourcePosition");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(consumers, "consumers");
        Objects.requireNonNull(consumerPositions, "consumerPositions");
        Objects.requireNonNull(consumerSides, "consumerSides");
        Objects.requireNonNull(plan, "plan");
        if (consumers.size() != consumerPositions.size()
                || consumers.size() != consumerSides.size()
                || consumers.size() != plan.length) {
            throw new IllegalArgumentException("Energy transfer plan shape mismatch");
        }
        long delivered = 0L;
        for (int index = 0; index < consumers.size(); index++) {
            if (plan[index] <= 0L) {
                continue;
            }
            IEnergyHandler consumer = consumers.get(index);
            BlockPos consumerPosition = consumerPositions.get(index);
            Direction consumerSide = consumerSides.get(index);
            try {
                consumer.invalidateSimulationCache();
            } catch (RuntimeException failure) {
                EnergyTransferDiagnostics.warnOnce(
                        "consumer cache invalidation",
                        consumer,
                        endpoint(
                                "consumer " + index
                                        + " cache invalidation",
                                consumer,
                                consumerPosition,
                                consumerSide),
                        failure);
                continue;
            }
            long simulated = safeInsert(
                    consumer,
                    type,
                    size,
                    plan[index],
                    consumerSide,
                    true,
                    "consumer " + index + " simulation",
                    consumerPosition);
            if (simulated <= 0L) {
                continue;
            }
            long sourceAvailable = safeExtract(
                    source,
                    type,
                    size,
                    simulated,
                    extractionSide,
                    true,
                    "source simulation",
                    sourcePosition);
            long transferable = Math.min(simulated, sourceAvailable);
            if (transferable <= 0L) {
                continue;
            }
            long extracted = safeExtract(
                    source,
                    type,
                    size,
                    transferable,
                    extractionSide,
                    false,
                    "source execution",
                    sourcePosition);
            if (extracted <= 0L) {
                continue;
            }
            long accepted = safeInsert(
                    consumer,
                    type,
                    size,
                    extracted,
                    consumerSide,
                    false,
                    "consumer " + index + " execution",
                    consumerPosition);
            delivered = EnergyPackets.add(delivered, accepted);
        }
        return delivered;
    }

    private static long safeInsert(
            IEnergyHandler handler,
            EnergyType type,
            long size,
            long requested,
            Direction side,
            boolean simulate,
            String operation,
            BlockPos position) {
        long actual;
        try {
            actual = handler.insert(
                    type, size, requested, side, simulate);
        } catch (RuntimeException failure) {
            EnergyTransferDiagnostics.warnOnce(
                    operation,
                    handler,
                    endpoint(operation, handler, position, side),
                    failure);
            return 0L;
        }
        return boundedResult(
                operation,
                handler,
                position,
                side,
                requested,
                actual);
    }

    private static long safeExtract(
            IEnergyHandler handler,
            EnergyType type,
            long size,
            long requested,
            Direction side,
            boolean simulate,
            String operation,
            BlockPos position) {
        long actual;
        try {
            actual = handler.extract(
                    type, size, requested, side, simulate);
        } catch (RuntimeException failure) {
            EnergyTransferDiagnostics.warnOnce(
                    operation,
                    handler,
                    endpoint(operation, handler, position, side),
                    failure);
            return 0L;
        }
        return boundedResult(
                operation,
                handler,
                position,
                side,
                requested,
                actual);
    }

    private static long boundedResult(
            String operation,
            IEnergyHandler handler,
            BlockPos position,
            Direction side,
            long requested,
            long actual) {
        if (actual < 0L || actual > requested) {
            EnergyTransferDiagnostics.warnOnce(
                    operation,
                    handler,
                    endpoint(operation, handler, position, side)
                            + " returned " + actual
                            + " packets for request " + requested);
            return 0L;
        }
        return actual;
    }

    private static String endpoint(
            String operation,
            IEnergyHandler handler,
            BlockPos position,
            Direction side) {
        return operation + " at " + position
                + " using " + handler.getClass().getName()
                + " on side " + side;
    }
}
