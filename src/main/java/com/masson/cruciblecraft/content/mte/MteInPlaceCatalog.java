package com.masson.cruciblecraft.content.mte;

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
 * Bundled in-place MTE identities. Rows accumulate across sequential family
 * children; missing families simply have no live BlockItems yet.
 */
public final class MteInPlaceCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/mte_inplace_catalog.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    private MteInPlaceCatalog() {}

    public static List<MteInPlaceSpec> specs() {
        return CATALOG.specs();
    }

    public static MteInPlaceSpec require(ResourceLocation id) {
        MteInPlaceSpec spec = CATALOG.byId().get(id);
        if (spec == null) {
            throw new IllegalArgumentException("Unknown in-place MTE " + id);
        }
        return spec;
    }

    private static Catalog loadBundled() {
        try (var stream = MteInPlaceCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return new Catalog(List.of(), Map.of());
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"MTE_INPLACE_CATALOG".equals(document.status)
                    || document.identities == null) {
                throw new IllegalStateException(
                        "In-place MTE catalog drifted from GT6 revision "
                                + SOURCE_REVISION);
            }
            List<MteInPlaceSpec> specs = new ArrayList<>();
            Map<ResourceLocation, MteInPlaceSpec> byId = new LinkedHashMap<>();
            for (Row row : document.identities) {
                ResourceLocation id = ResourceLocation.parse(row.runtimeId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())
                        || !row.registryPath.equals(id.getPath())) {
                    throw new IllegalStateException(
                            "In-place MTE runtime id drifted: " + row.runtimeId);
                }
                MteInPlaceKind kind = MteInPlaceKind.valueOf(row.kind);
                MteInPlaceSpec spec = new MteInPlaceSpec(
                        id,
                        row.registryPath,
                        row.meta,
                        kind,
                        Objects.requireNonNull(row.family, "family"),
                        Objects.requireNonNull(row.englishName, "english"),
                        Objects.requireNonNull(row.chineseName, "chinese"),
                        Objects.requireNonNull(row.gt6Class, "gt6_class"));
                if (byId.put(id, spec) != null) {
                    throw new IllegalStateException(
                            "Duplicate in-place MTE " + id);
                }
                specs.add(spec);
            }
            return new Catalog(List.copyOf(specs), Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read in-place MTE catalog", failure);
        }
    }

    private record Catalog(
            List<MteInPlaceSpec> specs,
            Map<ResourceLocation, MteInPlaceSpec> byId) {}

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        String status;
        List<Row> identities;
    }

    private static final class Row {
        @SerializedName("chinese_name")
        String chineseName;
        @SerializedName("english_name")
        String englishName;
        @SerializedName("family")
        String family;
        @SerializedName("gt6_class")
        String gt6Class;
        String kind;
        int meta;
        @SerializedName("registry_path")
        String registryPath;
        @SerializedName("runtime_id")
        String runtimeId;
    }
}
