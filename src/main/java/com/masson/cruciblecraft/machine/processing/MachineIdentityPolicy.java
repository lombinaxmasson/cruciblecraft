package com.masson.cruciblecraft.machine.processing;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure persisted-identity policy. Exact migrations are data; all other
 * mismatches remain quarantined with their original tuple intact.
 */
public final class MachineIdentityPolicy {
    public enum Resolution {
        ACCEPTED,
        MIGRATED,
        QUARANTINED
    }

    public record Identity(
            String machineKind,
            String tierProfile,
            String materialId,
            String energyIdentity) {
        public Identity {
            machineKind = normalize(machineKind);
            tierProfile = normalize(tierProfile);
            materialId = normalize(materialId);
            energyIdentity = normalize(energyIdentity);
        }

        public boolean allBlank() {
            return machineKind.isBlank()
                    && tierProfile.isBlank()
                    && materialId.isBlank()
                    && energyIdentity.isBlank();
        }

        private static String normalize(String value) {
            return value == null ? "" : value;
        }
    }

    public record Migration(
            String variantId,
            Identity from,
            Identity to) {
        public Migration {
            if (variantId == null || variantId.isBlank()) {
                throw new IllegalArgumentException(
                        "Migration variant id must not be blank");
            }
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            if (from.equals(to)) {
                throw new IllegalArgumentException(
                        "Identity migration must change the tuple");
            }
        }

        private boolean applies(
                String candidateVariantId,
                Identity saved,
                Identity current) {
            return variantId.equals(candidateVariantId)
                    && from.equals(saved)
                    && to.equals(current);
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
            String variantId,
            Identity saved,
            Identity current,
            List<Migration> migrations) {
        if (variantId == null || variantId.isBlank()) {
            throw new IllegalArgumentException(
                    "Variant id must not be blank");
        }
        Objects.requireNonNull(saved, "saved");
        Objects.requireNonNull(current, "current");
        List<Migration> known = List.copyOf(
                Objects.requireNonNull(migrations, "migrations"));
        if (saved.allBlank() || saved.equals(current)) {
            return new Decision(
                    Resolution.ACCEPTED,
                    current,
                    Optional.empty());
        }
        if (known.stream().anyMatch(
                migration -> migration.applies(
                        variantId, saved, current))) {
            return new Decision(
                    Resolution.MIGRATED,
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
        if (!saved.tierProfile().equals(current.tierProfile())) {
            return "saved tier "
                    + saved.tierProfile()
                    + " does not match "
                    + current.tierProfile();
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
