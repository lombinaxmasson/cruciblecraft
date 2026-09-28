package com.masson.cruciblecraft.worldgen.tree;

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
import com.masson.cruciblecraft.content.item.BathMteFluidCatalog;

import net.minecraft.resources.ResourceLocation;

/** Explicit tree-hole overlay fluids with no legal bath remainder equivalent. */
public final class TreeHoleFluidCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/tree_hole_fluid_mapping.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int FLUID_COUNT = 2;

    private TreeHoleFluidCatalog() {}

    public static List<BathMteFluidCatalog.FluidSpec> fluids() {
        return CATALOG.fluids();
    }

    private static Catalog loadBundled() {
        try (var stream = TreeHoleFluidCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled tree-hole fluid mapping " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"TREE_HOLE_FLUID_MAPPING".equals(document.status)
                    || document.mapping == null
                    || document.mapping.size() != FLUID_COUNT) {
                throw new IllegalStateException(
                        "Tree-hole fluid overlay must remain " + FLUID_COUNT + " fluids");
            }
            List<BathMteFluidCatalog.FluidSpec> fluids = new ArrayList<>(FLUID_COUNT);
            Map<ResourceLocation, BathMteFluidCatalog.FluidSpec> byId = new LinkedHashMap<>();
            for (MappingRow row : document.mapping) {
                ResourceLocation id = ResourceLocation.parse(row.ccFluidId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())) {
                    throw new IllegalStateException(
                            "Tree-hole fluid runtime id drifted: " + row.ccFluidId);
                }
                BathMteFluidCatalog.FluidSpec spec = new BathMteFluidCatalog.FluidSpec(
                        id,
                        row.sourceFluid,
                        row.englishName,
                        row.colorRgb);
                if (byId.put(id, spec) != null) {
                    throw new IllegalStateException("Duplicate tree-hole fluid " + id);
                }
                fluids.add(spec);
            }
            return new Catalog(List.copyOf(fluids), Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read tree-hole fluid mapping", failure);
        }
    }

    private record Catalog(
            List<BathMteFluidCatalog.FluidSpec> fluids,
            Map<ResourceLocation, BathMteFluidCatalog.FluidSpec> byId) {
        private Catalog {
            Objects.requireNonNull(fluids, "fluids");
            Objects.requireNonNull(byId, "byId");
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        String status;
        List<MappingRow> mapping;
    }

    private static final class MappingRow {
        @SerializedName("cc_fluid_id")
        String ccFluidId;
        @SerializedName("english_name")
        String englishName;
        @SerializedName("source_fluid")
        String sourceFluid;
        @SerializedName("color_rgb")
        int colorRgb;
    }
}
