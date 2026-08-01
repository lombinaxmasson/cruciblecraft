package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Pure bipartite capacity matcher used by concrete item and fluid queries.
 *
 * <p>The dense Edmonds-Karp graph and presence-reservation search are intended
 * for the current small machine input sets. Large multiblock recipe maps should
 * move to a sparse flow graph and a polynomial witness-allocation strategy.
 */
public final class CapacityMatcher {
    private static final int PRESENCE_RESERVATION_SUPPLY_CAP = 12;

    private CapacityMatcher() {}

    public static boolean canSatisfy(
            long[] demands,
            long[] supplies,
            boolean[][] compatible) {
        return solve(demands, supplies, compatible).isPresent();
    }

    public static boolean canSatisfy(
            long[] demands,
            long[] supplies,
            boolean[][] compatible,
            boolean[] presenceOnly) {
        return solve(demands, supplies, compatible, presenceOnly).isPresent();
    }

    /**
     * Returns how much each consuming requirement takes from each supply.
     * Every demand in this overload must be positive.
     */
    public static Optional<long[][]> solve(
            long[] demands,
            long[] supplies,
            boolean[][] compatible) {
        return solve(demands, supplies, compatible, new boolean[demands.length]);
    }

    /**
     * Returns a full requirement-by-supply allocation. Presence-only rows consume
     * nothing, but the solver reserves compatible witnesses before allocating so
     * a valid alternative allocation is not rejected accidentally.
     */
    public static Optional<long[][]> solve(
            long[] demands,
            long[] supplies,
            boolean[][] compatible,
            boolean[] presenceOnly) {
        if (demands.length != compatible.length) {
            throw new IllegalArgumentException("Compatibility rows must match demands");
        }
        if (presenceOnly.length != demands.length) {
            throw new IllegalArgumentException("Presence flags must match demands");
        }
        boolean hasPresenceRequirement = false;
        for (boolean presence : presenceOnly) {
            hasPresenceRequirement |= presence;
        }
        if (hasPresenceRequirement
                && supplies.length > PRESENCE_RESERVATION_SUPPLY_CAP) {
            throw new IllegalArgumentException(
                    "Presence-reservation solver supply limit exceeded"
                            + " (actual=" + supplies.length
                            + ", cap=" + PRESENCE_RESERVATION_SUPPLY_CAP
                            + "); replace it with a polynomial witness-allocation algorithm");
        }
        for (boolean[] row : compatible) {
            if (row.length != supplies.length) {
                throw new IllegalArgumentException("Compatibility columns must match supplies");
            }
        }
        if (Arrays.stream(demands).anyMatch(value -> value < 0L)
                || Arrays.stream(supplies).anyMatch(value -> value < 0L)) {
            throw new IllegalArgumentException("Capacities must not be negative");
        }
        if (demands.length == 0) {
            return Optional.of(new long[0][supplies.length]);
        }

        int consumingCount = 0;
        for (int requirement = 0; requirement < demands.length; requirement++) {
            if (presenceOnly[requirement]) {
                if (demands[requirement] != 0L) {
                    throw new IllegalArgumentException(
                            "Presence-only requirements must have zero demand");
                }
            } else {
                if (demands[requirement] <= 0L) {
                    throw new IllegalArgumentException(
                            "Consuming requirements must have positive demand");
                }
                consumingCount++;
            }
        }
        if (consumingCount == 0) {
            for (int requirement = 0; requirement < demands.length; requirement++) {
                boolean present = false;
                for (int supply = 0; supply < supplies.length; supply++) {
                    if (supplies[supply] > 0L && compatible[requirement][supply]) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    return Optional.empty();
                }
            }
            return Optional.of(new long[demands.length][supplies.length]);
        }

        int[] consumingRows = new int[consumingCount];
        long[] consumingDemands = new long[consumingCount];
        boolean[][] consumingCompatibility = new boolean[consumingCount][supplies.length];
        int consumingIndex = 0;
        for (int requirement = 0; requirement < demands.length; requirement++) {
            if (!presenceOnly[requirement]) {
                consumingRows[consumingIndex] = requirement;
                consumingDemands[consumingIndex] = demands[requirement];
                consumingCompatibility[consumingIndex] = compatible[requirement].clone();
                consumingIndex++;
            }
        }

        Optional<long[][]> consumingSolution = hasPresenceRequirement
                ? solveWithPresenceReservations(
                        consumingDemands,
                        supplies,
                        consumingCompatibility,
                        compatible,
                        presenceOnly)
                : solveConsuming(consumingDemands, supplies, consumingCompatibility);
        if (consumingSolution.isEmpty()) {
            return Optional.empty();
        }

        long[][] allocation = new long[demands.length][supplies.length];
        long[] remaining = supplies.clone();
        long[][] consumingAllocation = consumingSolution.get();
        for (int row = 0; row < consumingRows.length; row++) {
            int requirement = consumingRows[row];
            allocation[requirement] = consumingAllocation[row];
            for (int supply = 0; supply < supplies.length; supply++) {
                remaining[supply] -= consumingAllocation[row][supply];
            }
        }
        for (int requirement = 0; requirement < demands.length; requirement++) {
            if (!presenceOnly[requirement]) {
                continue;
            }
            boolean present = false;
            for (int supply = 0; supply < supplies.length; supply++) {
                if (remaining[supply] > 0L && compatible[requirement][supply]) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                return Optional.empty();
            }
        }
        return Optional.of(allocation);
    }

