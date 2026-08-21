package com.masson.cruciblecraft.logistics.hopper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * Flat 60-row SOURCE_BACKED hopper catalog projected 1-to-2 into Hopper and
 * Queue Hopper variants. There is no {@code tierOf}.
 */
public final class HopperVariantCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/hopper_variants.json";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static List<BaseEntry> baseEntries() {
        return CATALOG.baseEntries();
    }

    public static List<HopperVariant> variants() {
        return CATALOG.variants();
    }

    public static List<HopperVariant> variantsOf(HopperKind kind) {
        Objects.requireNonNull(kind, "kind");
        return CATALOG.byKind().getOrDefault(kind, List.of());
    }

    public static HopperVariant require(ResourceLocation id) {
        HopperVariant variant = CATALOG.byId().get(id);
        if (variant == null) {
            throw new IllegalArgumentException("Unknown hopper variant " + id);
        }
        return variant;
    }

    private static Catalog loadBundled() {
        try (var stream = HopperVariantCatalog.class.getResourceAsStream(
                RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled hopper catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !"3703e40308c8c030763fd6297dea8b210d2a77b1"
                            .equals(document.sourceRevision)
                    || document.variants == null
                    || document.variants.size() != 60) {
                throw new IllegalStateException(
                        "Hopper catalog must remain 60 SOURCE_BACKED rows");
            }
            List<BaseEntry> bases = new ArrayList<>(60);
            List<HopperVariant> variants = new ArrayList<>(120);
            Map<ResourceLocation, HopperVariant> byId = new LinkedHashMap<>();
            EnumMap<HopperKind, List<HopperVariant>> byKind =
                    new EnumMap<>(HopperKind.class);
            byKind.put(HopperKind.HOPPER, new ArrayList<>(60));
            byKind.put(HopperKind.QUEUE_HOPPER, new ArrayList<>(60));
            Set<String> materials = new HashSet<>();
            for (Row row : document.variants) {
                BaseEntry base = row.toEntry();
                if (!materials.add(base.materialId().toString())) {
                    throw new IllegalStateException(
                            "Duplicate hopper material " + base.materialId());
                }
                bases.add(base);
                HopperVariant hopper = project(base, HopperKind.HOPPER, base.slots());
                HopperVariant queue = project(
                        base,
                        HopperKind.QUEUE_HOPPER,
                        Math.max(2, base.slots()));
                add(byId, byKind, hopper);
                add(byId, byKind, queue);
                variants.add(hopper);
                variants.add(queue);
            }
            if (variants.size() != 120 || byId.size() != 120) {
                throw new IllegalStateException(
                        "Hopper projection must be 120 unique variants");
            }
            return new Catalog(
                    List.copyOf(bases),
                    List.copyOf(variants),
                    Map.copyOf(byId),
                    Map.of(
                            HopperKind.HOPPER,
                            List.copyOf(byKind.get(HopperKind.HOPPER)),
                            HopperKind.QUEUE_HOPPER,
                            List.copyOf(byKind.get(HopperKind.QUEUE_HOPPER))));
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to read hopper catalog", failure);
        }
    }

    private static HopperVariant project(
            BaseEntry base, HopperKind kind, int slots) {
        return new HopperVariant(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID,
                        base.materialPath() + "_" + kind.pathSuffix()),
                kind,
                base.materialId(),
                slots);
    }

    private static void add(
            Map<ResourceLocation, HopperVariant> byId,
            EnumMap<HopperKind, List<HopperVariant>> byKind,
            HopperVariant variant) {
        if (byId.put(variant.id(), variant) != null) {
            throw new IllegalStateException(
                    "Duplicate hopper variant " + variant.id());
        }
        byKind.get(variant.kind()).add(variant);
    }

    public record BaseEntry(ResourceLocation materialId, int slots) {
        public String materialPath() {
            return materialId.getPath();
        }
    }

    private record Catalog(
            List<BaseEntry> baseEntries,
            List<HopperVariant> variants,
            Map<ResourceLocation, HopperVariant> byId,
            Map<HopperKind, List<HopperVariant>> byKind) {}

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        List<Row> variants;
    }

    private static final class Row {
        String material;
        int slots;

        BaseEntry toEntry() {
            if (material == null || !material.startsWith("cruciblecraft:")) {
                throw new IllegalStateException(
                        "Hopper row material must be CC-namespaced");
            }
            if (slots < 1 || slots > 36) {
                throw new IllegalStateException(
                        "Hopper row slots must be 1..36: " + material);
            }
            return new BaseEntry(ResourceLocation.parse(material), slots);
        }
    }

    private HopperVariantCatalog() {}
}
