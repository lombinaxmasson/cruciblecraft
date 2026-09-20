package com.masson.cruciblecraft.energy.cooler;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** Bundled catalog of the ten GT6 electric and flux coolers. */
public final class CoolerCatalog {
    private static final String RESOURCE = "/data/cruciblecraft/coolers.json";
    private static final Map<ResourceLocation, CoolerProfile> PROFILES =
            loadBundled();

    public static List<CoolerProfile> profiles() {
        return List.copyOf(PROFILES.values());
    }

    public static CoolerProfile require(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        CoolerProfile profile = parsed == null ? null : PROFILES.get(parsed);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown cooler profile " + id);
        }
        return profile;
    }

    public static CoolerProfile require(ResourceLocation id) {
        CoolerProfile profile = PROFILES.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown cooler profile " + id);
        }
        return profile;
    }

    private static Map<ResourceLocation, CoolerProfile> loadBundled() {
        Document document = CatalogJson.readBundled(
                CoolerCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.machines == null
                || document.machines.size() != CoolerProfile.EXPECTED_SIZE) {
            throw new IllegalStateException("Invalid cooler catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        LinkedHashMap<ResourceLocation, CoolerProfile> result =
                new LinkedHashMap<>();
        Set<Integer> sourceIds = new HashSet<>();
        int electric = 0;
        int flux = 0;
        for (MachineRow row : document.machines) {
            CoolerProfile profile = row.toProfile();
            if (result.putIfAbsent(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate cooler profile " + profile.id());
            }
            if (!sourceIds.add(profile.sourceId())) {
                throw new IllegalStateException(
                        "Duplicate cooler source id " + profile.sourceId());
            }
            if (profile.electric()) {
                electric++;
            } else {
                flux++;
            }
        }
        CoolerProfile lv = result.get(ResourceLocation.parse(
                "cruciblecraft:thermoelectric_cooler_lv"));
        CoolerProfile iv = result.get(ResourceLocation.parse(
                "cruciblecraft:thermoelectric_cooler_iv"));
        CoolerProfile lead = result.get(ResourceLocation.parse(
                "cruciblecraft:thermofluxic_cooler_lead"));
        CoolerProfile enderium = result.get(ResourceLocation.parse(
                "cruciblecraft:thermofluxic_cooler_enderium"));
        if (electric != 5
                || flux != 5
                || lv == null
                || lv.sourceId() != 10161
                || lv.nbtInput() != 32
                || lv.nbtOutput() != 8
                || !"steel_galvanized".equals(lv.material())
                || iv == null
                || iv.sourceId() != 10165
                || iv.nbtInput() != 8192
                || lead == null
                || lead.sourceId() != 11161
                || lead.nbtInput() != 128
                || lead.hostId() == null
                || !lead.hostId().equals(lv.id())
                || enderium == null
                || enderium.sourceId() != 11165
                || enderium.nbtInput() != 32768
                || !sourceIds.contains(10162)
                || !sourceIds.contains(11163)) {
            throw new IllegalStateException("Cooler catalog anchors drifted");
        }
        return Map.copyOf(result);
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
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
        private String voltage;
        private String material;
        @SerializedName("nbt_input")
        private int nbtInput;
        @SerializedName("nbt_output")
        private int nbtOutput;
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
        private RecipeRow recipe;

        private CoolerProfile toProfile() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(gt6Class)
                    || !CatalogJson.nonBlank(kind)
                    || !CatalogJson.nonBlank(material)
                    || !CatalogJson.nonBlank(langEn)
                    || !CatalogJson.nonBlank(langZh)
                    || !CatalogJson.nonBlank(textureFolder)
                    || recipe == null) {
                throw new IllegalStateException("Incomplete cooler row " + id);
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) {
                throw new IllegalStateException("Invalid cooler id " + id);
            }
            ResourceLocation host = null;
            if (CatalogJson.nonBlank(hostId)) {
                host = ResourceLocation.tryParse(hostId);
                if (host == null) {
                    throw new IllegalStateException(
                            "Invalid cooler host id " + hostId);
                }
            }
            return new CoolerProfile(
                    parsed,
                    sourceId,
                    sourceLine,
                    gt6Class,
                    kind,
                    voltage,
                    material,
                    nbtInput,
                    nbtOutput,
                    hardness,
                    resistance,
                    langEn,
                    langZh,
                    textureFolder,
                    host,
                    recipe.toRecipe());
        }
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;
        private List<String> catalysts;

        private CoolerProfile.Recipe toRecipe() {
            if (pattern == null || pattern.isEmpty() || keys == null) {
                throw new IllegalStateException("Incomplete cooler recipe");
            }
            LinkedHashMap<String, CoolerProfile.Ingredient> parsed =
                    new LinkedHashMap<>();
            keys.forEach((letter, row) ->
                    parsed.put(letter, row.toIngredient()));
            return new CoolerProfile.Recipe(
                    pattern, parsed, CatalogJson.list(catalysts));
        }
    }

    private static final class IngredientRow {
        private String item;
        private String prefix;
        private String material;
        private String family;

        private CoolerProfile.Ingredient toIngredient() {
            return new CoolerProfile.Ingredient(
                    item, prefix, material, family);
        }
    }

    private CoolerCatalog() {}
}
