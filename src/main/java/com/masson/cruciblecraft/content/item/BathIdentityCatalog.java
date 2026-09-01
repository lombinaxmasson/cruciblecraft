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
 * Bath identity catalog. Each (source item, exact meta) is a distinct
 * player-holdable item. Tool heads are not folded into material prefixes.
 */
public final class BathIdentityCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/bath_identity_catalog.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int VARIANT_COUNT = CATALOG.identities().size();

    private BathIdentityCatalog() {}

    public static List<Identity> identities() {
        return CATALOG.identities();
    }

    public static String sourceRevision() {
        return CATALOG.sourceRevision();
    }

    private static Catalog loadBundled() {
        try (var stream = BathIdentityCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled bath identity catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"BATH_IDENTITY_CATALOG".equals(document.status)
                    || document.identities == null
                    || document.identities.isEmpty()
                    || document.identities.size() != document.identityCount
                    || document.identities.size() != document.variantCount) {
                throw new IllegalStateException(
                        "Bath identity catalog schema, status, or count drifted");
            }
            List<Identity> all = new ArrayList<>(document.identities.size());
            Map<ResourceLocation, Identity> byId = new LinkedHashMap<>();
            for (IdentityRow row : document.identities) {
                ResourceLocation id = ResourceLocation.parse(row.runtimeId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())
                        || !row.registryPath.equals(id.getPath())) {
                    throw new IllegalStateException(
                            "Bath identity runtime id drifted: " + row.runtimeId);
                }
                if (!"item".equals(row.registryKind)) {
                    throw new IllegalStateException(
                            "Bath identity registry kind drifted: " + row.registryKind);
                }
                Identity identity = new Identity(
                        id,
                        row.registryPath,
                        row.sourceItem,
                        row.meta,
                        row.kind,
                        row.englishName,
                        row.chineseName);
                if (byId.put(id, identity) != null) {
                    throw new IllegalStateException(
                            "Duplicate bath identity runtime id " + id);
                }
                all.add(identity);
            }
            return new Catalog(
                    SOURCE_REVISION,
                    List.copyOf(all),
                    Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read bath identity catalog", failure);
        }
    }

    public record Identity(
            ResourceLocation id,
            String registryPath,
            String sourceItem,
            int meta,
            String kind,
            String englishName,
            String chineseName) {
        public Identity {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(registryPath, "registryPath");
            Objects.requireNonNull(sourceItem, "sourceItem");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(englishName, "englishName");
            Objects.requireNonNull(chineseName, "chineseName");
        }
    }

    private record Catalog(
            String sourceRevision,
            List<Identity> identities,
            Map<ResourceLocation, Identity> byId) {}

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        String status;
        @SerializedName("identity_count")
        int identityCount;
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
