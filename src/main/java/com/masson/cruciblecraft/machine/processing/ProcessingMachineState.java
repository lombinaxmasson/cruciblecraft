package com.masson.cruciblecraft.machine.processing;

import net.minecraft.nbt.CompoundTag;

/** Versioned primitive processor/energy state used by disk NBT and tests. */
public record ProcessingMachineState(
        String activeRecipe,
        int progress,
        int duration,
        String status,
        long powerDemand,
        long energy,
        long resourceRevision) {
    public static final int VERSION = 1;

    public ProcessingMachineState {
        activeRecipe = activeRecipe == null ? "" : activeRecipe;
        duration = Math.max(0, duration);
        progress = Math.max(0, Math.min(progress, duration));
        status = status == null ? "idle" : status;
        powerDemand = Math.max(0L, powerDemand);
        energy = Math.max(0L, energy);
        resourceRevision = Math.max(0L, resourceRevision);
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("processing_version", VERSION);
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
        int version = tag.getInt("processing_version");
        if (version < 0 || version > VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported processing_version " + version
                            + " (supported through " + VERSION + ")");
        }
        return new ProcessingMachineState(
                tag.getString("active_recipe"),
                tag.getInt("progress"),
                tag.getInt("duration"),
                tag.contains("status") ? tag.getString("status") : "idle",
                tag.getLong("power_demand"),
                tag.getLong("energy"),
                tag.getLong("resource_revision"));
    }
}
