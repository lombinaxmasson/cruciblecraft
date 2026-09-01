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

/** Explicit native fluids for semantic ordinary-closure waves. */
public final class SemanticFluidCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/semantic_fluid_mapping.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int FLUID_COUNT = CATALOG.fluids().size();

    private SemanticFluidCatalog() {}

    public static List<BathMteFluidCatalog.FluidSpec> fluids() {
        return CATALOG.fluids();
    }

    private static Catalog loadBundled() {
        try (var stream = SemanticFluidCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled semantic fluid mapping " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"SEMANTIC_FLUID_MAPPING".equals(document.status)
                    || document.mapping == null
                    || document.mapping.size() != document.mappedCount()) {
                throw new IllegalStateException(
                        "Semantic fluid mapping schema, status, or count drifted");
            }
            List<BathMteFluidCatalog.FluidSpec> fluids =
                    new ArrayList<>(document.mapping.size());
            Map<ResourceLocation, BathMteFluidCatalog.FluidSpec> byId =
                    new LinkedHashMap<>();
            for (MappingRow row : document.mapping) {
                ResourceLocation id = ResourceLocation.parse(row.ccFluidId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())) {
                    throw new IllegalStateException(
                            "Semantic fluid runtime id drifted: " + row.ccFluidId);
                }
                BathMteFluidCatalog.FluidSpec spec = new BathMteFluidCatalog.FluidSpec(
                        id,
                        row.sourceFluid,
                        row.englishName,
                        colorFor(row.ccFluidId));
                if (byId.put(id, spec) != null) {
                    throw new IllegalStateException("Duplicate semantic fluid " + id);
                }
                fluids.add(spec);
            }
            return new Catalog(List.copyOf(fluids));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read semantic fluid mapping", failure);
        }
    }

    private static int colorFor(String runtimeId) {
        int hash = Objects.requireNonNull(runtimeId).hashCode();
        return 0xFF000000 | (hash & 0x00FFFFFF);
    }

    private record Catalog(List<BathMteFluidCatalog.FluidSpec> fluids) {}

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        String status;
        List<MappingRow> mapping;
        Counts counts;

        int mappedCount() {
            if (counts != null) {
                return counts.mapped;
            }
            return mapping == null ? 0 : mapping.size();
        }
    }

    private static final class Counts {
        int mapped;
    }

    private static final class MappingRow {
        @SerializedName("cc_fluid_id")
        String ccFluidId;
        @SerializedName("english_name")
        String englishName;
        @SerializedName("source_fluid")
        String sourceFluid;
    }
}
