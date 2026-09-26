package com.masson.cruciblecraft.content.item;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/** GT6 technological parts for the T1–T6 circuit path and fission survival. */
public final class TechnologicalPartCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/technological_parts.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final int PART_COUNT = 164;
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    private TechnologicalPartCatalog() {}

    public static List<Part> parts() {
        return CATALOG.parts();
    }

    public static String sourceRevision() {
        return CATALOG.sourceRevision();
    }

    public static Optional<Part> findByPath(String registryPath) {
        if (registryPath == null || registryPath.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CATALOG.byPath().get(registryPath));
    }

    private static Catalog loadBundled() {
        try (var stream = TechnologicalPartCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled technological parts " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"TECHNOLOGICAL_PARTS".equals(document.status)
                    || document.parts == null
                    || document.parts.size() != PART_COUNT) {
                throw new IllegalStateException(
                        "Technological parts catalog must remain " + PART_COUNT + " items");
            }
            List<Part> parts = new ArrayList<>(PART_COUNT);
            Map<ResourceLocation, Part> byId = new LinkedHashMap<>();
            Map<String, Part> byPath = new LinkedHashMap<>();
            for (PartRow row : document.parts) {
                ResourceLocation id = ResourceLocation.parse(row.id);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())
                        || !row.registryPath.equals(id.getPath())) {
                    throw new IllegalStateException("Technological part id drifted: " + row.id);
                }
                Part part = new Part(
                        id,
                        row.registryPath,
                        row.sourceId,
                        row.englishName,
                        row.chineseName,
                        row.texture);
                if (byId.put(id, part) != null) {
                    throw new IllegalStateException("Duplicate technological part " + id);
                }
                if (byPath.put(row.registryPath, part) != null) {
                    throw new IllegalStateException(
                            "Duplicate technological part path " + row.registryPath);
                }
                parts.add(part);
            }
            return new Catalog(
                    SOURCE_REVISION,
                    List.copyOf(parts),
                    Map.copyOf(byId),
                    Map.copyOf(byPath));
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to read technological parts", failure);
        }
    }

    public record Part(
            ResourceLocation id,
            String registryPath,
            int sourceId,
            String englishName,
            String chineseName,
            String texture) {
        public Part {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(registryPath, "registryPath");
            Objects.requireNonNull(englishName, "englishName");
            Objects.requireNonNull(chineseName, "chineseName");
            Objects.requireNonNull(texture, "texture");
        }
    }

    private record Catalog(
            String sourceRevision,
            List<Part> parts,
            Map<ResourceLocation, Part> byId,
            Map<String, Part> byPath) {}

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        String status;
        List<PartRow> parts;
    }

    private static final class PartRow {
        @SerializedName("chinese_name")
        String chineseName;
        @SerializedName("english_name")
        String englishName;
        String id;
        @SerializedName("registry_path")
        String registryPath;
        @SerializedName("source_id")
        int sourceId;
        String texture;
    }
}
