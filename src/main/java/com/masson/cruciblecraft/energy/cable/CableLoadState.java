package com.masson.cruciblecraft.energy.cable;

import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.ElectricalProperties;

/** Persistent overload state plus lazily rolled same-tick transfer totals. */
public final class CableLoadState {
    public static final int BURN_LIMIT = 16;
    public static final long DECAY_INTERVAL = 512L;

    private boolean tickInitialized;
    private long currentTick;
    private long amperesThisTick;
    private long wattageThisTick;
    private long wattageLast;
    private int burnCounter;
    private long nextDecayTick = -1L;
    private long burnAtTick = -1L;

    public Snapshot snapshot(long tick) {
        long projectedAmperes = amperesThisTick;
        long projectedWattage = wattageThisTick;
        long projectedLast = wattageLast;
        long projectedTick = currentTick;
        boolean projectedInitialized = tickInitialized;
        if (!projectedInitialized) {
            projectedInitialized = true;
            projectedTick = tick;
            projectedAmperes = 0L;
            projectedWattage = 0L;
        } else if (tick != projectedTick) {
            projectedLast = tick == projectedTick + 1L
                    ? projectedWattage
                    : 0L;
            projectedTick = tick;
            projectedAmperes = 0L;
            projectedWattage = 0L;
        }

        int projectedBurn = burnCounter;
        long projectedDecay = nextDecayTick;
        if (burnAtTick < 0L && projectedBurn > 0) {
            if (projectedDecay < 0L) {
                projectedDecay = safeAdd(tick, DECAY_INTERVAL);
            }
            if (tick >= projectedDecay) {
                long periods = 1L + (tick - projectedDecay) / DECAY_INTERVAL;
                int decay = (int) Math.min(periods, projectedBurn);
                projectedBurn -= decay;
                projectedDecay = projectedBurn == 0
                        ? -1L
                        : safeAdd(
                                projectedDecay,
                                safeMultiply(
                                        periods, DECAY_INTERVAL));
            }
        } else if (projectedBurn == 0) {
            projectedDecay = -1L;
        }
        return new Snapshot(
                projectedInitialized,
                projectedTick,
                projectedAmperes,
                projectedWattage,
                projectedLast,
                projectedBurn,
                projectedDecay,
                burnAtTick);
    }

    public Change advance(long tick) {
        long previousAmperes = amperesThisTick;
        long previousWattage = wattageThisTick;
        long previousLast = wattageLast;
        int previousBurn = burnCounter;
        long previousDecay = nextDecayTick;
        long previousBurnAt = burnAtTick;
        if (!tickInitialized) {
            tickInitialized = true;
            currentTick = tick;
            amperesThisTick = 0L;
            wattageThisTick = 0L;
        } else if (tick != currentTick) {
            wattageLast = tick == currentTick + 1L
                    ? wattageThisTick
                    : 0L;
            currentTick = tick;
            amperesThisTick = 0L;
            wattageThisTick = 0L;
        }
        if (burnAtTick < 0L && burnCounter > 0) {
            if (nextDecayTick < 0L) {
                nextDecayTick = safeAdd(tick, DECAY_INTERVAL);
            }
            if (tick >= nextDecayTick) {
                long periods =
                        1L + (tick - nextDecayTick) / DECAY_INTERVAL;
                int decay = (int) Math.min(periods, burnCounter);
                burnCounter -= decay;
                nextDecayTick = burnCounter == 0
                        ? -1L
                        : safeAdd(
                                nextDecayTick,
                                safeMultiply(
                                        periods, DECAY_INTERVAL));
            }
        } else if (burnCounter == 0) {
            nextDecayTick = -1L;
        }
        return new Change(
                previousBurn != burnCounter
                        || previousDecay != nextDecayTick
                        || previousBurnAt != burnAtTick,
                previousAmperes != amperesThisTick
                        || previousWattage != wattageThisTick
                        || previousLast != wattageLast
                        || previousBurn != burnCounter);
    }