    private static Optional<long[][]> solveConsuming(
            long[] demands,
            long[] supplies,
            boolean[][] compatible) {
        long totalDemand = exactSum(demands);
        if (exactSum(supplies) < totalDemand) {
            return Optional.empty();
        }

        int source = 0;
        int firstRequirement = 1;
        int firstSupply = firstRequirement + demands.length;
        int sink = firstSupply + supplies.length;
        long[][] residual = new long[sink + 1][sink + 1];

        for (int requirement = 0; requirement < demands.length; requirement++) {
            int node = firstRequirement + requirement;
            residual[source][node] = demands[requirement];
            for (int supply = 0; supply < supplies.length; supply++) {
                if (compatible[requirement][supply]) {
                    residual[node][firstSupply + supply] = totalDemand;
                }
            }
        }
        for (int supply = 0; supply < supplies.length; supply++) {
            residual[firstSupply + supply][sink] = supplies[supply];
        }

        long flow = 0L;
        int[] parent = new int[residual.length];
        while (findPath(residual, source, sink, parent)) {
            long pathCapacity = Long.MAX_VALUE;
            for (int node = sink; node != source; node = parent[node]) {
                pathCapacity = Math.min(pathCapacity, residual[parent[node]][node]);
            }
            for (int node = sink; node != source; node = parent[node]) {
                int previous = parent[node];
                residual[previous][node] -= pathCapacity;
                residual[node][previous] += pathCapacity;
            }
            flow += pathCapacity;
            if (flow == totalDemand) {
                long[][] allocation = new long[demands.length][supplies.length];
                for (int requirement = 0; requirement < demands.length; requirement++) {
                    for (int supply = 0; supply < supplies.length; supply++) {
                        allocation[requirement][supply] =
                                residual[firstSupply + supply][firstRequirement + requirement];
                    }
                }
                return Optional.of(allocation);
            }
        }
        return Optional.empty();
    }

    private static Optional<long[][]> solveWithPresenceReservations(
            long[] consumingDemands,
            long[] supplies,
            boolean[][] consumingCompatibility,
            boolean[][] allCompatibility,
            boolean[] presenceOnly) {
        return searchPresenceReservations(
                consumingDemands,
                supplies,
                consumingCompatibility,
                allCompatibility,
                presenceOnly,
                new BitSet(supplies.length),
                new HashSet<>());
    }

    private static Optional<long[][]> searchPresenceReservations(
            long[] consumingDemands,
            long[] supplies,
            boolean[][] consumingCompatibility,
            boolean[][] allCompatibility,
            boolean[] presenceOnly,
            BitSet reserved,
            Set<BitSet> failedReservations) {
        if (failedReservations.contains(reserved)) {
            return Optional.empty();
        }

        int uncoveredRequirement = -1;
        int fewestCandidates = Integer.MAX_VALUE;
        for (int requirement = 0; requirement < presenceOnly.length; requirement++) {
            if (!presenceOnly[requirement]
                    || isCovered(allCompatibility[requirement], reserved)) {
                continue;
            }
            int candidates = 0;
            for (int supply = 0; supply < supplies.length; supply++) {
                if (supplies[supply] > 0L && allCompatibility[requirement][supply]) {
                    candidates++;
                }
            }
            if (candidates == 0) {
                failedReservations.add((BitSet) reserved.clone());
                return Optional.empty();
            }
            if (candidates < fewestCandidates) {
                fewestCandidates = candidates;
                uncoveredRequirement = requirement;
            }
        }

        if (uncoveredRequirement < 0) {
            long[] available = supplies.clone();
            for (int supply = reserved.nextSetBit(0);
                    supply >= 0;
                    supply = reserved.nextSetBit(supply + 1)) {
                available[supply]--;
            }
            Optional<long[][]> solution =
                    solveConsuming(consumingDemands, available, consumingCompatibility);
            if (solution.isEmpty()) {
                failedReservations.add((BitSet) reserved.clone());
            }
            return solution;
        }

        for (int supply = 0; supply < supplies.length; supply++) {
            if (reserved.get(supply)
                    || supplies[supply] <= 0L
                    || !allCompatibility[uncoveredRequirement][supply]) {
                continue;
            }
            BitSet next = (BitSet) reserved.clone();
            next.set(supply);
            Optional<long[][]> solution = searchPresenceReservations(
                    consumingDemands,
                    supplies,
                    consumingCompatibility,
                    allCompatibility,
                    presenceOnly,
                    next,
                    failedReservations);
            if (solution.isPresent()) {
                return solution;
            }
        }
        failedReservations.add((BitSet) reserved.clone());
        return Optional.empty();
    }

    private static boolean isCovered(boolean[] compatible, BitSet reserved) {
        for (int supply = reserved.nextSetBit(0);
                supply >= 0;
                supply = reserved.nextSetBit(supply + 1)) {
            if (compatible[supply]) {
                return true;
            }
        }
        return false;
    }

    private static long exactSum(long[] values) {
        long sum = 0L;
        try {
            for (long value : values) {
                sum = Math.addExact(sum, value);
            }
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Total capacity exceeds long range", exception);
        }
        return sum;
    }

    private static boolean findPath(
            long[][] residual,
            int source,
            int sink,
            int[] parent) {
        Arrays.fill(parent, -1);
        parent[source] = source;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(source);

        while (!queue.isEmpty()) {
            int node = queue.removeFirst();
            for (int next = 0; next < residual.length; next++) {
                if (parent[next] == -1 && residual[node][next] > 0L) {
                    parent[next] = node;
                    if (next == sink) {
                        return true;
                    }
                    queue.addLast(next);
                }
            }
        }
        return false;
    }
}
