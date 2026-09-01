package com.masson.cruciblecraft.machine.processing;

import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** Kind-level acquisition template and display names. */
public final class MachineKindCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/machine_kinds.json";
    private static final MachineKindCatalog BUNDLED = loadBundled();

    private final List<Kind> kinds;
    private final Map<ResourceLocation, Kind> byId;
    private final Map<String, MaterialLang> materialLang;

    public static MachineKindCatalog bundled() {
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

    public static MachineKindCatalog load(InputStream stream) {
        return fromDocument(CatalogJson.read(
                stream, Document.class, RESOURCE));
    }

    public static MachineKindCatalog load(Reader reader) {
        return fromDocument(CatalogJson.read(
                reader, Document.class, RESOURCE));
    }

    private MachineKindCatalog(
            List<Kind> kinds,
            Map<String, MaterialLang> materialLang) {
        this.kinds = List.copyOf(kinds);
        LinkedHashMap<ResourceLocation, Kind> indexed = new LinkedHashMap<>();
        for (Kind kind : this.kinds) {
            if (indexed.putIfAbsent(kind.id(), kind) != null) {
                throw new IllegalStateException(
                        "Duplicate machine kind " + kind.id());
            }
        }
        this.byId = Map.copyOf(indexed);
        this.materialLang = Map.copyOf(materialLang);
    }

    public Kind requireKind(ResourceLocation id) {
        Kind kind = byId.get(id);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown machine kind " + id);
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
                    "No machine material language for " + materialPath);
        }
        return lang;
    }

    public List<Kind> entries() {
        return kinds;
    }

    public boolean containsMaterialLang(String materialPath) {
        String path = materialPath;
        int colon = path.indexOf(':');
        if (colon >= 0) {
            path = path.substring(colon + 1);
        }
        return materialLang.containsKey(path);
    }

    private static MachineKindCatalog loadBundled() {
        return fromDocument(CatalogJson.readBundled(
                MachineKindCatalog.class, RESOURCE, Document.class));
    }

    private static MachineKindCatalog fromDocument(Document document) {
        if (document.schemaVersion != 1
                || document.kinds == null
                || document.kinds.isEmpty()
                || document.materialLang == null) {
            throw new IllegalStateException("Invalid machine kind catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        List<Kind> kinds = document.kinds.stream()
                .map(KindRow::toKind)
                .toList();
        LinkedHashMap<String, MaterialLang> lang = new LinkedHashMap<>();
        document.materialLang.forEach((material, row) ->
                lang.put(material, row.toLang(material)));
        return new MachineKindCatalog(kinds, lang);
    }

    public record Kind(
            ResourceLocation id,
            String acquisitionTemplate,
            String langZh,
            String langEn,
            String displayGroup) {
        public Kind {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(acquisitionTemplate, "acquisitionTemplate");
            Objects.requireNonNull(langZh, "langZh");
            Objects.requireNonNull(langEn, "langEn");
        }

        public boolean usesMaterialPrefix() {
            return "kinetic".equals(displayGroup) || "heat".equals(displayGroup);
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
        @SerializedName("acquisition_template")
        private String acquisitionTemplate;
        @SerializedName("lang_key_zh")
        private String langKeyZh;
        @SerializedName("lang_key_en")
        private String langKeyEn;
        @SerializedName("display_group")
        private String displayGroup;

        private Kind toKind() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(acquisitionTemplate)
                    || !CatalogJson.nonBlank(langKeyZh)
                    || !CatalogJson.nonBlank(langKeyEn)) {
                throw new IllegalStateException("Incomplete machine kind row");
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) {
                throw new IllegalStateException("Invalid machine kind id " + id);
            }
            return new Kind(
                    parsed,
                    acquisitionTemplate,
                    langKeyZh,
                    langKeyEn,
                    displayGroup);
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
                        "Incomplete machine material language " + material);
            }
            return new MaterialLang(material, langKeyZh, langKeyEn);
        }
    }

}
