package com.masson.cruciblecraft.energy.heatexchanger;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import net.minecraft.resources.ResourceLocation;

/** One GT6 {@code MultiTileEntityGeneratorHotFluid} identity. */
public record HeatExchangerProfile(
        ResourceLocation id,
        int sourceId,
        int sourceLine,
        String gt6Class,
        boolean dense,
        String material,
        boolean anyW,
        int huRate,
        int efficiencyBps,
        float hardness,
        float resistance,
        String langEn,
        String langZh,
        Recipe recipe) {
    public static final int EXPECTED_SIZE = 8;

    public HeatExchangerProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(gt6Class, "gt6Class");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(langEn, "langEn");
        Objects.requireNonNull(langZh, "langZh");
        Objects.requireNonNull(recipe, "recipe");
        if (sourceId <= 0
                || sourceLine <= 0
                || huRate <= 0
                || efficiencyBps <= 0
                || efficiencyBps > 10_000
                || hardness <= 0.0F
                || resistance <= 0.0F
                || !"MultiTileEntityGeneratorHotFluid".equals(gt6Class)) {
            throw new IllegalArgumentException(
                    "Heat exchanger profile source and rate must be valid");
        }
    }

    public EnergyType energyType() {
        return EnergyType.HEAT;
    }

    public long packetSize() {
        return 1L;
    }

    public long maximumOutputPacketsPerTick() {
        return huRate;
    }

    public long energyCapacity() {
        return Math.addExact(Math.multiplyExact((long) huRate, 4L), 256L);
    }

    public int inputCapacityMb() {
        return Math.multiplyExact(huRate, 10);
    }

    public int outputCapacityMb() {
        return Math.multiplyExact(huRate, 20);
    }

    public long consumeThreshold() {
        return Math.multiplyExact((long) huRate, 2L);
    }

    public String textureFolder() {
        return "block/machine/heat_exchanger";
    }

    public long huFromRecipe(GTRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        long raw = Math.multiplyExact(
                Math.abs(recipe.eut()), (long) recipe.duration());
        return Math.multiplyExact(raw, (long) efficiencyBps) / 10_000L;
    }

    public record Recipe(
            List<String> pattern,
            Map<String, Ingredient> keys,
            String catalyst) {
        public Recipe {
            pattern = List.copyOf(pattern);
            keys = Map.copyOf(keys);
            Objects.requireNonNull(catalyst, "catalyst");
        }
    }

    public record Ingredient(String prefix, String material, String family) {
        public Ingredient {
            Objects.requireNonNull(prefix, "prefix");
            Objects.requireNonNull(material, "material");
        }
    }
}
