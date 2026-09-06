package com.masson.cruciblecraft.energy.transformer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/** Kind-level electric transformer identity. */
public final class EnergyTransformerKindCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/energy_transformer_kinds.json";
    public static final int EXPECTED_SIZE = 1;
    private static final EnergyTransformerKindCatalog BUNDLED = loadBundled();

    private final List<Kind> kinds;
    private final Map<ResourceLocation, Kind> byId;

    public static List<Kind> kinds() {
        return BUNDLED.kinds;
    }

    public static Kind require(ResourceLocation id) {
        Kind kind = BUNDLED.byId.get(id);
        if (kind == null) {
            throw new IllegalArgumentException(
                    "Unknown energy transformer kind " + id);
        }
        return kind;
    }

    private EnergyTransformerKindCatalog(List<Kind> kinds) {
        this.kinds = List.copyOf(kinds);
        LinkedHashMap<ResourceLocation, Kind> indexed = new LinkedHashMap<>();
        for (Kind kind : this.kinds) {
            if (indexed.putIfAbsent(kind.id(), kind) != null) {
                throw new IllegalStateException(
                        "Duplicate energy transformer kind " + kind.id());
            }
        }
        this.byId = Map.copyOf(indexed);
    }

    private static EnergyTransformerKindCatalog loadBundled() {
        Document document = CatalogJson.readBundled(
                EnergyTransformerKindCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.kinds == null
                || document.kinds.size() != EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Invalid energy transformer kind catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        List<Kind> kinds = document.kinds.stream()
                .map(KindRow::toKind)
                .toList();
        return new EnergyTransformerKindCatalog(kinds);
    }

    public record Kind(
            ResourceLocation id,
            String runtime,
            EnergyType energyType,
            String textureProfile,
            String langZh,
            String langEn,
            String gt6Class) {
        public Kind {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(runtime, "runtime");
            Objects.requireNonNull(energyType, "energyType");
            Objects.requireNonNull(textureProfile, "textureProfile");
            Objects.requireNonNull(langZh, "langZh");
            Objects.requireNonNull(langEn, "langEn");
            Objects.requireNonNull(gt6Class, "gt6Class");
            if (!"transformer".equals(runtime)
                    || energyType != EnergyType.ELECTRIC) {
                throw new IllegalArgumentException(
                        "Transformer kind must be EU transformer runtime");
            }
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        private List<KindRow> kinds;
    }

    private static final class KindRow {
        private String id;
        private String runtime;
        private String energy;
        @SerializedName("texture_profile")
        private String textureProfile;
        @SerializedName("lang_key_zh")
        private String langKeyZh;
        @SerializedName("lang_key_en")
        private String langKeyEn;
        @SerializedName("gt6_class")
        private String gt6Class;

        private Kind toKind() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(runtime)
                    || !CatalogJson.nonBlank(energy)
                    || !CatalogJson.nonBlank(textureProfile)
                    || !CatalogJson.nonBlank(langKeyZh)
                    || !CatalogJson.nonBlank(langKeyEn)
                    || !CatalogJson.nonBlank(gt6Class)) {
                throw new IllegalStateException(
                        "Incomplete energy transformer kind row " + id);
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) {
                throw new IllegalStateException(
                        "Invalid energy transformer kind id " + id);
            }
            return new Kind(
                    parsed,
                    runtime,
                    EnergyTransformerProfile.energyType(energy),
                    textureProfile,
                    langKeyZh,
                    langKeyEn,
                    gt6Class);
        }
    }
}
