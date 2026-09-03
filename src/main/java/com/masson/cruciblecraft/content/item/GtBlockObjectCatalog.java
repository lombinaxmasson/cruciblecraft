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
 * Bundled GT block-object identities. Each (source item, meta) is a distinct
 * Block+BlockItem with source-backed behavior.
 */
public final class GtBlockObjectCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/gt_block_object_catalog.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int VARIANT_COUNT = 365;

    private GtBlockObjectCatalog() {}

    public static List<Variant> variants() {
        return CATALOG.variants();
    }

    public static Variant require(ResourceLocation id) {
        Variant variant = CATALOG.byId().get(id);
        if (variant == null) {
            throw new IllegalArgumentException("Unknown GT block object " + id);
        }
        return variant;
    }

    public static String sourceRevision() {
        return CATALOG.sourceRevision();
    }

    private static Catalog loadBundled() {
        try (var stream = GtBlockObjectCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled GT block-object catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"BLOCK_OBJECT_CATALOG".equals(document.status)
                    || document.identities == null
                    || document.identities.size() != VARIANT_COUNT
                    || document.variantCount != VARIANT_COUNT) {
                throw new IllegalStateException(
                        "GT block-object catalog must remain "
                                + VARIANT_COUNT
                                + " identities");
            }
            List<Variant> variants = new ArrayList<>(VARIANT_COUNT);
            Map<ResourceLocation, Variant> byId = new LinkedHashMap<>();
            for (IdentityRow identity : document.identities) {
                ResourceLocation id = ResourceLocation.parse(identity.runtimeId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())
                        || !identity.registryPath.equals(id.getPath())) {
                    throw new IllegalStateException(
                            "GT block-object runtime id drifted: "
                                    + identity.runtimeId);
                }
                Variant variant = new Variant(
                        id,
                        identity.registryPath,
                        identity.sourceItem,
                        identity.meta,
                        identity.behavior == null ? identity.kind : identity.behavior,
                        identity.texture,
                        identity.englishName,
                        identity.chineseName);
                if (byId.put(id, variant) != null) {
                    throw new IllegalStateException(
                            "Duplicate GT block-object runtime id " + id);
                }
                variants.add(variant);
            }
            if (variants.size() != VARIANT_COUNT || byId.size() != VARIANT_COUNT) {
                throw new IllegalStateException(
                        "GT block-object catalog variant count drifted: "
                                + variants.size());
            }
            return new Catalog(
                    SOURCE_REVISION,
                    List.copyOf(variants),
                    Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read GT block-object catalog", failure);
        }
    }

    public record Variant(
            ResourceLocation id,
            String registryPath,
            String sourceItem,
            int meta,
            String behavior,
            String texture,
            String englishName,
            String chineseName) {
        public Variant {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(registryPath, "registryPath");
            Objects.requireNonNull(sourceItem, "sourceItem");
            Objects.requireNonNull(behavior, "behavior");
            Objects.requireNonNull(texture, "texture");
            Objects.requireNonNull(englishName, "englishName");
            Objects.requireNonNull(chineseName, "chineseName");
        }

        public ResourceLocation textureLocation() {
            ResourceLocation sourced = BlockArtIndex.texture(registryPath);
            return sourced != null ? sourced : ResourceLocation.parse(texture);
        }

        public ResourceLocation sideTextureLocation() {
            ResourceLocation side = BlockArtIndex.sideTexture(registryPath);
            return side != null ? side : textureLocation();
        }

        public boolean dyeTint() {
            return BlockArtIndex.dyeTint(registryPath);
        }

        public boolean slab() {
            return "slab".equals(behavior);
        }

        public boolean log() {
            return "log".equals(behavior);
        }

        public boolean fireproof() {
            return sourceItem.contains("fireproof");
        }

        public boolean bars() {
            return "bars".equals(behavior);
        }

        public boolean rail() {
            return "rail".equals(behavior);
        }

        public boolean spike() {
            return "spike".equals(behavior);
        }

        public boolean bale() {
            return "bale".equals(behavior);
        }

        public boolean cfoamFresh() {
            return "cfoam_fresh".equals(behavior);
        }

        public boolean cfoam() {
            return "cfoam".equals(behavior);
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
