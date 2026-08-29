package com.masson.cruciblecraft.machine.processing;

import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** Opening 33 identity plus sparse acquisition/casing overrides. */
public final class MachineAcquisitionCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/machine_acquisition.json";
    private static final MachineAcquisitionCatalog BUNDLED = loadBundled();

    private final Set<ResourceLocation> openingIds;
    private final Map<ResourceLocation, Override> overrides;

    public static MachineAcquisitionCatalog bundled() {
        return BUNDLED;
    }

    public static boolean isOpening(ResourceLocation id) {
        return BUNDLED.openingIds.contains(id);
    }

    public static Set<ResourceLocation> openingIds() {
        return BUNDLED.openingIds;
    }

    public static Override override(ResourceLocation variantId) {
        return BUNDLED.overrideOf(variantId);
    }

    public static MachineAcquisitionCatalog load(InputStream stream) {
        return fromDocument(CatalogJson.read(
                stream, Document.class, RESOURCE));
    }

    public static MachineAcquisitionCatalog load(Reader reader) {
        return fromDocument(CatalogJson.read(
                reader, Document.class, RESOURCE));
    }

    private MachineAcquisitionCatalog(
            Set<ResourceLocation> openingIds,
            Map<ResourceLocation, Override> overrides) {
        this.openingIds = Set.copyOf(openingIds);
        this.overrides = Map.copyOf(overrides);
        if (this.openingIds.size() != 33) {
            throw new IllegalStateException(
                    "Opening variant set must remain 33 ids, got "
                            + this.openingIds.size());
        }
    }

    public Override overrideOf(ResourceLocation variantId) {
        Override override = overrides.get(variantId);
        return override == null ? Override.NONE : override;
    }

    public Set<ResourceLocation> opening() {
        return openingIds;
    }

    private static MachineAcquisitionCatalog loadBundled() {
        return fromDocument(CatalogJson.readBundled(
                MachineAcquisitionCatalog.class, RESOURCE, Document.class));
    }

    private static MachineAcquisitionCatalog fromDocument(Document document) {
        if (document.schemaVersion != 1
                || document.openingVariantIds == null) {
            throw new IllegalStateException(
                    "Invalid machine acquisition catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        LinkedHashSet<ResourceLocation> opening = new LinkedHashSet<>();
        for (String id : document.openingVariantIds) {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null || !opening.add(parsed)) {
                throw new IllegalStateException(
                        "Invalid or duplicate opening variant " + id);
            }
        }
        LinkedHashMap<ResourceLocation, Override> overrides = new LinkedHashMap<>();
        if (document.overrides != null) {
            for (OverrideRow row : document.overrides) {
                Override parsed = row.toOverride();
                overrides.merge(
                        parsed.variantId(),
                        parsed,
                        Override::merge);
            }
        }
        return new MachineAcquisitionCatalog(opening, overrides);
    }

    public record Override(
            ResourceLocation variantId,
            String acquisitionTemplate,
            ResourceLocation casingItem) {
        static final Override NONE = new Override(null, null, null);

        public Override {
            if (variantId != null) {
                Objects.requireNonNull(variantId, "variantId");
            }
        }

        private Override merge(Override other) {
            return new Override(
                    variantId,
                    first(acquisitionTemplate, other.acquisitionTemplate),
                    casingItem != null ? casingItem : other.casingItem);
        }

        private static String first(String left, String right) {
            return CatalogJson.nonBlank(left) ? left : right;
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        @SerializedName("opening_variant_ids")
        private List<String> openingVariantIds;
        private List<OverrideRow> overrides;
    }

    private static final class OverrideRow {
        private String id;
        @SerializedName("acquisition_template")
        private String acquisitionTemplate;
        @SerializedName("casing_item")
        private String casingItem;

        private Override toOverride() {
            ResourceLocation variantId = ResourceLocation.tryParse(id);
            if (variantId == null) {
                throw new IllegalStateException(
                        "Invalid acquisition override id " + id);
            }
            ResourceLocation casing = CatalogJson.nonBlank(casingItem)
                    ? ResourceLocation.tryParse(casingItem)
                    : null;
            if (CatalogJson.nonBlank(casingItem) && casing == null) {
                throw new IllegalStateException(
                        "Invalid casing override " + casingItem);
            }
            return new Override(variantId, acquisitionTemplate, casing);
        }
    }
}
