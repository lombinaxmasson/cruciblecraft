package com.masson.cruciblecraft.energy.battery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** Source-backed battery variants: 37 Loader_MultiTileEntities rows. */
public final class EnergyBatteryTierCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/energy_battery_tiers.json";
    public static final int EXPECTED_SIZE = 37;
    private static final EnergyBatteryTierCatalog BUNDLED = loadBundled();

    private final List<Entry> entries;
    private final Map<ResourceLocation, Entry> byId;

    public static List<Entry> entries() {
        return BUNDLED.entries;
    }

    public static Entry require(ResourceLocation id) {
        Entry entry = BUNDLED.byId.get(id);
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Unknown energy battery variant " + id);
        }
        return entry;
    }

    public static Entry require(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "Unknown energy battery variant " + id);
        }
        return require(parsed);
    }

    private EnergyBatteryTierCatalog(List<Entry> entries) {
        this.entries = List.copyOf(entries);
        LinkedHashMap<ResourceLocation, Entry> indexed = new LinkedHashMap<>();
        for (Entry entry : this.entries) {
            if (indexed.putIfAbsent(entry.id(), entry) != null) {
                throw new IllegalStateException(
                        "Duplicate energy battery variant " + entry.id());
            }
        }
        this.byId = Map.copyOf(indexed);
    }

    private static EnergyBatteryTierCatalog loadBundled() {
        Document document = CatalogJson.readBundled(
                EnergyBatteryTierCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.tiers == null
                || document.tiers.size() != EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Invalid energy battery tier catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        List<Entry> entries = document.tiers.stream()
                .map(TierRow::toEntry)
                .toList();
        return new EnergyBatteryTierCatalog(entries);
    }

    public record Entry(
            ResourceLocation id,
            ResourceLocation kindId,
            String voltage,
            int voltageIndex,
            long inputSize,
            long capacity,
            String energy,
            int sourceId,
            int sourceLine,
            String gt6Class,
            int[] voxel,
            int displayScaleMax,
            Recipe recipe) {
        public Entry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(kindId, "kindId");
            Objects.requireNonNull(voltage, "voltage");
            Objects.requireNonNull(energy, "energy");
            Objects.requireNonNull(gt6Class, "gt6Class");
            Objects.requireNonNull(voxel, "voxel");
            Objects.requireNonNull(recipe, "recipe");
            if (sourceId <= 0
                    || sourceLine <= 0
                    || inputSize <= 0L
                    || capacity <= 0L
                    || voltageIndex < 0
                    || displayScaleMax <= 0
                    || voxel.length != 6) {
                throw new IllegalArgumentException(
                        "Battery tier source, window and voxel must be valid");
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
        private String voltage;
        @SerializedName("voltage_index")
        private int voltageIndex;
        @SerializedName("input_size")
        private long inputSize;
        private long capacity;
        private String energy;
        @SerializedName("source_id")
        private int sourceId;
        @SerializedName("source_line")
        private int sourceLine;
        @SerializedName("gt6_class")
        private String gt6Class;
        private int[] voxel;
        @SerializedName("display_scale_max")
        private int displayScaleMax;
        private RecipeRow recipe;

        private Entry toEntry() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(kind)
                    || !CatalogJson.nonBlank(voltage)
                    || !CatalogJson.nonBlank(energy)
                    || !CatalogJson.nonBlank(gt6Class)
                    || voxel == null
                    || recipe == null) {
                throw new IllegalStateException(
                        "Incomplete energy battery tier " + id);
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            ResourceLocation kindId = ResourceLocation.tryParse(kind);
            if (parsed == null || kindId == null) {
                throw new IllegalStateException(
                        "Invalid energy battery ids " + id + " / " + kind);
            }
            return new Entry(
                    parsed,
                    kindId,
                    voltage,
                    voltageIndex,
                    inputSize,
                    capacity,
                    energy,
                    sourceId,
                    sourceLine,
                    gt6Class,
                    voxel.clone(),
                    displayScaleMax,
                    recipe.toRecipe());
        }
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;

        private Recipe toRecipe() {
            if (pattern == null || pattern.isEmpty() || keys == null) {
                throw new IllegalStateException("Incomplete battery recipe");
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
                        "Battery recipe key needs item or prefix+material");
            }
            return new Ingredient(prefix, material, null);
        }
    }
}
