package com.masson.cruciblecraft.content.mte;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** One in-place GT6 MTE identity. The registry path is the dummy modern id. */
public record MteInPlaceSpec(
        ResourceLocation id,
        String registryPath,
        int meta,
        MteInPlaceKind kind,
        String family,
        String englishName,
        String chineseName,
        String gt6Class) {
    public MteInPlaceSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(registryPath, "registryPath");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(englishName, "englishName");
        Objects.requireNonNull(chineseName, "chineseName");
        Objects.requireNonNull(gt6Class, "gt6Class");
        if (meta < 0) {
            throw new IllegalArgumentException("meta must be >= 0: " + meta);
        }
        if (!registryPath.equals(id.getPath())) {
            throw new IllegalArgumentException(
                    "registry path drifted from id: " + id);
        }
    }
}
