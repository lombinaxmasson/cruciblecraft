package com.masson.cruciblecraft.content.storage;

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
 * Bundled T44 storage catalog. 624 storage rows plus 1 logistics row.
 * There is no second Java variant list.
 */
public final class StorageVariantCatalog {
    public static final int STORAGE_COUNT = 624;
    public static final int LOGISTICS_COUNT = 1;
    public static final int TOTAL_COUNT = STORAGE_COUNT + LOGISTICS_COUNT;
    public static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final String RESOURCE =
            "/data/cruciblecraft/storage_variants.json";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    private StorageVariantCatalog() {}

    public static List<StorageVariant> variants() {
        return CATALOG.variants();
    }

    public static List<StorageVariant> storageVariants() {
        return CATALOG.storage();
    }

    public static StorageVariant logistics() {
        return CATALOG.logistics();
    }

    public static StorageVariant require(ResourceLocation id) {
        StorageVariant variant = CATALOG.byId().get(id);
        if (variant == null) {
            throw new IllegalArgumentException("Unknown storage variant " + id);
        }
        return variant;
    }

    public static List<StorageVariant> of(StorageBehaviorProfile profile) {
        Objects.requireNonNull(profile, "profile");
        return CATALOG.byProfile().getOrDefault(profile, List.of());
    }

    public static List<StorageVariant> sourceVisible() {
        return CATALOG.visible();
    }

    private static Catalog loadBundled() {
        try (var stream = StorageVariantCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled storage catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || document.storageCount != STORAGE_COUNT
                    || document.logisticsCount != LOGISTICS_COUNT
                    || document.variants == null
                    || document.variants.size() != TOTAL_COUNT) {
                throw new IllegalStateException(
                        "Storage catalog must remain 624+1 SOURCE_BACKED rows");
            }
            List<StorageVariant> variants = new ArrayList<>(TOTAL_COUNT);
            List<StorageVariant> storage = new ArrayList<>(STORAGE_COUNT);
            Map<ResourceLocation, StorageVariant> byId = new LinkedHashMap<>();
            Map<StorageBehaviorProfile, List<StorageVariant>> byProfile =
                    new LinkedHashMap<>();
            StorageVariant logistics = null;
            for (Row row : document.variants) {
                StorageVariant variant = row.toVariant();
                if (byId.put(variant.id(), variant) != null) {
                    throw new IllegalStateException(
                            "Duplicate storage runtime id " + variant.id());
                }
                variants.add(variant);
                byProfile.computeIfAbsent(variant.behavior(), ignored -> new ArrayList<>())
                        .add(variant);
                if (variant.countsTowardStorage624()) {
                    storage.add(variant);
                } else {
                    if (logistics != null) {
                        throw new IllegalStateException(
                                "More than one logistics storage row");
                    }
                    logistics = variant;
                }
            }
            if (storage.size() != STORAGE_COUNT || logistics == null) {
                throw new IllegalStateException(
                        "Storage/logistics split drifted from 624+1");
            }
            Map<StorageBehaviorProfile, List<StorageVariant>> frozenProfile =
                    new LinkedHashMap<>();
            byProfile.forEach((profile, rows) ->
                    frozenProfile.put(profile, List.copyOf(rows)));
            List<StorageVariant> visible = variants.stream()
                    .filter(StorageVariant::sourceVisible)
                    .toList();
            return new Catalog(
                    List.copyOf(variants),
                    List.copyOf(storage),
                    logistics,
                    List.copyOf(visible),
                    Map.copyOf(byId),
                    Map.copyOf(frozenProfile));
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to read storage catalog", failure);
        }
    }

    private record Catalog(
            List<StorageVariant> variants,
            List<StorageVariant> storage,
            StorageVariant logistics,
            List<StorageVariant> visible,
            Map<ResourceLocation, StorageVariant> byId,
            Map<StorageBehaviorProfile, List<StorageVariant>> byProfile) {}

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        @SerializedName("storage_count")
        int storageCount;
        @SerializedName("logistics_count")
        int logisticsCount;
        List<Row> variants;
    }

    private static final class Row {
        @SerializedName("runtime_id")
        String runtimeId;
        String family;
        @SerializedName("behavior_profile")
        String behaviorProfile;
        int slots;
        int capacity;
        String visibility;
        boolean charging;
        boolean logistics;
        @SerializedName("counts_toward_storage_624")
        boolean countsTowardStorage624;
        String english;
        String chinese;
        @SerializedName("plank_index")
        Integer plankIndex;
        @SerializedName("acquisition_profile")
        String acquisitionProfile;

        StorageVariant toVariant() {
            return new StorageVariant(
                    ResourceLocation.parse(runtimeId),
                    family,
                    StorageBehaviorProfile.parse(behaviorProfile),
                    slots,
                    capacity,
                    "source_visible".equals(visibility),
                    charging,
                    logistics,
                    countsTowardStorage624,
                    english,
                    chinese,
                    plankIndex,
                    acquisitionProfile);
        }
    }
}
