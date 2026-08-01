package com.masson.cruciblecraft.energy;

/**
 * Deterministic, Minecraft-independent packet allocation.
 *
 * <p>Consumer order is the stable tie-break order for indivisible remainders.
 */
public final class EnergyAllocation {
    private EnergyAllocation() {}

    /**
     * Distributes at most {@code amount} packets without exceeding any demand.
     *
     * @param amount total packet budget
     * @param demands maximum packet count accepted by each consumer
     * @return one allocation per demand; the input array is never mutated
     */
    public static long[] distribute(long amount, long[] demands) {
        if (demands == null) {
            throw new NullPointerException("demands");
        }
        long[] result = new long[demands.length];
        long remaining = Math.max(0L, amount);
        int active = countUnsatisfied(demands, result);

        while (remaining > 0L && active > 0) {
            long share = Math.max(1L, remaining / active);
            boolean progressed = false;
            for (int index = 0; index < demands.length && remaining > 0L; index++) {
                long demand = Math.max(0L, demands[index]);
                long room = demand - result[index];
                if (room <= 0L) {
                    continue;
                }
                long given = Math.min(share, Math.min(room, remaining));
                result[index] += given;
                remaining -= given;
                progressed |= given > 0L;
            }
            if (!progressed) {
                break;
            }
            active = countUnsatisfied(demands, result);
        }
        return result;
    }

    private static int countUnsatisfied(long[] demands, long[] allocated) {
        int count = 0;
        for (int index = 0; index < demands.length; index++) {
            if (demands[index] > allocated[index]) {
                count++;
            }
        }
        return count;
    }
}
