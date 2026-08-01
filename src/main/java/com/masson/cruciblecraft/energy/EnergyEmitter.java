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
     * consumers before deducting the number of packets actually delivered.
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
        if (budget <= 0L) {
            return 0L;
        }

        List<IEnergyHandler> consumers = new ArrayList<>();
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
            if (demand > 0L) {
                consumers.add(target);
                consumerSides.add(consumerSide);
                demandList.add(Math.min(demand, budget));
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

        long delivered = 0L;
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
            delivered += Math.max(0L, Math.min(plan[index], accepted));
        }
        if (delivered > 0L) {
            source.extract(type, size, delivered, extractionSide, false);
        }
        return delivered;
    }
}
