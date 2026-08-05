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
        Optional<Integer> unsupportedVersion) {
    public static final int VERSION = 1;

    public ProcessingMachineState {
        activeRecipe = activeRecipe == null ? "" : activeRecipe;
        duration = Math.max(0, duration);
        progress = Math.max(0, Math.min(progress, duration));
        status = status == null ? "idle" : status;
        powerDemand = Math.max(0L, powerDemand);
        energy = Math.max(0L, energy);
        resourceRevision = Math.max(0L, resourceRevision);
        unsupportedVersion = unsupportedVersion == null
                ? Optional.empty()
                : unsupportedVersion.filter(version -> version > VERSION);
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
                Optional.empty());
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
