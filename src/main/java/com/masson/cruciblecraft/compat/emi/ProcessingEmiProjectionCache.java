package com.masson.cruciblecraft.compat.emi;

import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader;
import com.masson.cruciblecraft.recipe.gt.GTRecipeRuntimeEpoch;

/**
 * EMI processing projection bound to the published recipe epoch. A stale
 * epoch cannot be served.
 */
public final class ProcessingEmiProjectionCache {
    private static volatile Cached cached;

    private ProcessingEmiProjectionCache() {}

    private record Cached(
            long epoch,
            String fingerprint,
            ProcessingEmiRegistrationPlan plan,
            long projectionNanos) {}

    public static synchronized ProcessingEmiRegistrationPlan planFor(
            List<ProcessingMachineSpec> specs) {
        Objects.requireNonNull(specs, "specs");
        long epoch = GTRecipeRuntimeEpoch.epoch();
        String fingerprint = fingerprint(epoch);
        Cached current = cached;
        if (current != null
                && current.epoch() == epoch
                && current.fingerprint().equals(fingerprint)) {
            return current.plan();
        }
        long started = System.nanoTime();
        ProcessingEmiRegistrationPlan plan =
                ProcessingEmiRegistrationPlan.create(specs);
        cached = new Cached(
                epoch,
                fingerprint,
                plan,
                System.nanoTime() - started);
        return plan;
    }

    public static long lastProjectionNanos() {
        Cached current = cached;
        return current == null ? -1L : current.projectionNanos();
    }

    public static long lastEpoch() {
        Cached current = cached;
        return current == null ? -1L : current.epoch();
    }

    public static synchronized void invalidate() {
        cached = null;
    }

    static void clearForTest() {
        invalidate();
    }

    private static String fingerprint(long epoch) {
        GTRecipeMapLoader.PublicationMetrics metrics =
                GTRecipeMapLoader.lastPublicationMetrics();
        return epoch
                + "|"
                + metrics.compactLoadExtruderStableFingerprint()
                + "|"
                + metrics.compactFamilyStableFingerprint();
    }
}
