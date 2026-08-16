package com.masson.cruciblecraft.machine.processing;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;

/** Versioned primitive processor/energy state used by disk NBT and tests. */
public record ProcessingMachineState(
        String activeRecipe,
        int progress,
        int duration,
        String status,
        long powerDemand,
        long energy,
        long resourceRevision,
        String machineKind,
        String tierBand,
        String materialId,
        String energyIdentity,
        int operations,
        long workProgress,
        long workRequired,
        Optional<String> identityQuarantine,
        Optional<Integer> unsupportedVersion) {
    public static final int VERSION = 3;

    public ProcessingMachineState {
        activeRecipe = activeRecipe == null ? "" : activeRecipe;
        duration = Math.max(0, duration);
        progress = Math.max(0, Math.min(progress, duration));
        status = status == null ? "idle" : status;
        powerDemand = Math.max(0L, powerDemand);
        energy = Math.max(0L, energy);
        resourceRevision = Math.max(0L, resourceRevision);
        machineKind = machineKind == null ? "" : machineKind;
        tierBand = tierBand == null ? "" : tierBand;
        materialId = materialId == null ? "" : materialId;
        energyIdentity = energyIdentity == null ? "" : energyIdentity;
        operations = Math.max(1, operations);
        workRequired = Math.max(0L, workRequired);
        workProgress = Math.max(
                0L, Math.min(workProgress, workRequired));
        identityQuarantine = identityQuarantine == null
                ? Optional.empty()
                : identityQuarantine.filter(reason -> !reason.isBlank());
        unsupportedVersion = unsupportedVersion == null
                ? Optional.empty()
                : unsupportedVersion.filter(version -> version > 0);
    }

    public ProcessingMachineState(
            String activeRecipe,
            int progress,
            int duration,
            String status,
            long powerDemand,
            long energy,
            long resourceRevision) {
        this(
                activeRecipe,
                progress,
                duration,
                status,
                powerDemand,
                energy,
                resourceRevision,
                "",
                "",
                "",
                "",
                1,
                0L,
                0L,
                Optional.empty(),
                Optional.empty());
    }

    public ProcessingMachineState(
            String activeRecipe,
            int progress,
            int duration,
            String status,
            long powerDemand,
            long energy,
            long resourceRevision,
            Optional<Integer> unsupportedVersion) {
        this(
                activeRecipe,
                progress,
                duration,
                status,
                powerDemand,
                energy,
                resourceRevision,
                "",
                "",
                "",
                "",
                1,
                0L,
                0L,
                Optional.empty(),
                unsupportedVersion);
    }

    public boolean unsupported() {
        return unsupportedVersion.isPresent();
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("processing_version", unsupportedVersion.orElse(VERSION));
        tag.putString("active_recipe", activeRecipe);
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putString("status", status);
        tag.putLong("power_demand", powerDemand);
        tag.putLong("energy", energy);
        tag.putLong("resource_revision", resourceRevision);
        tag.putString("machine_kind", machineKind);
        tag.putString("tier_band", tierBand);
        tag.putString("tier_material", materialId);
        tag.putString("energy_identity", energyIdentity);
        tag.putInt("parallel_operations", operations);
        tag.putLong("work_progress", workProgress);
        tag.putLong("work_required", workRequired);
        identityQuarantine.ifPresent(reason ->
                tag.putString("identity_quarantine", reason));
        return tag;
    }

    public static ProcessingMachineState read(CompoundTag tag) {
        return read(tag, VERSION);
    }

    static ProcessingMachineState read(CompoundTag tag, int supportedVersion) {
        int version = tag.getInt("processing_version");
        if (version < 0) {
            throw new IllegalArgumentException(
                    "Unsupported processing_version " + version
                            + " (supported through " + VERSION + ")");
        }
        String status = tag.contains("status") ? tag.getString("status") : "idle";
        Optional<String> identityQuarantine =
                readIdentityQuarantine(tag, version);
        return new ProcessingMachineState(
                tag.getString("active_recipe"),
                tag.getInt("progress"),
                tag.getInt("duration"),
                status,
                tag.getLong("power_demand"),
                tag.getLong("energy"),
                tag.getLong("resource_revision"),
                tag.getString("machine_kind"),
                tag.getString("tier_band"),
                tag.getString("tier_material"),
                tag.getString("energy_identity"),
                Math.max(1, tag.getInt("parallel_operations")),
                tag.getLong("work_progress"),
                tag.getLong("work_required"),
                identityQuarantine,
                version > supportedVersion
                        ? Optional.of(version)
                        : Optional.empty());
    }

    private static Optional<String> readIdentityQuarantine(
            CompoundTag tag, int version) {
        if (tag.contains("identity_quarantine")) {
            return Optional.of(tag.getString("identity_quarantine"));
        }
        if (tag.contains("tier_profile")) {
            return Optional.of(
                    "persisted tier_profile is unsupported; tier_band is required");
        }
        if (version < VERSION) {
            return Optional.of(
                    "processing_version "
                            + version
                            + " is older than current version "
                            + VERSION);
        }
        return Optional.empty();
    }
}
