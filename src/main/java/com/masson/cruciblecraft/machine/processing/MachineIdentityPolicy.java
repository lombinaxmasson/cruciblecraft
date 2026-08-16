package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure current persisted-identity policy. Blank first-save identities adopt
 * the current tuple; every nonblank mismatch remains quarantined unchanged.
 */
public final class MachineIdentityPolicy {
    public enum Resolution {
        ACCEPTED,
        QUARANTINED
    }

    public record Identity(
            String machineKind,
            String tierBand,
            String materialId,
            String energyIdentity) {
        public Identity {
            machineKind = normalize(machineKind);
            tierBand = normalize(tierBand);
            materialId = normalize(materialId);
            energyIdentity = normalize(energyIdentity);
        }

        public boolean allBlank() {
            return machineKind.isBlank()
                    && tierBand.isBlank()
                    && materialId.isBlank()
                    && energyIdentity.isBlank();
        }

        private static String normalize(String value) {
            return value == null ? "" : value;
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
                        "Only quarantined identities have a reason");
            }
        }
    }

    public static Decision resolve(
            Identity saved,
            Identity current) {
        Objects.requireNonNull(saved, "saved");
        Objects.requireNonNull(current, "current");
        if (saved.allBlank() || saved.equals(current)) {
            return new Decision(
                    Resolution.ACCEPTED,
                    current,
                    Optional.empty());
        }
        return new Decision(
                Resolution.QUARANTINED,
                saved,
                Optional.of(mismatchReason(saved, current)));
    }

    private static String mismatchReason(
            Identity saved, Identity current) {
        if (!saved.machineKind().equals(current.machineKind())) {
            return "saved kind "
                    + saved.machineKind()
                    + " does not match "
                    + current.machineKind();
        }
        if (!saved.tierBand().equals(current.tierBand())) {
            return "saved tier band "
                    + saved.tierBand()
                    + " does not match "
                    + current.tierBand();
        }
        if (!saved.materialId().equals(current.materialId())) {
            return "saved material "
                    + saved.materialId()
                    + " does not match "
                    + current.materialId();
        }
        return "saved energy "
                + saved.energyIdentity()
                + " does not match "
                + current.energyIdentity();
    }

    private MachineIdentityPolicy() {}
}
