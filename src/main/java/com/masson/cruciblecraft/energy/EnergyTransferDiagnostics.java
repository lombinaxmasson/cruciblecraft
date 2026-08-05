package com.masson.cruciblecraft.energy;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.masson.cruciblecraft.CrucibleCraft;

/**
 * Reports capability contract violations once per operation and endpoint class.
 *
 * <p>Capability implementations are external ownership boundaries. A broken
 * endpoint may already have mutated before returning an invalid result, so the
 * runtime must preserve server availability and prefer bounded energy loss over
 * throwing from a block-entity tick.
 */
public final class EnergyTransferDiagnostics {
    private static final Set<String> REPORTED =
            ConcurrentHashMap.newKeySet();

    private EnergyTransferDiagnostics() {}

    public static void warnOnce(
            String operation,
            Object endpoint,
            String details) {
        warnOnce(operation, endpoint, details, null);
    }

    public static void warnOnce(
            String operation,
            Object endpoint,
            String details,
            RuntimeException failure) {
        String endpointClass = endpoint == null
                ? "null"
                : endpoint.getClass().getName();
        if (!REPORTED.add(operation + "|" + endpointClass)) {
            return;
        }
        if (failure == null) {
            CrucibleCraft.LOGGER.warn(
                    "Energy endpoint contract violation during {} using {}: {}",
                    operation,
                    endpointClass,
                    details);
        } else {
            CrucibleCraft.LOGGER.warn(
                    "Energy endpoint failure during {} using {}: {}",
                    operation,
                    endpointClass,
                    details,
                    failure);
        }
    }
}
