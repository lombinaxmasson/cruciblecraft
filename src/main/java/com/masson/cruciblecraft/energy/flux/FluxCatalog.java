package com.masson.cruciblecraft.energy.flux;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** Bundled catalog of the thirty GT6 flux converters. */
public final class FluxCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/flux_converters.json";
    private static final Map<ResourceLocation, FluxProfile> PROFILES =
            loadBundled();

    public static List<FluxProfile> profiles() {
        return List.copyOf(PROFILES.values());
    }

    public static FluxProfile require(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        FluxProfile profile = parsed == null ? null : PROFILES.get(parsed);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown flux profile " + id);
        }
        return profile;
    }

    public static FluxProfile require(ResourceLocation id) {
        FluxProfile profile = PROFILES.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown flux profile " + id);
        }
        return profile;
    }

    private static Map<ResourceLocation, FluxProfile> loadBundled() {
        Document document = CatalogJson.readBundled(
                FluxCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.rfPerEu != 4
                || document.machines == null
                || document.machines.size() != FluxProfile.EXPECTED_SIZE) {
            throw new IllegalStateException("Invalid flux converter catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        LinkedHashMap<ResourceLocation, FluxProfile> result =
                new LinkedHashMap<>();
        Set<Integer> sourceIds = new HashSet<>();
        int heaters = 0;
        int engines = 0;
        int motors = 0;
        int magnets = 0;
        int lasers = 0;
        int dynamos = 0;
        int liveRecipes = 0;
        for (MachineRow row : document.machines) {
            FluxProfile profile = row.toProfile();
            if (result.putIfAbsent(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate flux profile " + profile.id());
            }
            if (!sourceIds.add(profile.sourceId())) {
                throw new IllegalStateException(
                        "Duplicate flux source id " + profile.sourceId());
            }
            switch (profile.kind()) {
                case FluxProfile.HEATER -> heaters++;
                case FluxProfile.ENGINE -> engines++;
                case FluxProfile.MOTOR -> motors++;
                case FluxProfile.MAGNET -> magnets++;
                case FluxProfile.LASER -> lasers++;
                case FluxProfile.DYNAMO -> dynamos++;
                default -> throw new IllegalStateException(
                        "Unknown flux kind " + profile.kind());
            }
            if (profile.recipeLive()) {
                liveRecipes++;
            }
        }
        FluxProfile leadHeater = result.get(ResourceLocation.parse(
                "cruciblecraft:flux_heater_lead"));
        FluxProfile enderiumDynamo = result.get(ResourceLocation.parse(
                "cruciblecraft:flux_dynamo_enderium"));
        FluxProfile leadMagnet = result.get(ResourceLocation.parse(
                "cruciblecraft:flux_magnet_lead"));
        if (heaters != 5
                || engines != 5
                || motors != 5
                || magnets != 5
                || lasers != 5
                || dynamos != 5
                || liveRecipes != 20
                || leadHeater == null
                || leadHeater.sourceId() != 11001
                || leadHeater.nbtInput() != 128
                || leadHeater.nbtOutput() != 16
                || !"lead".equals(leadHeater.material())
                || enderiumDynamo == null
                || enderiumDynamo.sourceId() != 11115
                || enderiumDynamo.nbtInput() != 8192
                || enderiumDynamo.nbtOutput() != 22528
                || leadMagnet == null
                || leadMagnet.recipeLive()
                || !sourceIds.contains(11021)
                || !sourceIds.contains(11101)) {
            throw new IllegalStateException(
                    "Flux converter catalog anchors drifted");
        }
        return Map.copyOf(result);
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        @SerializedName("rf_per_eu")
        private int rfPerEu;
        private List<MachineRow> machines;
    }

    private static final class MachineRow {
        private String id;
        @SerializedName("source_id")
        private int sourceId;
        @SerializedName("source_line")
        private int sourceLine;
        @SerializedName("gt6_class")
        private String gt6Class;
        private String kind;
        private String material;
        @SerializedName("nbt_input")
        private int nbtInput;
        @SerializedName("nbt_output")
        private int nbtOutput;
        private String accepts;
        private String emits;
        private float hardness;
        private float resistance;
        @SerializedName("lang_en")
        private String langEn;
        @SerializedName("lang_zh")
        private String langZh;
        @SerializedName("texture_folder")
        private String textureFolder;
        @SerializedName("host_id")
        private String hostId;
        @SerializedName("recipe_live")
        private boolean recipeLive;
        private RecipeRow recipe;

        private FluxProfile toProfile() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(gt6Class)
                    || !CatalogJson.nonBlank(kind)
                    || !CatalogJson.nonBlank(material)
                    || !CatalogJson.nonBlank(accepts)
                    || !CatalogJson.nonBlank(emits)
                    || !CatalogJson.nonBlank(langEn)
                    || !CatalogJson.nonBlank(langZh)
                    || !CatalogJson.nonBlank(textureFolder)
                    || !CatalogJson.nonBlank(hostId)
                    || recipe == null) {
                throw new IllegalStateException("Incomplete flux row " + id);
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            ResourceLocation host = ResourceLocation.tryParse(hostId);
            if (parsed == null || host == null) {
                throw new IllegalStateException("Invalid flux ids " + id);
            }
            return new FluxProfile(
                    parsed,
                    sourceId,
                    sourceLine,
                    gt6Class,
                    kind,
                    material,
                    nbtInput,
                    nbtOutput,
                    accepts,
                    emits,
                    hardness,
                    resistance,
                    langEn,
                    langZh,
                    textureFolder,
                    host,
                    recipeLive,
                    recipe.toRecipe());
        }
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;
        private List<String> catalysts;

        private FluxProfile.Recipe toRecipe() {
            if (pattern == null || pattern.isEmpty() || keys == null) {
                throw new IllegalStateException("Incomplete flux recipe");
            }
            LinkedHashMap<String, FluxProfile.Ingredient> parsed =
                    new LinkedHashMap<>();
            keys.forEach((letter, row) ->
                    parsed.put(letter, row.toIngredient()));
            return new FluxProfile.Recipe(
                    pattern, parsed, CatalogJson.list(catalysts));
        }
    }

    private static final class IngredientRow {
        private String item;
        private String prefix;
        private String material;

        private FluxProfile.Ingredient toIngredient() {
            return new FluxProfile.Ingredient(item, prefix, material);
        }
    }

    private FluxCatalog() {}
}
