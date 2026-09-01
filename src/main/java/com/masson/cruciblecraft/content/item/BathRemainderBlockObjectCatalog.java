package com.masson.cruciblecraft.content.item;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * Bath remainder GT block-object identities. Does not rewrite the frozen
 * 365-variant catalog; new (source item, meta) pairs are registered here.
 */
public final class BathRemainderBlockObjectCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/bath_remainder_identity_catalog.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int VARIANT_COUNT = 283;

    private BathRemainderBlockObjectCatalog() {}

    public static List<GtBlockObjectCatalog.Variant> variants() {
        return CATALOG.variants();
    }

    public static GtBlockObjectCatalog.Variant require(ResourceLocation id) {
        GtBlockObjectCatalog.Variant variant = CATALOG.byId().get(id);
        if (variant == null) {
            throw new IllegalArgumentException("Unknown bath remainder block object " + id);
        }
        return variant;
    }

    public static GtBlockObjectCatalog.Variant find(ResourceLocation id) {
        return CATALOG.byId().get(id);
    }

    private static Catalog loadBundled() {
        try (var stream = BathRemainderBlockObjectCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled bath remainder identity catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"BATH_REMAINDER_IDENTITY_CATALOG".equals(document.status)
                    || document.identities == null
                    || document.identities.size() != VARIANT_COUNT
                    || document.variantCount != VARIANT_COUNT) {
                throw new IllegalStateException(
                        "Bath remainder block-object catalog must remain "
                                + VARIANT_COUNT
                                + " identities");
            }
            List<GtBlockObjectCatalog.Variant> variants = new ArrayList<>(VARIANT_COUNT);
            Map<ResourceLocation, GtBlockObjectCatalog.Variant> byId = new LinkedHashMap<>();
            for (IdentityRow identity : document.identities) {
                ResourceLocation id = ResourceLocation.parse(identity.runtimeId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())
                        || !identity.registryPath.equals(id.getPath())) {
                    throw new IllegalStateException(
                            "Bath remainder block-object runtime id drifted: "
                                    + identity.runtimeId);
                }
                String behavior = identity.behavior == null ? identity.kind : identity.behavior;
                if ("block".equals(behavior)) {
                    behavior = "solid";
                }
                GtBlockObjectCatalog.Variant variant = new GtBlockObjectCatalog.Variant(
                        id,
                        identity.registryPath,
                        identity.sourceItem,
                        identity.meta,
                        behavior,
                        identity.texture,
                        identity.englishName,
                        identity.chineseName);
                if (byId.put(id, variant) != null) {
                    throw new IllegalStateException(
                            "Duplicate bath remainder block-object runtime id " + id);
                }
                variants.add(variant);
            }
            if (variants.size() != VARIANT_COUNT || byId.size() != VARIANT_COUNT) {
                throw new IllegalStateException(
                        "Bath remainder block-object catalog variant count drifted: "
                                + variants.size());
            }
            return new Catalog(List.copyOf(variants), Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read bath remainder identity catalog", failure);
        }
    }

    private record Catalog(
            List<GtBlockObjectCatalog.Variant> variants,
            Map<ResourceLocation, GtBlockObjectCatalog.Variant> byId) {
        private Catalog {
            Objects.requireNonNull(variants, "variants");
            Objects.requireNonNull(byId, "byId");
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        String status;
        @SerializedName("variant_count")
        int variantCount;
        List<IdentityRow> identities;
    }

    private static final class IdentityRow {
        String behavior;
        @SerializedName("chinese_name")
        String chineseName;
        @SerializedName("english_name")
        String englishName;
        String kind;
        int meta;
        @SerializedName("registry_path")
        String registryPath;
        @SerializedName("runtime_id")
        String runtimeId;
        @SerializedName("source_item")
        String sourceItem;
        String texture;
    }
}
