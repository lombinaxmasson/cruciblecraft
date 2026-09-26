package com.masson.cruciblecraft.energy.converter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** 179 GT6 loader rows, 7 DESIGN_POLICY turbines, and 16 laser/magnet/ZPM hosts. */
public final class EnergyConverterTierCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/energy_converter_tiers.json";
    public static final int GT6_LOADER_SIZE = 179;
    public static final int SMALL_GAS_TURBINE_SIZE = 7;
    public static final int LASER_MAGNET_ZPM_SIZE = 17;
    public static final int EXPECTED_SIZE =
            GT6_LOADER_SIZE + SMALL_GAS_TURBINE_SIZE + LASER_MAGNET_ZPM_SIZE;
    private static final EnergyConverterTierCatalog BUNDLED = loadBundled();

    private final List<Entry> entries;
    private final Map<ResourceLocation, Entry> byId;

    public static List<Entry> entries() {
        return BUNDLED.entries;
    }

    public static Entry require(ResourceLocation id) {
        Entry entry = BUNDLED.byId.get(id);
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Unknown energy converter variant " + id);
        }
        return entry;
    }

    public static Entry require(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "Unknown energy converter variant " + id);
        }
        return require(parsed);
    }

    public static Entry findByPath(String path) {
        Objects.requireNonNull(path, "path");
        return BUNDLED.byId.get(ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", path));
    }

    private EnergyConverterTierCatalog(List<Entry> entries) {
        this.entries = List.copyOf(entries);
        LinkedHashMap<ResourceLocation, Entry> indexed = new LinkedHashMap<>();
        for (Entry entry : this.entries) {
            if (indexed.putIfAbsent(entry.id(), entry) != null) {
                throw new IllegalStateException(
                        "Duplicate energy converter variant " + entry.id());
            }
        }
        this.byId = Map.copyOf(indexed);
    }

    private static EnergyConverterTierCatalog loadBundled() {
        Document document = CatalogJson.readBundled(
                EnergyConverterTierCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.steamPerEu != 2
                || document.tiers == null
                || document.tiers.size() != EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Invalid energy converter tier catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        List<Entry> entries = document.tiers.stream()
                .map(TierRow::toEntry)
                .toList();
        return new EnergyConverterTierCatalog(entries);
    }

    public record Entry(
            ResourceLocation id,
            ResourceLocation kindId,
            String material,
            int sourceId,
            int sourceLine,
            String gt6Class,
            String materialExpression,
            int nbtOutput,
            int nbtCapacity,
            int nbtInput,
            int efficiencyBps,
            String outputExpression,
            String fuelMap,
            Recipe recipe) {
        public Entry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(kindId, "kindId");
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(gt6Class, "gt6Class");
            Objects.requireNonNull(materialExpression, "materialExpression");
            Objects.requireNonNull(outputExpression, "outputExpression");
            Objects.requireNonNull(fuelMap, "fuelMap");
            Objects.requireNonNull(recipe, "recipe");
            if (sourceId <= 0 || sourceLine <= 0
                    || nbtOutput < 0
                    || nbtCapacity < 0
                    || nbtInput < 0) {
                throw new IllegalArgumentException(
                        "Converter tier source and NBT must be non-negative");
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

    public record Ingredient(
            String prefix,
            String material,
            String item,
            boolean catalyst) {}

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        @SerializedName("steam_per_eu")
        private int steamPerEu;
        private List<TierRow> tiers;
    }

    private static final class TierRow {
        private String id;
        private String kind;
        private String material;
        @SerializedName("source_id")
        private int sourceId;
        @SerializedName("source_line")
        private int sourceLine;
        @SerializedName("gt6_class")
        private String gt6Class;
        @SerializedName("material_expression")
        private String materialExpression;
        @SerializedName("nbt_output")
        private int nbtOutput;
        @SerializedName("nbt_capacity")
        private int nbtCapacity;
        @SerializedName("nbt_input")
        private int nbtInput;
        @SerializedName("efficiency_bps")
        private int efficiencyBps;
        @SerializedName("output_expression")
        private String outputExpression;
        @SerializedName("fuel_map")
        private String fuelMap;
        private RecipeRow recipe;

        private Entry toEntry() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(kind)
                    || !CatalogJson.nonBlank(material)
                    || !CatalogJson.nonBlank(gt6Class)
                    || !CatalogJson.nonBlank(materialExpression)
                    || !CatalogJson.nonBlank(outputExpression)
                    || !CatalogJson.nonBlank(fuelMap)
                    || recipe == null) {
                throw new IllegalStateException(
                        "Incomplete energy converter tier " + id);
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            ResourceLocation kindId = ResourceLocation.tryParse(kind);
            if (parsed == null || kindId == null) {
                throw new IllegalStateException(
                        "Invalid energy converter ids " + id + " / " + kind);
            }
            return new Entry(
                    parsed,
                    kindId,
                    material,
                    sourceId,
                    sourceLine,
                    gt6Class,
                    materialExpression,
                    nbtOutput,
                    nbtCapacity,
                    nbtInput,
                    efficiencyBps,
                    outputExpression,
                    fuelMap,
                    recipe.toRecipe());
        }
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;

        private Recipe toRecipe() {
            if (pattern == null || pattern.isEmpty() || keys == null) {
                throw new IllegalStateException("Incomplete converter recipe");
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
        private boolean catalyst;

        private Ingredient toIngredient() {
            if (CatalogJson.nonBlank(item)) {
                return new Ingredient(null, null, item, catalyst);
            }
            if (!CatalogJson.nonBlank(prefix) || !CatalogJson.nonBlank(material)) {
                throw new IllegalStateException(
                        "Converter recipe key needs item or prefix+material");
            }
            return new Ingredient(prefix, material, null, catalyst);
        }
    }
}
