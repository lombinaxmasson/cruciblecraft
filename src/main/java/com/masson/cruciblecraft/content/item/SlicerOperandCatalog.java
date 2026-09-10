package com.masson.cruciblecraft.content.item;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/** Source-backed GT6 food identities required by the live slicer recipes. */
public final class SlicerOperandCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/slicer_operands.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    private SlicerOperandCatalog() {}

    public static List<Operand> operands() {
        return CATALOG.operands();
    }

    private static Catalog loadBundled() {
        try (var stream = SlicerOperandCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing slicer operand catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || !"SLICER_OPERANDS".equals(document.status)
                    || document.operands == null
                    || document.operands.size() != document.operandCount) {
                throw new IllegalStateException(
                        "Slicer operand catalog schema, source, or count drifted");
            }
            List<Operand> operands = new ArrayList<>(document.operands.size());
            for (OperandRow row : document.operands) {
                ResourceLocation id = ResourceLocation.parse(row.runtimeId);
                if (!CrucibleCraft.MODID.equals(id.getNamespace())
                        || !row.registryPath.equals(id.getPath())) {
                    throw new IllegalStateException(
                            "Slicer operand runtime id drifted: " + row.runtimeId);
                }
                operands.add(new Operand(
                        id,
                        row.registryPath,
                        row.meta,
                        row.sourceItem,
                        row.englishName,
                        row.chineseName));
            }
            return new Catalog(SOURCE_REVISION, List.copyOf(operands));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read slicer operand catalog", failure);
        }
    }

    public record Operand(
            ResourceLocation id,
            String registryPath,
            int meta,
            String sourceItem,
            String englishName,
            String chineseName) {
        public Operand {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(registryPath, "registryPath");
            Objects.requireNonNull(sourceItem, "sourceItem");
            Objects.requireNonNull(englishName, "englishName");
            Objects.requireNonNull(chineseName, "chineseName");
        }
    }

    private record Catalog(String sourceRevision, List<Operand> operands) {}

    private static final class Document {
        @SerializedName("operand_count")
        int operandCount;
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        String status;
        List<OperandRow> operands;
    }

    private static final class OperandRow {
        @SerializedName("chinese_name")
        String chineseName;
        @SerializedName("english_name")
        String englishName;
        int meta;
        @SerializedName("registry_path")
        String registryPath;
        @SerializedName("runtime_id")
        String runtimeId;
        @SerializedName("source_item")
        String sourceItem;
    }
}
