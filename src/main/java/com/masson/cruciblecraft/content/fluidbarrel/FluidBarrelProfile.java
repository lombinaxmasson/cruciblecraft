package com.masson.cruciblecraft.content.fluidbarrel;

import java.util.Objects;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * One loader identity from {@code Loader_MultiTileEntities} barrel/drum adds.
 */
public record FluidBarrelProfile(
        ResourceLocation id,
        int meta,
        FluidBarrelKind kind,
        String art,
        String materialId,
        String englishName,
        String chineseName,
        String gt6Class,
        long capacity,
        Long meltingKelvin,
        boolean gasProof,
        boolean acidProof,
        boolean plasmaProof,
        boolean magicProof,
        boolean glowing,
        int flammability,
        float hardness,
        float resistance,
        String recipeStatus) {
    public FluidBarrelProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(art, "art");
        Objects.requireNonNull(materialId, "materialId");
        Objects.requireNonNull(englishName, "englishName");
        Objects.requireNonNull(chineseName, "chineseName");
        Objects.requireNonNull(gt6Class, "gt6Class");
        Objects.requireNonNull(recipeStatus, "recipeStatus");
        if (capacity <= 0L) {
            throw new IllegalArgumentException("capacity must be positive for " + id);
        }
        if (art.isBlank() || englishName.isBlank()) {
            throw new IllegalArgumentException("blank art or name for " + id);
        }
    }

    public String path() {
        return id.getPath();
    }

    /** Explicit NBT melting point, otherwise no invented melt. */
    public long meltingPoint() {
        return meltingKelvin == null ? Long.MAX_VALUE : meltingKelvin;
    }

    public boolean onlySimple() {
        return kind.onlySimple();
    }

    public boolean canSeal() {
        return kind.canSeal();
    }

    public boolean keepsFilter() {
        return kind.keepsFilter();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }
}
