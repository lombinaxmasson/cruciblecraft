package com.masson.cruciblecraft.energy.converter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** Kind-level runtime, fuel map, faces and display names. */
public final class EnergyConverterKindCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/energy_converter_kinds.json";
    private static final EnergyConverterKindCatalog BUNDLED = loadBundled();

    private final List<Kind> kinds;
    private final Map<ResourceLocation, Kind> byId;
    private final Map<String, MaterialLang> materialLang;

    public static EnergyConverterKindCatalog bundled() {
        return BUNDLED;
    }

    public static List<Kind> kinds() {
        return BUNDLED.kinds;
    }

    public static Kind require(ResourceLocation id) {
        return BUNDLED.requireKind(id);
    }

    public static MaterialLang materialLang(String materialPath) {
        return BUNDLED.requireMaterialLang(materialPath);
    }

    private EnergyConverterKindCatalog(
            List<Kind> kinds,
            Map<String, MaterialLang> materialLang) {
        this.kinds = List.copyOf(kinds);
        LinkedHashMap<ResourceLocation, Kind> indexed = new LinkedHashMap<>();
        for (Kind kind : this.kinds) {
            if (indexed.putIfAbsent(kind.id(), kind) != null) {
                throw new IllegalStateException(
                        "Duplicate energy converter kind " + kind.id());
            }
        }
        this.byId = Map.copyOf(indexed);
        this.materialLang = Map.copyOf(materialLang);
    }

    public Kind requireKind(ResourceLocation id) {
        Kind kind = byId.get(id);
        if (kind == null) {
            throw new IllegalArgumentException(
                    "Unknown energy converter kind " + id);
        }
        return kind;
    }

    public MaterialLang requireMaterialLang(String materialPath) {
        Objects.requireNonNull(materialPath, "materialPath");
        String path = materialPath;
        int colon = path.indexOf(':');
        if (colon >= 0) {
            path = path.substring(colon + 1);
        }
        MaterialLang lang = materialLang.get(path);
        if (lang == null) {
            throw new IllegalStateException(
                    "No converter material language for " + materialPath);
        }
        return lang;
    }

    public boolean containsMaterialLang(String materialPath) {
        String path = materialPath;
        int colon = path.indexOf(':');
        if (colon >= 0) {
            path = path.substring(colon + 1);
        }
        return materialLang.containsKey(path);
    }

    private static EnergyConverterKindCatalog loadBundled() {
        return fromDocument(CatalogJson.readBundled(
                EnergyConverterKindCatalog.class, RESOURCE, Document.class));
    }

    private static EnergyConverterKindCatalog fromDocument(Document document) {
        if (document.schemaVersion != 1
                || document.kinds == null
                || document.kinds.isEmpty()
                || document.materialLang == null) {
            throw new IllegalStateException(
                    "Invalid energy converter kind catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        List<Kind> kinds = document.kinds.stream()
                .map(KindRow::toKind)
                .toList();
        LinkedHashMap<String, MaterialLang> lang = new LinkedHashMap<>();
        document.materialLang.forEach((material, row) ->
                lang.put(material, row.toLang(material)));
        return new EnergyConverterKindCatalog(kinds, lang);
    }

    public record Kind(
            ResourceLocation id,
            String runtime,
            String fuelMap,
            List<String> accepts,
            List<String> emits,
            EnergyConverterProfile.Faces faces,
            String textureProfile,
            boolean overlayActive,
            String langZh,
            String langEn,
            String gt6Class) {
        public Kind {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(runtime, "runtime");
            Objects.requireNonNull(fuelMap, "fuelMap");
            accepts = List.copyOf(accepts);
            emits = List.copyOf(emits);
            Objects.requireNonNull(faces, "faces");
            Objects.requireNonNull(textureProfile, "textureProfile");
            Objects.requireNonNull(langZh, "langZh");
            Objects.requireNonNull(langEn, "langEn");
            Objects.requireNonNull(gt6Class, "gt6Class");
        }
    }

    public record MaterialLang(String materialPath, String zh, String en) {
        public MaterialLang {
            Objects.requireNonNull(materialPath, "materialPath");
            Objects.requireNonNull(zh, "zh");
            Objects.requireNonNull(en, "en");
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        private List<KindRow> kinds;
        @SerializedName("material_lang")
        private Map<String, MaterialLangRow> materialLang;
    }

    private static final class KindRow {
        private String id;
        private String runtime;
        @SerializedName("fuel_map")
        private String fuelMap;
        private List<String> accepts;
        private List<String> emits;
        private FacesRow faces;
        @SerializedName("texture_profile")
        private String textureProfile;
        @SerializedName("overlay_active")
        private boolean overlayActive;
        @SerializedName("lang_key_zh")
        private String langKeyZh;
        @SerializedName("lang_key_en")
        private String langKeyEn;
        @SerializedName("gt6_class")
        private String gt6Class;

        private Kind toKind() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(runtime)
                    || !CatalogJson.nonBlank(fuelMap)
                    || faces == null
                    || !CatalogJson.nonBlank(textureProfile)
                    || !CatalogJson.nonBlank(langKeyZh)
                    || !CatalogJson.nonBlank(langKeyEn)
                    || !CatalogJson.nonBlank(gt6Class)) {
                throw new IllegalStateException(
                        "Incomplete energy converter kind row " + id);
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) {
                throw new IllegalStateException(
                        "Invalid energy converter kind id " + id);
            }
            return new Kind(
                    parsed,
                    runtime,
                    fuelMap,
                    CatalogJson.list(accepts),
                    CatalogJson.list(emits),
                    faces.toFaces(),
                    textureProfile,
                    overlayActive,
                    langKeyZh,
                    langKeyEn,
                    gt6Class);
        }
    }

    private static final class FacesRow {
        private List<String> energyInputs;
        private List<String> energyOutputs;
        private List<String> fluidInputs;
        private List<String> fluidOutputs;

        private EnergyConverterProfile.Faces toFaces() {
            return new EnergyConverterProfile.Faces(
                    CatalogJson.list(energyInputs),
                    CatalogJson.list(energyOutputs),
                    CatalogJson.list(fluidInputs),
                    CatalogJson.list(fluidOutputs));
        }
    }

    private static final class MaterialLangRow {
        @SerializedName("lang_key_zh")
        private String langKeyZh;
        @SerializedName("lang_key_en")
        private String langKeyEn;

        private MaterialLang toLang(String material) {
            if (!CatalogJson.nonBlank(langKeyZh)
                    || !CatalogJson.nonBlank(langKeyEn)) {
                throw new IllegalStateException(
                        "Incomplete converter material language " + material);
            }
            return new MaterialLang(material, langKeyZh, langKeyEn);
        }
    }
}
