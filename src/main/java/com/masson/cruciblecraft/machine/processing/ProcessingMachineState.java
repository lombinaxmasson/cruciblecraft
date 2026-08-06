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
        String tierProfile,
        String materialId,
        String energyIdentity,
        int operations,
        long workProgress,
        long workRequired,
        Optional<Integer> unsupportedVersion) {
    public static final int VERSION = 2;

    public ProcessingMachineState {
        activeRecipe = activeRecipe == null ? "" : activeRecipe;
        duration = Math.max(0, duration);
        progress = Math.max(0, Math.min(progress, duration));
        status = status == null ? "idle" : status;
        powerDemand = Math.max(0L, powerDemand);
        energy = Math.max(0L, energy);
        resourceRevision = Math.max(0L, resourceRevision);
        machineKind = machineKind == null ? "" : machineKind;
        tierProfile = tierProfile == null ? "" : tierProfile;
        materialId = materialId == null ? "" : materialId;
        energyIdentity = energyIdentity == null ? "" : energyIdentity;
        operations = Math.max(1, operations);
        workRequired = Math.max(0L, workRequired);
        workProgress = Math.max(
                0L, Math.min(workProgress, workRequired));
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
        tag.putString("tier_profile", tierProfile);
        tag.putString("tier_material", materialId);
        tag.putString("energy_identity", energyIdentity);
        tag.putInt("parallel_operations", operations);
        tag.putLong("work_progress", workProgress);
        tag.putLong("work_required", workRequired);
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
        Optional<Integer> legacyUnsupported = legacyUnsupportedVersion(status);
        if (legacyUnsupported.isPresent()) {
            status = "idle";
            if (legacyUnsupported.get() > supportedVersion) {
                version = legacyUnsupported.get();
            }
        }
        return new ProcessingMachineState(
                tag.getString("active_recipe"),
                tag.getInt("progress"),
                tag.getInt("duration"),
                status,
                tag.getLong("power_demand"),
                tag.getLong("energy"),
                tag.getLong("resource_revision"),
                version >= 2 ? tag.getString("machine_kind") : "",
                version >= 2 ? tag.getString("tier_profile") : "",
                version >= 2 ? tag.getString("tier_material") : "",
                version >= 2 ? tag.getString("energy_identity") : "",
                version >= 2
                        ? Math.max(1, tag.getInt("parallel_operations"))
                        : 1,
                version >= 2 ? tag.getLong("work_progress") : 0L,
                version >= 2 ? tag.getLong("work_required") : 0L,
                version > supportedVersion
                        ? Optional.of(version)
                        : Optional.empty());
    }

    private static Optional<Integer> legacyUnsupportedVersion(String status) {
        String prefix = "unsupported_version_";
        if (!status.startsWith(prefix)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(status.substring(prefix.length())));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
