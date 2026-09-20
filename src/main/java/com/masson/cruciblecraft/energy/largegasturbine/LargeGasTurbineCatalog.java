package com.masson.cruciblecraft.energy.largegasturbine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** GT6 LargeTurbineGas 17231–17234. Not kTFRUAddon 10000–10006. */
public final class LargeGasTurbineCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/large_gas_turbines.json";
    private static final Catalog CATALOG = loadBundled();

    public static List<Profile> profiles() {
        return CATALOG.byId.values().stream().toList();
    }

    public static Optional<Profile> find(ResourceLocation id) {
        return Optional.ofNullable(CATALOG.byId.get(id));
    }

    public static Profile require(ResourceLocation id) {
        Profile profile = CATALOG.byId.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown large gas turbine " + id);
        }
        return profile;
    }

    public record Profile(
            ResourceLocation id,
            int sourceId,
            long huInput,
            long ruOutput,
            ResourceLocation wallId,
            Recipe recipe,
            String langEn,
            String langZh) {
        public Profile {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(wallId, "wallId");
            Objects.requireNonNull(recipe, "recipe");
            Objects.requireNonNull(langEn, "langEn");
            Objects.requireNonNull(langZh, "langZh");
            if (huInput <= 0L || ruOutput <= 0L) {
                throw new IllegalArgumentException(
                        "Large gas turbine rates must be positive: " + id);
            }
        }

        /** GT6 {@code NBT_INPUT * 2} capacitor. */
        public long capacitor() {
            return Math.multiplyExact(huInput, 2L);
        }

        public long inputMin() {
            return huInput / 2L;
        }

        public long inputRec() {
            return huInput;
        }

        public long inputMax() {
            return Math.multiplyExact(huInput, 2L);
        }

        public long outputMin() {
            return ruOutput / 2L;
        }

        public long outputRec() {
            return ruOutput;
        }

        public long outputMax() {
            return Math.multiplyExact(ruOutput, 2L);
        }

        public int inputCapacityMb() {
            return Math.toIntExact(Math.min(
                    Integer.MAX_VALUE, Math.multiplyExact(inputMax(), 4L)));
        }

        public int outputCapacityMb() {
            return Math.toIntExact(Math.min(
                    Integer.MAX_VALUE, Math.multiplyExact(inputMax(), 16L)));
        }
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

    public record Ingredient(String item, String prefix, String material) {}

    private record Catalog(Map<ResourceLocation, Profile> byId) {}

    private static Catalog loadBundled() {
        Document document = CatalogJson.readBundled(
                LargeGasTurbineCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.machines == null
                || document.expectedCount != 4) {
            throw new IllegalStateException("Invalid large gas turbine catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        LinkedHashMap<ResourceLocation, Profile> byId = new LinkedHashMap<>();
        for (MachineRow row : document.machines) {
            Profile profile = row.toProfile();
            if (byId.put(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate large gas turbine " + profile.id());
            }
        }
        if (byId.size() != 4) {
            throw new IllegalStateException(
                    "Large gas turbine count drifted: " + byId.size());
        }
        return new Catalog(Map.copyOf(byId));
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        @SerializedName("expected_count")
        private int expectedCount;
        private List<MachineRow> machines;
    }

    private static final class MachineRow {
        private String id;
        @SerializedName("source_id")
        private int sourceId;
        @SerializedName("hu_input")
        private long huInput;
        @SerializedName("ru_output")
        private long ruOutput;
        @SerializedName("wall_id")
        private String wallId;
        @SerializedName("lang_en")
        private String langEn;
        @SerializedName("lang_zh")
        private String langZh;
        private RecipeRow recipe;

        private Profile toProfile() {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null || recipe == null
                    || !CatalogJson.nonBlank(wallId)
                    || !CatalogJson.nonBlank(langEn)
                    || !CatalogJson.nonBlank(langZh)) {
                throw new IllegalStateException("Incomplete large gas turbine " + id);
            }
            return new Profile(
                    parsed,
                    sourceId,
                    huInput,
                    ruOutput,
                    ResourceLocation.parse(wallId),
                    recipe.toRecipe(),
                    langEn,
                    langZh);
        }
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;
        private String catalyst;

        private Recipe toRecipe() {
            LinkedHashMap<String, Ingredient> parsed = new LinkedHashMap<>();
            if (keys != null) {
                keys.forEach((letter, row) -> parsed.put(letter, row.toIngredient()));
            }
            List<String> tools = CatalogJson.nonBlank(catalyst)
                    ? List.of(catalyst)
                    : List.of();
            return new Recipe(
                    pattern == null ? List.of() : pattern, parsed, tools);
        }
    }

    private static final class IngredientRow {
        private String item;
        private String prefix;
        private String material;

        private Ingredient toIngredient() {
            return new Ingredient(item, prefix, material);
        }
    }

    private LargeGasTurbineCatalog() {}
}
