package com.masson.cruciblecraft.energy.transformer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** Source-backed transformer variants: 9 Loader_MultiTileEntities rows. */
public final class EnergyTransformerTierCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/energy_transformer_tiers.json";
    public static final int EXPECTED_SIZE = 9;
    private static final EnergyTransformerTierCatalog BUNDLED = loadBundled();

    private final List<Entry> entries;
    private final Map<ResourceLocation, Entry> byId;

    public static List<Entry> entries() {
        return BUNDLED.entries;
    }

    public static Entry require(ResourceLocation id) {
        Entry entry = BUNDLED.byId.get(id);
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Unknown energy transformer variant " + id);
        }
        return entry;
    }

    public static Entry findByPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return BUNDLED.byId.get(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }

    private EnergyTransformerTierCatalog(List<Entry> entries) {
        this.entries = List.copyOf(entries);
        LinkedHashMap<ResourceLocation, Entry> indexed = new LinkedHashMap<>();
        for (Entry entry : this.entries) {
            if (indexed.putIfAbsent(entry.id(), entry) != null) {
                throw new IllegalStateException(
                        "Duplicate energy transformer variant " + entry.id());
            }
        }
        this.byId = Map.copyOf(indexed);
    }

    private static EnergyTransformerTierCatalog loadBundled() {
        Document document = CatalogJson.readBundled(
                EnergyTransformerTierCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.tiers == null
                || document.tiers.size() != EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Invalid energy transformer tier catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        List<Entry> entries = document.tiers.stream()
                .map(TierRow::toEntry)
                .toList();
        return new EnergyTransformerTierCatalog(entries);
    }

    public record Entry(
            ResourceLocation id,
            ResourceLocation kindId,
            String lowVoltage,
            String highVoltage,
            int voltageIndex,
            long inputSize,
            long outputSize,
            long multiplier,
            long capacity,
            String material,
            String energy,
            int sourceId,
            int sourceLine,
            String gt6Class,
            Recipe recipe) {
        public Entry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(kindId, "kindId");
            Objects.requireNonNull(lowVoltage, "lowVoltage");
            Objects.requireNonNull(highVoltage, "highVoltage");
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(energy, "energy");
            Objects.requireNonNull(gt6Class, "gt6Class");
            Objects.requireNonNull(recipe, "recipe");
            if (sourceId <= 0
                    || sourceLine <= 0
                    || inputSize <= 0L
                    || outputSize <= 0L
                    || multiplier <= 0L
                    || capacity <= 0L
                    || voltageIndex < 0) {
                throw new IllegalArgumentException(
                        "Transformer tier source and window must be valid");
            }
        }
    }

    public record Recipe(
            List<String> pattern,
            Map<String, Ingredient> keys) {
        public Recipe {
            pattern = List.copyOf(pattern);
            keys = Map.copyOf(keys);
        }
    }

    public record Ingredient(String prefix, String material, String item) {}

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        private List<TierRow> tiers;
    }

    private static final class TierRow {
        private String id;
        private String kind;
        @SerializedName("low_voltage")
        private String lowVoltage;
        @SerializedName("high_voltage")
        private String highVoltage;
        @SerializedName("voltage_index")
        private int voltageIndex;
        @SerializedName("input_size")
        private long inputSize;
        @SerializedName("output_size")
        private long outputSize;
        private long multiplier;
        private long capacity;
        private String material;
        private String energy;
        @SerializedName("source_id")
        private int sourceId;
        @SerializedName("source_line")
        private int sourceLine;
        @SerializedName("gt6_class")
        private String gt6Class;
        private RecipeRow recipe;

        private Entry toEntry() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(kind)
                    || !CatalogJson.nonBlank(lowVoltage)
                    || !CatalogJson.nonBlank(highVoltage)
                    || !CatalogJson.nonBlank(material)
                    || !CatalogJson.nonBlank(energy)
                    || !CatalogJson.nonBlank(gt6Class)
                    || recipe == null) {
                throw new IllegalStateException(
                        "Incomplete energy transformer tier " + id);
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            ResourceLocation kindId = ResourceLocation.tryParse(kind);
            if (parsed == null || kindId == null) {
                throw new IllegalStateException(
                        "Invalid energy transformer ids " + id + " / " + kind);
            }
            return new Entry(
                    parsed,
                    kindId,
                    lowVoltage,
                    highVoltage,
                    voltageIndex,
                    inputSize,
                    outputSize,
                    multiplier,
                    capacity,
                    material,
                    energy,
                    sourceId,
                    sourceLine,
                    gt6Class,
                    recipe.toRecipe());
        }
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;

        private Recipe toRecipe() {
            if (pattern == null || pattern.isEmpty() || keys == null) {
                throw new IllegalStateException("Incomplete transformer recipe");
            }
            LinkedHashMap<String, Ingredient> parsed = new LinkedHashMap<>();
            keys.forEach((letter, row) -> parsed.put(letter, row.toIngredient()));
            return new Recipe(pattern, parsed);
        }
    }

    private static final class IngredientRow {
        private String prefix;
        private String material;
        private String item;

        private Ingredient toIngredient() {
            if (CatalogJson.nonBlank(item)) {
                return new Ingredient(null, null, item);
            }
            if (!CatalogJson.nonBlank(prefix) || !CatalogJson.nonBlank(material)) {
                throw new IllegalStateException(
                        "Transformer recipe key needs item or prefix+material");
            }
            return new Ingredient(prefix, material, null);
        }
    }
}
