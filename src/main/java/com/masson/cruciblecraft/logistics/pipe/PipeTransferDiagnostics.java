package com.masson.cruciblecraft.logistics.pipe;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.masson.cruciblecraft.CrucibleCraft;

/** Rate-limited reporting for third-party item/fluid endpoint violations. */
public final class PipeTransferDiagnostics {
    private static final Set<String> REPORTED =
            ConcurrentHashMap.newKeySet();

    private PipeTransferDiagnostics() {}

    public static void warnOnce(
            String operation, Object endpoint, String details) {
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
                    "Pipe endpoint contract violation during {} using {}: {}",
                    operation,
                    endpointClass,
                    details);
        } else {
            CrucibleCraft.LOGGER.warn(
                    "Pipe endpoint failure during {} using {}: {}",
                    operation,
                    endpointClass,
                    details,
                    failure);
        }
    }
}
