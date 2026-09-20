package com.masson.cruciblecraft.energy.cooler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** One GT6 electric or flux cooler identity. */
public record CoolerProfile(
        ResourceLocation id,
        int sourceId,
        int sourceLine,
        String gt6Class,
        String kind,
        String voltage,
        String material,
        int nbtInput,
        int nbtOutput,
        float hardness,
        float resistance,
        String langEn,
        String langZh,
        String textureFolder,
        ResourceLocation hostId,
        Recipe recipe) {
    public static final int EXPECTED_SIZE = 10;
    public static final String ELECTRIC = "electric";
    public static final String FLUX = "flux";

    public CoolerProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(gt6Class, "gt6Class");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(langEn, "langEn");
        Objects.requireNonNull(langZh, "langZh");
        Objects.requireNonNull(textureFolder, "textureFolder");
        Objects.requireNonNull(recipe, "recipe");
        if (sourceId <= 0
                || sourceLine <= 0
                || nbtInput <= 0
                || nbtOutput <= 0
                || hardness <= 0.0F
                || resistance <= 0.0F) {
            throw new IllegalArgumentException(
                    "Cooler profile source and rates must be valid");
        }
        if (ELECTRIC.equals(kind)) {
            if (!"MultiTileEntityCoolerElectric".equals(gt6Class)
                    || voltage == null
                    || voltage.isBlank()
                    || hostId != null) {
                throw new IllegalArgumentException(
                        "Electric cooler row is malformed " + id);
            }
        } else if (FLUX.equals(kind)) {
            if (!"MultiTileEntityCoolerFlux".equals(gt6Class)
                    || voltage != null
                    || hostId == null) {
                throw new IllegalArgumentException(
                        "Flux cooler row is malformed " + id);
            }
        } else {
            throw new IllegalArgumentException("Unknown cooler kind " + kind);
        }
    }

    public boolean electric() {
        return ELECTRIC.equals(kind);
    }

    public boolean flux() {
        return FLUX.equals(kind);
    }

    public boolean switchableMode() {
        return electric();
    }

    public long energyCapacity() {
        return Math.multiplyExact((long) nbtInput, 2L);
    }

    public long inputMaximum() {
        return energyCapacity();
    }

    public long outputMinimum() {
        return Math.max(1L, nbtOutput / 2L);
    }

    public long outputMaximum() {
        return Math.multiplyExact((long) nbtOutput, 2L);
    }

    public record Recipe(
            List<String> pattern,
            Map<String, Ingredient> keys,
            List<String> catalysts) {
        public Recipe {
            pattern = List.copyOf(pattern);
            keys = Map.copyOf(keys);
            catalysts = List.copyOf(catalysts);
        }
    }

    public record Ingredient(
            String item, String prefix, String material, String family) {
        public Ingredient {
            boolean hasItem = item != null && !item.isBlank();
            boolean hasPart = prefix != null && material != null;
            if (hasItem == hasPart) {
                throw new IllegalArgumentException(
                        "Cooler recipe key needs item or prefix+material");
            }
            if (hasPart && (prefix.isBlank() || material.isBlank())) {
                throw new IllegalArgumentException(
                        "Cooler recipe key needs prefix+material");
            }
        }
    }
}
