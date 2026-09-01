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

/** Explicit bath/mte overlay fluids with no legal hydrocarbon equivalent. */
public final class BathMteFluidCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/bath_mte_fluid_mapping.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static final int FLUID_COUNT = 30;

    private BathMteFluidCatalog() {}

    public static List<FluidSpec> fluids() {
        return CATALOG.fluids();
    }

    private static Catalog loadBundled() {
        try (var stream = BathMteFluidCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled bath/mte fluid mapping " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"BATH_MTE_FLUID_MAPPING".equals(document.status)
                    || document.mapping == null
                    || document.mapping.size() != FLUID_COUNT) {
                throw new IllegalStateException(
                        "Bath/mte fluid overlay must remain " + FLUID_COUNT + " fluids");
            }
            List<FluidSpec> fluids = new ArrayList<>(FLUID_COUNT);
            Map<ResourceLocation, FluidSpec> byId = new LinkedHashMap<>();
            for (MappingRow row : document.mapping) {
                ResourceLocation id = ResourceLocation.parse(row.ccFluidId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())) {
                    throw new IllegalStateException(
                            "Bath/mte fluid runtime id drifted: " + row.ccFluidId);
                }
                FluidSpec spec = new FluidSpec(
                        id,
                        row.sourceFluid,
                        row.englishName,
                        parseColor(row.ccFluidId));
                if (byId.put(id, spec) != null) {
                    throw new IllegalStateException(
                            "Duplicate bath/mte fluid " + id);
                }
                fluids.add(spec);
            }
            return new Catalog(List.copyOf(fluids), Map.copyOf(byId));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read bath/mte fluid mapping", failure);
        }
    }

    private static int parseColor(String runtimeId) {
        String path = ResourceLocation.parse(runtimeId).getPath();
        if (path.contains("black")) {
            return 0x1D1D21;
        }
        if (path.contains("blue") && path.contains("light")) {
            return 0x3AB3DA;
        }
        if (path.contains("blue")) {
            return 0x3C44AA;
        }
        if (path.contains("brown")) {
            return 0x835432;
        }
        if (path.contains("cyan")) {
            return 0x169C9C;
        }
        if (path.contains("gray") && path.contains("light")) {
            return 0x9D9D97;
        }
        if (path.contains("gray")) {
            return 0x474F52;
        }
        if (path.contains("green") && path.contains("lime")) {
            return 0x80C71F;
        }
        if (path.contains("green")) {
            return 0x5E7C16;
        }
        if (path.contains("lime")) {
            return 0x80C71F;
        }
        if (path.contains("magenta")) {
            return 0xC74EBD;
        }
        if (path.contains("orange")) {
            return 0xF9801D;
        }
        if (path.contains("pink")) {
            return 0xF38BAA;
        }
        if (path.contains("purple")) {
            return 0x8932B8;
        }
        if (path.contains("red")) {
            return 0xB02E26;
        }
        if (path.contains("white")) {
            return 0xF9FFFE;
        }
        if (path.contains("yellow")) {
            return 0xFED83D;
        }
        if (path.contains("indigo")) {
            return 0x4B0082;
        }
        if (path.contains("squid")) {
            return 0x1B1B2A;
        }
        return 0x7A7A7A;
    }

    public record FluidSpec(
            ResourceLocation id,
            String sourceFluid,
            String englishName,
            int colorRgb) {
        public FluidSpec {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(sourceFluid, "sourceFluid");
            Objects.requireNonNull(englishName, "englishName");
        }
    }

    private record Catalog(
            List<FluidSpec> fluids,
            Map<ResourceLocation, FluidSpec> byId) {}

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
    }
}
