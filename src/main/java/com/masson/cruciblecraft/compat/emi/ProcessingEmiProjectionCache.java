package com.masson.cruciblecraft.compat.emi;

import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader;
import com.masson.cruciblecraft.recipe.gt.GTRecipeRuntimeEpoch;

/**
 * EMI processing projection bound to the published recipe epoch. A stale
 * epoch cannot be served.
 *
 * <p>Cache state is stored in static fields rather than a nested record.
 * NeoForge's GameTest exploded-directory loader has failed to resolve
 * {@code ProcessingEmiProjectionCache$Cached}.
 */
public final class ProcessingEmiProjectionCache {
    private static volatile long cachedEpoch = -1L;
    private static volatile String cachedFingerprint;
    private static volatile ProcessingEmiRegistrationPlan cachedPlan;
    private static volatile long cachedProjectionNanos = -1L;

    private ProcessingEmiProjectionCache() {}

    public static synchronized ProcessingEmiRegistrationPlan planFor(
            List<ProcessingMachineSpec> specs) {
        Objects.requireNonNull(specs, "specs");
        long epoch = GTRecipeRuntimeEpoch.epoch();
        String fingerprint = fingerprint(epoch);
        ProcessingEmiRegistrationPlan current = cachedPlan;
        if (current != null
                && cachedEpoch == epoch
                && fingerprint.equals(cachedFingerprint)) {
            return current;
        }
        long started = System.nanoTime();
        ProcessingEmiRegistrationPlan plan =
                ProcessingEmiRegistrationPlan.create(specs);
        cachedEpoch = epoch;
        cachedFingerprint = fingerprint;
        cachedPlan = plan;
        cachedProjectionNanos = System.nanoTime() - started;
        return plan;
    }

    public static long lastProjectionNanos() {
        return cachedPlan == null ? -1L : cachedProjectionNanos;
    }

    public static long lastEpoch() {
        return cachedPlan == null ? -1L : cachedEpoch;
    }

    public static synchronized void invalidate() {
        cachedEpoch = -1L;
        cachedFingerprint = null;
        cachedPlan = null;
        cachedProjectionNanos = -1L;
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
