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
 * Bundled GT stone identities. Each (source item, meta) is a distinct
 * Block+BlockItem; slabs are not folded onto the full stone.
 */
public final class GtStoneCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/gt_stone_catalog.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int IDENTITY_COUNT = 119;
    public static final int VARIANT_COUNT = 406;

    private GtStoneCatalog() {}

    public static List<Variant> variants() {
        return CATALOG.variants();
    }

    public static Variant require(ResourceLocation id) {
        Variant variant = CATALOG.byId().get(id);
        if (variant == null) {
            throw new IllegalArgumentException("Unknown GT stone " + id);
        }
        return variant;
    }

    public static String sourceRevision() {
        return CATALOG.sourceRevision();
    }

    private static Catalog loadBundled() {
        try (var stream = GtStoneCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled GT stone catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"T43_STONE_CATALOG".equals(document.status)
                    || document.identities == null
                    || document.identities.size() != IDENTITY_COUNT
                    || document.variantCount != VARIANT_COUNT) {
                throw new IllegalStateException(
                        "GT stone catalog must remain 119 identities / 406 variants");
            }
            List<Variant> variants = new ArrayList<>(VARIANT_COUNT);
            Map<ResourceLocation, Variant> byId = new LinkedHashMap<>();
            for (IdentityRow identity : document.identities) {
                if (identity.variants == null || identity.variants.isEmpty()) {
                    throw new IllegalStateException(
                            "GT stone identity missing variants: "
                                    + identity.sourceItem);
                }
                boolean slab = "slab".equals(identity.kind);
                if (slab == (identity.slabVariant == null)) {
                    throw new IllegalStateException(
                            "GT stone slab_variant drifted for "
                                    + identity.sourceItem);
                }
                for (VariantRow row : identity.variants) {
                    ResourceLocation id = ResourceLocation.parse(row.runtimeId);
                    if (!CrucibleCraft.MODID.equals(id.getNamespace())
                            || !row.registryPath.equals(id.getPath())) {
                        throw new IllegalStateException(
                                "GT stone runtime id drifted: " + row.runtimeId);
                    }
                    Variant variant = new Variant(
                            id,
                            row.registryPath,
                            identity.sourceItem,
                            row.meta,
                            slab,
                            identity.slabVariant,
                            identity.stone,
                            identity.texture,
                            englishName(identity, row.meta),
                            chineseName(identity, row.meta));
                    if (byId.put(id, variant) != null) {
                        throw new IllegalStateException(
                                "Duplicate GT stone runtime id " + id);
                    }
                    variants.add(variant);
                }
            }
            if (variants.size() != VARIANT_COUNT || byId.size() != VARIANT_COUNT) {
                throw new IllegalStateException(
                        "GT stone catalog variant count drifted: "
                                + variants.size());
            }
            return new Catalog(
                    SOURCE_REVISION,
                    List.copyOf(variants),
                    Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read GT stone catalog", failure);
        }
    }

    private static String englishName(IdentityRow identity, int meta) {
        if (identity.slabVariant == null) {
            return identity.englishName + " m" + meta;
        }
        return identity.englishName
                + " Slab "
                + identity.slabVariant
                + " m"
                + meta;
    }

    private static String chineseName(IdentityRow identity, int meta) {
        if (identity.slabVariant == null) {
            return identity.chineseName + " m" + meta;
        }
        return identity.chineseName
                + "台阶"
                + identity.slabVariant
                + " m"
                + meta;
    }

    public record Variant(
            ResourceLocation id,
            String registryPath,
            String sourceItem,
            int meta,
            boolean slab,
            Integer slabVariant,
            String stone,
            String texture,
            String englishName,
            String chineseName) {
        public Variant {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(registryPath, "registryPath");
            Objects.requireNonNull(sourceItem, "sourceItem");
            Objects.requireNonNull(stone, "stone");
            Objects.requireNonNull(texture, "texture");
            Objects.requireNonNull(englishName, "englishName");
            Objects.requireNonNull(chineseName, "chineseName");
        }

        public ResourceLocation textureLocation() {
            return ResourceLocation.parse(texture);
        }
    }

    private record Catalog(
            String sourceRevision,
            List<Variant> variants,
            Map<ResourceLocation, Variant> byId) {}

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
        @SerializedName("chinese_name")
        String chineseName;
        @SerializedName("english_name")
        String englishName;
        String kind;
        @SerializedName("slab_variant")
        Integer slabVariant;
        @SerializedName("source_item")
        String sourceItem;
        String stone;
        String texture;
        List<VariantRow> variants;
    }

    private static final class VariantRow {
        int meta;
        @SerializedName("registry_path")
        String registryPath;
        @SerializedName("runtime_id")
        String runtimeId;
    }
}
