package com.masson.cruciblecraft.energy.largeheatexchanger;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import net.minecraft.resources.ResourceLocation;

/** GT6 {@code MultiTileEntityLargeHeatExchanger} 17197 plus transmitter 18101. */
public record LargeHeatExchangerProfile(
        ResourceLocation id,
        int sourceId,
        int sourceLine,
        String gt6Class,
        String sourcePolicy,
        int huRate,
        int efficiencyBps,
        int packetSize,
        ResourceLocation wallId,
        int wallSourceId,
        ResourceLocation transmitterId,
        int transmitterSourceId,
        String langEn,
        String langZh,
        Recipe recipe,
        Recipe transmitterRecipe) {
    public LargeHeatExchangerProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(gt6Class, "gt6Class");
        Objects.requireNonNull(sourcePolicy, "sourcePolicy");
        Objects.requireNonNull(wallId, "wallId");
        Objects.requireNonNull(transmitterId, "transmitterId");
        Objects.requireNonNull(langEn, "langEn");
        Objects.requireNonNull(langZh, "langZh");
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(transmitterRecipe, "transmitterRecipe");
        if (sourceId != 17197
                || wallSourceId != 18024
                || transmitterSourceId != 18101
                || huRate != 16_384
                || efficiencyBps != 10_000
                || packetSize != 1
                || !"source_backed".equals(sourcePolicy)
                || !"MultiTileEntityLargeHeatExchanger".equals(gt6Class)) {
            throw new IllegalArgumentException(
                    "Large heat exchanger catalog drifted from GT6 17197");
        }
    }

    public EnergyType energyType() {
        return EnergyType.HEAT;
    }

    public long packetSizeLong() {
        return packetSize;
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

    public long huFromRecipe(GTRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        long raw = Math.multiplyExact(
                Math.abs(recipe.eut()), (long) recipe.duration());
        return Math.multiplyExact(raw, (long) efficiencyBps) / 10_000L;
    }

    public record Recipe(
            List<String> pattern,
            Map<String, Ingredient> keys,
            List<String> catalysts) {
        public Recipe {
            pattern = List.copyOf(pattern);
            keys = Map.copyOf(keys);
            catalysts = catalysts == null ? List.of() : List.copyOf(catalysts);
        }
    }

    public record Ingredient(String item, String prefix, String material, String family) {}
}
