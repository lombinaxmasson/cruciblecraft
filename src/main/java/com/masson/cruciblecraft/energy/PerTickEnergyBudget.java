package com.masson.cruciblecraft.energy;

/**
 * Transient shared packet budget for one producer tick.
 *
 * <p>Push emitters normally call a source once, but the public capability must
 * still remain rate-safe when queried by networks or third-party callers.
 */
public final class PerTickEnergyBudget {
    private long budgetTick;
    private long claimedThisTick;
    private boolean initialized;

    /**
     * Claims up to {@code requested} packets from the tick's {@code limit}.
     * Simulation is side-effect free.
     */
    public long claim(long gameTime, long limit, long requested, boolean simulate) {
        if (limit <= 0L || requested <= 0L) {
            return 0L;
        }
        long alreadyClaimed = initialized && gameTime == budgetTick ? claimedThisTick : 0L;
        long claimed = Math.min(requested, Math.max(0L, limit - alreadyClaimed));
        if (!simulate && claimed > 0L) {
            if (!initialized || gameTime != budgetTick) {
                budgetTick = gameTime;
                claimedThisTick = 0L;
                initialized = true;
            }
            claimedThisTick += claimed;
        }
        return claimed;
    }
}
