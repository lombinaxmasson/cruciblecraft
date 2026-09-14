package com.masson.cruciblecraft.energy.quantum;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** One Quantum Energizer host: LU in, QU out at half rate. */
public record QuantumEnergizerProfile(
        ResourceLocation id,
        int sourceId,
        int sourceLine,
        String sourcePolicy,
        String gt6Class,
        String material,
        long luInput,
        long quOutput,
        String langEn,
        String langZh,
        Recipe recipe) {
    public QuantumEnergizerProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sourcePolicy, "sourcePolicy");
        Objects.requireNonNull(gt6Class, "gt6Class");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(langEn, "langEn");
        Objects.requireNonNull(langZh, "langZh");
        Objects.requireNonNull(recipe, "recipe");
        if (luInput <= 0L || quOutput <= 0L || sourceId <= 0 || sourceLine < 0) {
            throw new IllegalArgumentException("Invalid quantum energizer " + id);
        }
        if (quOutput != luInput / 2L) {
            throw new IllegalArgumentException(
                    "Quantum energizer ratio must be 1/2: " + id);
        }
    }

    public boolean extension() {
        return "CC_EXTENSION".equals(sourcePolicy);
    }

    public long luCapacity() {
        return Math.multiplyExact(luInput, 4L);
    }

    public long quCapacity() {
        return Math.multiplyExact(quOutput, 4L);
    }

    public record Recipe(
            List<String> pattern,
            Map<String, Ingredient> keys) {
        public Recipe {
            pattern = List.copyOf(pattern);
            keys = Map.copyOf(keys);
        }
    }

    public record Ingredient(String item, String prefix, String material) {}
}
