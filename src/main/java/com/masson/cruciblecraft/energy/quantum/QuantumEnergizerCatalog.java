package com.masson.cruciblecraft.energy.quantum;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** T1–T5 source-backed Quantum Energizers plus the OMEGA CC extension. */
public final class QuantumEnergizerCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/quantum_energizers.json";
    private static final Catalog CATALOG = loadBundled();

    public static List<QuantumEnergizerProfile> profiles() {
        return CATALOG.byId.values().stream().toList();
    }

    public static QuantumEnergizerProfile require(ResourceLocation id) {
        QuantumEnergizerProfile profile = CATALOG.byId.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown quantum energizer " + id);
        }
        return profile;
    }

    private record Catalog(Map<ResourceLocation, QuantumEnergizerProfile> byId) {}

    private static Catalog loadBundled() {
        Document document = CatalogJson.readBundled(
                QuantumEnergizerCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.machines == null
                || document.expectedSize != 6
                || document.machines.size() != 6) {
            throw new IllegalStateException("Invalid quantum energizer catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        LinkedHashMap<ResourceLocation, QuantumEnergizerProfile> byId =
                new LinkedHashMap<>();
        for (MachineRow row : document.machines) {
            QuantumEnergizerProfile profile = row.toProfile();
            if (byId.put(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate quantum energizer " + profile.id());
            }
        }
        return new Catalog(Map.copyOf(byId));
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        @SerializedName("expected_size")
        private int expectedSize;
        private List<MachineRow> machines;
    }

    private static final class MachineRow {
        private String id;
        @SerializedName("source_id")
        private int sourceId;
        @SerializedName("source_line")
        private int sourceLine;
        @SerializedName("source_policy")
        private String sourcePolicy;
        @SerializedName("gt6_class")
        private String gt6Class;
        private String material;
        @SerializedName("lu_input")
        private long luInput;
        @SerializedName("qu_output")
        private long quOutput;
        @SerializedName("lang_en")
        private String langEn;
        @SerializedName("lang_zh")
        private String langZh;
        private RecipeRow recipe;

        private QuantumEnergizerProfile toProfile() {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null || recipe == null
                    || !CatalogJson.nonBlank(langEn)
                    || !CatalogJson.nonBlank(langZh)) {
                throw new IllegalStateException("Incomplete quantum energizer " + id);
            }
            return new QuantumEnergizerProfile(
                    parsed,
                    sourceId,
                    sourceLine,
                    sourcePolicy,
                    gt6Class,
                    material,
                    luInput,
                    quOutput,
                    langEn,
                    langZh,
                    recipe.toRecipe());
        }
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;

        private QuantumEnergizerProfile.Recipe toRecipe() {
            LinkedHashMap<String, QuantumEnergizerProfile.Ingredient> parsed =
                    new LinkedHashMap<>();
            if (keys != null) {
                keys.forEach((letter, row) -> parsed.put(letter, row.toIngredient()));
            }
            return new QuantumEnergizerProfile.Recipe(
                    pattern == null ? List.of() : pattern, parsed);
        }
    }

    private static final class IngredientRow {
        private String item;
        private String prefix;
        private String material;

        private QuantumEnergizerProfile.Ingredient toIngredient() {
            return new QuantumEnergizerProfile.Ingredient(item, prefix, material);
        }
    }

    private QuantumEnergizerCatalog() {}
}
