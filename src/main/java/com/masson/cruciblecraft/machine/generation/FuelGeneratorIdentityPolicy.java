package com.masson.cruciblecraft.machine.generation;

import java.util.Objects;
import java.util.Optional;

/**
 * Dedicated fail-closed identity policy for the shared fuel-generator host.
 */
public final class FuelGeneratorIdentityPolicy {
    public static final int MISSING_SCHEMA_VERSION = -1;

    public enum Resolution {
        ACCEPTED,
        QUARANTINED
    }

    public record Identity(
            int schemaVersion,
            String generatorId,
            String energyIdentity) {
        public Identity {
            generatorId = generatorId == null ? "" : generatorId;
            energyIdentity =
                    energyIdentity == null ? "" : energyIdentity;
        }
    }

    public record Decision(
            Resolution resolution,
            Identity persistedIdentity,
            Optional<String> quarantineReason) {
        public Decision {
            Objects.requireNonNull(resolution, "resolution");
            Objects.requireNonNull(
                    persistedIdentity, "persistedIdentity");
            quarantineReason = quarantineReason == null
                    ? Optional.empty()
                    : quarantineReason;
            if (resolution == Resolution.QUARANTINED
                    != quarantineReason.isPresent()) {
                throw new IllegalArgumentException(
                        "Only quarantined generator identities have a reason");
            }
        }
    }

    public static Identity current(FuelGeneratorSpec spec) {
        Objects.requireNonNull(spec, "spec");
        return new Identity(
                spec.identitySchemaVersion(),
                spec.id().toString(),
                spec.outputEnergyType().name());
    }

    public static Decision resolve(
            FuelGeneratorSpec spec,
            Identity saved) {
        Identity expected = current(spec);
        if (saved == null) {
            return quarantine(
                    new Identity(
                            MISSING_SCHEMA_VERSION, "", ""),
                    "saved fuel-generator identity is missing");
        }
        if (saved.equals(expected)) {
            return new Decision(
                    Resolution.ACCEPTED,
                    expected,
                    Optional.empty());
        }
        return quarantine(
                saved,
                mismatchReason(saved, expected));
    }

    private static Decision quarantine(
            Identity saved, String reason) {
        return new Decision(
                Resolution.QUARANTINED,
                saved,
                Optional.of(reason));
    }

    private static String mismatchReason(
            Identity saved, Identity expected) {
        if (saved.schemaVersion() != expected.schemaVersion()) {
            return "saved schema "
                    + saved.schemaVersion()
                    + " does not match "
                    + expected.schemaVersion();
        }
        if (!saved.generatorId().equals(expected.generatorId())) {
            return "saved generator "
                    + saved.generatorId()
                    + " does not match "
                    + expected.generatorId();
        }
        return "saved energy "
                + saved.energyIdentity()
                + " does not match "
                + expected.energyIdentity();
    }

    private FuelGeneratorIdentityPolicy() {}
}