    public boolean wouldOverload(
            long tick,
            long postLossSize,
            long additionalAmperes,
            ElectricalProperties electrical) {
        Snapshot snapshot = snapshot(tick);
        return EnergyPackets.magnitude(postLossSize)
                        > electrical.maxVoltage()
                || EnergyPackets.add(
                                snapshot.amperesThisTick(),
                                additionalAmperes)
                        > electrical.maxAmperage();
    }

    public Change record(
            long tick,
            long postLossSize,
            long amperes,
            boolean overloaded) {
        if (amperes <= 0L) {
            return Change.NONE;
        }
        long previousAmperes = amperesThisTick;
        long previousWattage = wattageThisTick;
        long previousLast = wattageLast;
        int previousBurn = burnCounter;
        long previousDecay = nextDecayTick;
        long previousBurnAt = burnAtTick;
        restore(snapshot(tick));
        amperesThisTick = EnergyPackets.add(amperesThisTick, amperes);
        wattageThisTick = EnergyPackets.add(
                wattageThisTick,
                EnergyPackets.units(postLossSize, amperes));
        if (overloaded) {
            int previous = burnCounter;
            burnCounter = Math.min(BURN_LIMIT, burnCounter + 1);
            if (previous == 0 && burnCounter > 0 && nextDecayTick < 0L) {
                nextDecayTick = safeAdd(tick, DECAY_INTERVAL);
            }
            if (burnCounter >= BURN_LIMIT && burnAtTick < 0L) {
                burnAtTick = safeAdd(tick, 1L);
            }
        }
        return new Change(
                previousBurn != burnCounter
                        || previousDecay != nextDecayTick
                        || previousBurnAt != burnAtTick,
                previousAmperes != amperesThisTick
                        || previousWattage != wattageThisTick
                        || previousLast != wattageLast
                        || previousBurn != burnCounter);
    }

    public boolean shouldBurn(long tick) {
        return burnAtTick >= 0L && tick >= burnAtTick;
    }

    public long wattageLast(long tick) {
        return snapshot(tick).wattageLast();
    }

    public int burnCounter(long tick) {
        return snapshot(tick).burnCounter();
    }

    public Snapshot persistedSnapshot() {
        return rawSnapshot();
    }

    public void restore(Snapshot snapshot) {
        tickInitialized = snapshot.tickInitialized();
        currentTick = snapshot.currentTick();
        amperesThisTick = Math.max(0L, snapshot.amperesThisTick());
        wattageThisTick = Math.max(0L, snapshot.wattageThisTick());
        wattageLast = Math.max(0L, snapshot.wattageLast());
        burnCounter = Math.max(
                0, Math.min(BURN_LIMIT, snapshot.burnCounter()));
        nextDecayTick = snapshot.nextDecayTick();
        burnAtTick = snapshot.burnAtTick();
        if (burnCounter == 0 && burnAtTick < 0L) {
            nextDecayTick = -1L;
        }
    }

    private Snapshot rawSnapshot() {
        return new Snapshot(
                tickInitialized,
                currentTick,
                amperesThisTick,
                wattageThisTick,
                wattageLast,
                burnCounter,
                nextDecayTick,
                burnAtTick);
    }

    private static long safeAdd(long value, long addition) {
        return value > Long.MAX_VALUE - addition
                ? Long.MAX_VALUE
                : value + addition;
    }

    private static long safeMultiply(long value, long multiplier) {
        return value > Long.MAX_VALUE / multiplier
                ? Long.MAX_VALUE
                : value * multiplier;
    }

    public record Snapshot(
            boolean tickInitialized,
            long currentTick,
            long amperesThisTick,
            long wattageThisTick,
            long wattageLast,
            int burnCounter,
            long nextDecayTick,
            long burnAtTick) {}

    public record Change(
            boolean persistenceChanged,
            boolean observableChanged) {
        private static final Change NONE = new Change(false, false);
    }
}
