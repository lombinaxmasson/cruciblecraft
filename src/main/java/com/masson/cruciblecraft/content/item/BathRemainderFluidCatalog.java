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

/** Explicit bath remainder overlay fluids with no legal hydrocarbon or bath/mte equivalent. */
public final class BathRemainderFluidCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/bath_remainder_fluid_mapping.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int FLUID_COUNT = 3;

    private BathRemainderFluidCatalog() {}

    public static List<BathMteFluidCatalog.FluidSpec> fluids() {
        return CATALOG.fluids();
    }

    private static Catalog loadBundled() {
        try (var stream = BathRemainderFluidCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled bath remainder fluid mapping " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"BATH_REMAINDER_FLUID_MAPPING".equals(document.status)
                    || document.mapping == null
                    || document.mapping.size() != FLUID_COUNT) {
                throw new IllegalStateException(
                        "Bath remainder fluid overlay must remain " + FLUID_COUNT + " fluids");
            }
            List<BathMteFluidCatalog.FluidSpec> fluids = new ArrayList<>(FLUID_COUNT);
            Map<ResourceLocation, BathMteFluidCatalog.FluidSpec> byId = new LinkedHashMap<>();
            for (MappingRow row : document.mapping) {
                ResourceLocation id = ResourceLocation.parse(row.ccFluidId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())) {
                    throw new IllegalStateException(
                            "Bath remainder fluid runtime id drifted: " + row.ccFluidId);
                }
                BathMteFluidCatalog.FluidSpec spec = new BathMteFluidCatalog.FluidSpec(
                        id,
                        row.sourceFluid,
                        row.englishName,
                        row.colorRgb);
                if (byId.put(id, spec) != null) {
                    throw new IllegalStateException("Duplicate bath remainder fluid " + id);
                }
                fluids.add(spec);
            }
            return new Catalog(List.copyOf(fluids), Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read bath remainder fluid mapping", failure);
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
