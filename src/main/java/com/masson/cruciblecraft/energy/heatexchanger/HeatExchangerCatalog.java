package com.masson.cruciblecraft.energy.heatexchanger;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/** Bundled catalog of the eight single-block GT6 heat exchangers. */
public final class HeatExchangerCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/heat_exchangers.json";
    private static final Map<ResourceLocation, HeatExchangerProfile> PROFILES =
            loadBundled();

    public static List<HeatExchangerProfile> profiles() {
        return List.copyOf(PROFILES.values());
    }

    public static HeatExchangerProfile require(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        HeatExchangerProfile profile =
                parsed == null ? null : PROFILES.get(parsed);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown heat exchanger profile " + id);
        }
        return profile;
    }

    public static HeatExchangerProfile require(ResourceLocation id) {
        HeatExchangerProfile profile = PROFILES.get(id);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown heat exchanger profile " + id);
        }
        return profile;
    }

    private static Map<ResourceLocation, HeatExchangerProfile> loadBundled() {
        Document document = CatalogJson.readBundled(
                HeatExchangerCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.machines == null
                || document.machines.size() != HeatExchangerProfile.EXPECTED_SIZE) {
            throw new IllegalStateException("Invalid heat exchanger catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        LinkedHashMap<ResourceLocation, HeatExchangerProfile> result =
                new LinkedHashMap<>();
        Set<Integer> sourceIds = new HashSet<>();
        for (MachineRow row : document.machines) {
            HeatExchangerProfile profile = row.toProfile();
            if (result.putIfAbsent(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate heat exchanger profile " + profile.id());
            }
            if (!sourceIds.add(profile.sourceId())) {
                throw new IllegalStateException(
                        "Duplicate heat exchanger source id "
                                + profile.sourceId());
            }
            if (profile.energyType() != EnergyType.HEAT) {
                throw new IllegalStateException(
                        "Heat exchanger is not HU " + profile.id());
            }
        }
        HeatExchangerProfile invar = result.get(ResourceLocation.parse(
                "cruciblecraft:heat_exchanger_invar"));
        HeatExchangerProfile tungstensteel = result.get(ResourceLocation.parse(
                "cruciblecraft:heat_exchanger_tungstensteel"));
        HeatExchangerProfile denseTantalum = result.get(ResourceLocation.parse(
                "cruciblecraft:dense_heat_exchanger_tantalum_hafnium_carbide"));
        if (invar == null
                || invar.sourceId() != 9103
                || invar.huRate() != 16
                || invar.efficiencyBps() != 10_000
                || tungstensteel == null
                || tungstensteel.sourceId() != 9108
                || tungstensteel.huRate() != 128
                || tungstensteel.efficiencyBps() != 9_000
                || denseTantalum == null
                || denseTantalum.sourceId() != 9159
                || denseTantalum.huRate() != 1024
                || !sourceIds.contains(9107)
                || sourceIds.contains(17197)) {
            throw new IllegalStateException(
                    "Heat exchanger catalog anchors drifted");
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
        private boolean dense;
        private String material;
        @SerializedName("any_w")
        private boolean anyW;
        @SerializedName("hu_rate")
        private int huRate;
        @SerializedName("efficiency_bps")
        private int efficiencyBps;
        private float hardness;
        private float resistance;
        @SerializedName("lang_en")
        private String langEn;
        @SerializedName("lang_zh")
        private String langZh;
        private RecipeRow recipe;

        private HeatExchangerProfile toProfile() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(gt6Class)
                    || !CatalogJson.nonBlank(material)
                    || !CatalogJson.nonBlank(langEn)
                    || !CatalogJson.nonBlank(langZh)
                    || recipe == null) {
                throw new IllegalStateException(
                        "Incomplete heat exchanger row " + id);
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) {
                throw new IllegalStateException(
                        "Invalid heat exchanger id " + id);
            }
            return new HeatExchangerProfile(
                    parsed,
                    sourceId,
                    sourceLine,
                    gt6Class,
                    dense,
                    material,
                    anyW,
                    huRate,
                    efficiencyBps,
                    hardness,
                    resistance,
                    langEn,
                    langZh,
                    recipe.toRecipe());
        }
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;
        private String catalyst;

        private HeatExchangerProfile.Recipe toRecipe() {
            if (pattern == null
                    || pattern.isEmpty()
                    || keys == null
                    || !CatalogJson.nonBlank(catalyst)) {
                throw new IllegalStateException(
                        "Incomplete heat exchanger recipe");
            }
            LinkedHashMap<String, HeatExchangerProfile.Ingredient> parsed =
                    new LinkedHashMap<>();
            keys.forEach((letter, row) ->
                    parsed.put(letter, row.toIngredient()));
            return new HeatExchangerProfile.Recipe(pattern, parsed, catalyst);
        }
    }

    private static final class IngredientRow {
        private String prefix;
        private String material;
        private String family;

        private HeatExchangerProfile.Ingredient toIngredient() {
            if (!CatalogJson.nonBlank(prefix)
                    || !CatalogJson.nonBlank(material)) {
                throw new IllegalStateException(
                        "Heat exchanger recipe key needs prefix+material");
            }
            return new HeatExchangerProfile.Ingredient(
                    prefix, material, family);
        }
    }

    private HeatExchangerCatalog() {}
}
