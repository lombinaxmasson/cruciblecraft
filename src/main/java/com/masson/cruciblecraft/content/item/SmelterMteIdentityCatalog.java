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
 * Bundled Smelter recovery MTE identities. Each proven family maps one
 * {@code (gregtech:gt.multitileentity, exact meta)} to a holdable CC item.
 * Bath overlap reuses existing runtime ids and is not re-registered.
 */
public final class SmelterMteIdentityCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/smelter_mte_identity_catalog.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int SOURCE_META_COUNT = 1817;

    private SmelterMteIdentityCatalog() {}

    public static List<Identity> identities() {
        return CATALOG.identities();
    }

    public static List<Identity> newItems() {
        return CATALOG.newItems();
    }

    public static String sourceRevision() {
        return CATALOG.sourceRevision();
    }

    public static int newItemCount() {
        return CATALOG.newItems().size();
    }

    private static Catalog loadBundled() {
        try (var stream = SmelterMteIdentityCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled smelter/mte catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"SMELTER_MTE_IDENTITY_CATALOG".equals(document.status)
                    || document.identities == null
                    || document.identities.size() != SOURCE_META_COUNT
                    || document.sourceMetaCount != SOURCE_META_COUNT
                    || document.runtimeIdentityCount != SOURCE_META_COUNT) {
                throw new IllegalStateException(
                        "Smelter/mte catalog must remain "
                                + SOURCE_META_COUNT
                                + " identities");
            }
            List<Identity> all = new ArrayList<>(SOURCE_META_COUNT);
            List<Identity> created = new ArrayList<>();
            Map<ResourceLocation, Identity> byId = new LinkedHashMap<>();
            Map<Integer, Identity> byMeta = new LinkedHashMap<>();
            for (IdentityRow row : document.identities) {
                ResourceLocation id = ResourceLocation.parse(row.runtimeId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())
                        || !row.registryPath.equals(id.getPath())) {
                    throw new IllegalStateException(
                            "Smelter/mte runtime id drifted: " + row.runtimeId);
                }
                if (!"gregtech:gt.multitileentity".equals(row.sourceItem)) {
                    throw new IllegalStateException(
                            "Smelter/mte source item drifted: " + row.sourceItem);
                }
                Identity identity = new Identity(
                        id,
                        row.registryPath,
                        row.sourceItem,
                        row.meta,
                        row.registryKind,
                        row.englishName,
                        row.chineseName);
                if (byId.put(id, identity) != null && "item".equals(row.registryKind)) {
                    throw new IllegalStateException(
                            "Duplicate smelter/mte runtime id " + id);
                }
                if (byMeta.put(row.meta, identity) != null) {
                    throw new IllegalStateException(
                            "Duplicate smelter/mte meta " + row.meta);
                }
                all.add(identity);
                if ("item".equals(row.registryKind)) {
                    created.add(identity);
                }
            }
            if (document.newItemCount != created.size()) {
                throw new IllegalStateException(
                        "Smelter/mte new item count drifted: " + created.size());
            }
            return new Catalog(
                    SOURCE_REVISION,
                    List.copyOf(all),
                    List.copyOf(created),
                    Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read smelter/mte catalog", failure);
        }
    }

    public record Identity(
            ResourceLocation id,
            String registryPath,
            String sourceItem,
            int meta,
            String registryKind,
            String englishName,
            String chineseName) {
        public Identity {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(registryPath, "registryPath");
            Objects.requireNonNull(sourceItem, "sourceItem");
            Objects.requireNonNull(registryKind, "registryKind");
            Objects.requireNonNull(englishName, "englishName");
            Objects.requireNonNull(chineseName, "chineseName");
        }

        public boolean registerItem() {
            return "item".equals(registryKind);
        }
    }

    private record Catalog(
            String sourceRevision,
            List<Identity> identities,
            List<Identity> newItems,
            Map<ResourceLocation, Identity> byId) {}

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        String status;
        @SerializedName("source_meta_count")
        int sourceMetaCount;
        @SerializedName("runtime_identity_count")
        int runtimeIdentityCount;
        @SerializedName("new_item_count")
        int newItemCount;
        List<IdentityRow> identities;
    }

    private static final class IdentityRow {
        @SerializedName("chinese_name")
        String chineseName;
        @SerializedName("english_name")
        String englishName;
        int meta;
        @SerializedName("registry_kind")
        String registryKind;
        @SerializedName("registry_path")
        String registryPath;
        @SerializedName("runtime_id")
        String runtimeId;
        @SerializedName("source_item")
        String sourceItem;
    }
}
