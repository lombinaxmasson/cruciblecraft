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
 * Bundled Bath MTE identities. Each (source item, exact meta) is a distinct
 * player-holdable item. Existing CC pipes/wires/cables are mapped, not
 * re-registered.
 */
public final class BathMteIdentityCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/bath_mte_identity_catalog.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int SOURCE_META_COUNT = 118;
    public static final int NEW_ITEM_COUNT = 76;

    private BathMteIdentityCatalog() {}

    public static List<Identity> identities() {
        return CATALOG.identities();
    }

    public static List<Identity> newItems() {
        return CATALOG.newItems();
    }

    public static String sourceRevision() {
        return CATALOG.sourceRevision();
    }

    private static Catalog loadBundled() {
        try (var stream = BathMteIdentityCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled bath/mte catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"BATH_MTE_IDENTITY_CATALOG".equals(document.status)
                    || document.identities == null
                    || document.identities.size() != SOURCE_META_COUNT
                    || document.sourceMetaCount != SOURCE_META_COUNT
                    || document.runtimeIdentityCount != SOURCE_META_COUNT) {
                throw new IllegalStateException(
                        "Bath/mte catalog must remain "
                                + SOURCE_META_COUNT
                                + " identities");
            }
            List<Identity> all = new ArrayList<>(SOURCE_META_COUNT);
            List<Identity> created = new ArrayList<>(NEW_ITEM_COUNT);
            Map<ResourceLocation, Identity> byId = new LinkedHashMap<>();
            for (IdentityRow row : document.identities) {
                ResourceLocation id = ResourceLocation.parse(row.runtimeId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())
                        || !row.registryPath.equals(id.getPath())) {
                    throw new IllegalStateException(
                            "Bath/mte runtime id drifted: " + row.runtimeId);
                }
                if (!"gregtech:gt.multitileentity".equals(row.sourceItem)) {
                    throw new IllegalStateException(
                            "Bath/mte source item drifted: " + row.sourceItem);
                }
                Identity identity = new Identity(
                        id,
                        row.registryPath,
                        row.sourceItem,
                        row.meta,
                        row.registryKind,
                        row.englishName,
                        row.chineseName);
                if (byId.put(id, identity) != null) {
                    throw new IllegalStateException(
                            "Duplicate bath/mte runtime id " + id);
                }
                all.add(identity);
                if ("item".equals(row.registryKind)) {
                    created.add(identity);
                }
            }
            if (created.size() != NEW_ITEM_COUNT) {
                throw new IllegalStateException(
                        "Bath/mte new item count drifted: " + created.size());
            }
            return new Catalog(
                    SOURCE_REVISION,
                    List.copyOf(all),
                    List.copyOf(created),
                    Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read bath/mte catalog", failure);
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

        public boolean decorativePanel() {
            return registryPath.startsWith("panel/asphalt_")
                    || registryPath.startsWith("panel/cfoam_")
                    || registryPath.startsWith("panel/concrete_");
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
