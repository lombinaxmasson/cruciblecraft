package com.masson.cruciblecraft.energy.longdistance;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** One GT6 10064–10068 long-distance transformer endpoint. */
public record LongDistanceTransformerProfile(
        ResourceLocation id,
        int sourceId,
        int sourceLine,
        int voltageIndex,
        long voltage,
        String material,
        ResourceLocation hostTransformer,
        String langEn,
        String langZh) {
    public LongDistanceTransformerProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(hostTransformer, "hostTransformer");
        Objects.requireNonNull(langEn, "langEn");
        Objects.requireNonNull(langZh, "langZh");
        if (sourceId <= 0 || sourceLine <= 0 || voltageIndex < 0 || voltage <= 0L) {
            throw new IllegalArgumentException("Invalid long-distance transformer " + id);
        }
    }

    public long capacity() {
        return Math.multiplyExact(voltage, 4L);
    }
}
