package com.masson.cruciblecraft.energy.largeheatexchanger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** Bundled GT6 large heat exchanger 17197. Not the eight single-block HEX rows. */
public final class LargeHeatExchangerCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/large_heat_exchanger.json";
    private static final LargeHeatExchangerProfile PROFILE = loadBundled();

    public static LargeHeatExchangerProfile profile() {
        return PROFILE;
    }

    private static LargeHeatExchangerProfile loadBundled() {
        Document document = CatalogJson.readBundled(
                LargeHeatExchangerCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.machine == null
                || document.transmitter == null) {
            throw new IllegalStateException("Invalid large heat exchanger catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        return document.toProfile();
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        private MachineRow machine;
        private TransmitterRow transmitter;

        private LargeHeatExchangerProfile toProfile() {
            return new LargeHeatExchangerProfile(
                    parse(machine.id),
                    machine.sourceId,
                    machine.sourceLine,
                    machine.gt6Class,
                    machine.sourcePolicy,
                    machine.huRate,
                    machine.efficiencyBps,
                    machine.packetSize,
                    parse(machine.wallId),
                    machine.wallSourceId,
                    parse(transmitter.id),
                    transmitter.sourceId,
                    machine.langEn,
                    machine.langZh,
                    machine.recipe.toRecipe(),
                    transmitter.recipe.toRecipe());
        }
    }

    private static ResourceLocation parse(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            throw new IllegalStateException("Invalid large HEX id " + id);
        }
        return parsed;
    }

    private static final class MachineRow {
        private String id;
        @SerializedName("source_id")
        private int sourceId;
        @SerializedName("source_line")
        private int sourceLine;
        @SerializedName("gt6_class")
        private String gt6Class;
        @SerializedName("source_policy")
        private String sourcePolicy;
        @SerializedName("hu_rate")
        private int huRate;
        @SerializedName("efficiency_bps")
        private int efficiencyBps;
        @SerializedName("packet_size")
        private int packetSize;
        @SerializedName("wall_source_id")
        private int wallSourceId;
        @SerializedName("wall_id")
        private String wallId;
        @SerializedName("lang_en")
        private String langEn;
        @SerializedName("lang_zh")
        private String langZh;
        private RecipeRow recipe;
    }

    private static final class TransmitterRow {
        private String id;
        @SerializedName("source_id")
        private int sourceId;
        private RecipeRow recipe;
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;
        private String catalyst;
        private List<String> catalysts;

        private LargeHeatExchangerProfile.Recipe toRecipe() {
            if (pattern == null || pattern.isEmpty() || keys == null) {
                throw new IllegalStateException("Incomplete large HEX recipe");
            }
            LinkedHashMap<String, LargeHeatExchangerProfile.Ingredient> parsed =
                    new LinkedHashMap<>();
            keys.forEach((letter, row) -> parsed.put(letter, row.toIngredient()));
            List<String> tools = catalysts != null
                    ? catalysts
                    : CatalogJson.nonBlank(catalyst) ? List.of(catalyst) : List.of();
            return new LargeHeatExchangerProfile.Recipe(pattern, parsed, tools);
        }
    }

    private static final class IngredientRow {
        private String item;
        private String prefix;
        private String material;
        private String family;

        private LargeHeatExchangerProfile.Ingredient toIngredient() {
            if (CatalogJson.nonBlank(item)) {
                return new LargeHeatExchangerProfile.Ingredient(
                        item, prefix, material, family);
            }
            if (!CatalogJson.nonBlank(prefix) || !CatalogJson.nonBlank(material)) {
                throw new IllegalStateException(
                        "Large HEX recipe key needs item or prefix+material");
            }
            return new LargeHeatExchangerProfile.Ingredient(
                    null, prefix, material, family);
        }
    }

    private LargeHeatExchangerCatalog() {}
}
