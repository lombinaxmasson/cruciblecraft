package com.masson.cruciblecraft.energy.longdistance;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** One GT6 LongDistWire01 meta mapped onto a dedicated CC block. */
public record LongDistanceWireProfile(
        ResourceLocation id,
        int voltageIndex,
        long voltage,
        String core,
        String langEn,
        String langZh) {
    public LongDistanceWireProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(langEn, "langEn");
        Objects.requireNonNull(langZh, "langZh");
        if (voltageIndex < 0 || voltage <= 0L) {
            throw new IllegalArgumentException("Invalid long-distance wire " + id);
        }
    }
}
