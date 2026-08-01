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
     * Simulates all consumers, fairly allocates the source budget, then executes
     * the source before consumers so a contract violation can lose energy but
     * can never create it.
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

        long budget = source.extract(type, size, Long.MAX_VALUE, extractionSide, true);
        requireBoundedResult("source simulation", Long.MAX_VALUE, budget);
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
            long demand = target.insert(type, size, budget, consumerSide, true);
            requireBoundedResult(
                    "consumer simulation at " + targetPosition + " side " + consumerSide,
                    budget,
                    demand);
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
        long[] expected = new long[plan.length];
        long delivered = 0L;
        for (int index = 0; index < consumers.size(); index++) {
            if (plan[index] <= 0L) {
                continue;
            }
            long simulated = consumers.get(index).insert(
                    type,
                    size,
                    plan[index],
                    consumerSides.get(index),
                    true);
            expected[index] = requireBoundedResult(
                    endpoint(
                            "consumer " + index + " simulation",
                            consumers.get(index),
                            consumerPositions.get(index),
                            consumerSides.get(index)),
                    plan[index],
                    simulated);
            delivered = Math.addExact(delivered, expected[index]);
        }
        if (delivered <= 0L) {
            return 0L;
        }

        long simulatedExtraction =
                source.extract(type, size, delivered, extractionSide, true);
        requireExactResult(
                endpoint(
                        "source simulation",
                        source,
                        sourcePosition,
                        extractionSide),
                delivered,
                simulatedExtraction);

        // Capability APIs provide no rollback. Exact preflight makes every
        // following call contract-identical; any mismatch is therefore an
        // endpoint violation and must stop the transfer immediately. Deducting
        // first deliberately makes the irrecoverable failure direction energy
        // loss rather than duplication.
        long extracted = source.extract(
                type,
                size,
                delivered,
                extractionSide,
                false);
        requireExactResult(
                endpoint(
                        "source execution",
                        source,
                        sourcePosition,
                        extractionSide),
                delivered,
                extracted);

        for (int index = 0; index < consumers.size(); index++) {
            if (plan[index] <= 0L) {
                continue;
            }
            long accepted = consumers.get(index).insert(
                    type,
                    size,
                    plan[index],
                    consumerSides.get(index),
                    false);
            requireExactResult(
                    endpoint(
                            "consumer " + index + " execution",
                            consumers.get(index),
                            consumerPositions.get(index),
                            consumerSides.get(index)),
                    expected[index],
                    accepted);
        }
        return delivered;
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

    private static long requireBoundedResult(
            String operation,
            long requested,
            long actual) {
        if (actual < 0L || actual > requested) {
            throw new IllegalStateException(
                    operation + " returned " + actual
                            + " packets for request " + requested);
        }
        return actual;
    }

    private static void requireExactResult(
            String operation,
            long expected,
            long actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    operation + " returned " + actual
                            + " packets after simulating " + expected);
        }
    }
}
