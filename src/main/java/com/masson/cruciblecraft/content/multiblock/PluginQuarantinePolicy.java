package com.masson.cruciblecraft.content.multiblock;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure persisted plugin-id policy, mirroring
 * {@code machine/processing/MachineIdentityPolicy}. Blank first-save ids
 * adopt the current id; every nonblank mismatch remains quarantined
 * unchanged so progress and contents stay recoverable (loss, never
 * duplication).
 */
public final class PluginQuarantinePolicy {
    public enum Resolution {
        ACCEPTED,
        QUARANTINED
    }

    public record Decision(
            Resolution resolution,
            String persistedPluginId,
            Optional<String> quarantineReason) {
        public Decision {
            Objects.requireNonNull(resolution, "resolution");
            persistedPluginId = normalize(persistedPluginId);
            quarantineReason = quarantineReason == null
                    ? Optional.empty()
                    : quarantineReason;
            if ((resolution == Resolution.QUARANTINED)
                    != quarantineReason.isPresent()) {
                throw new IllegalArgumentException(
                        "Only quarantined plugin ids have a reason");
            }
        }
    }

    public static Decision resolve(
            String savedPluginId,
            String currentPluginId) {
        String saved = normalize(savedPluginId);
        String current = normalize(currentPluginId);
        if (saved.isBlank() || saved.equals(current)) {
            return new Decision(
                    Resolution.ACCEPTED,
                    current,
                    Optional.empty());
        }
        return new Decision(
                Resolution.QUARANTINED,
                saved,
                Optional.of(
                        "saved plugin id "
                                + saved
                                + " does not match "
                                + current));
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    private PluginQuarantinePolicy() {}
}
