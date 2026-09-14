package com.masson.cruciblecraft.energy.longdistance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** GT6 10064–10068 endpoints plus five LongDistWire01 voltage hosts. */
public final class LongDistanceTransformerCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/long_distance_transformers.json";
    public static final int EXPECTED_SIZE = 5;
    private static final Catalog CATALOG = loadBundled();

    private LongDistanceTransformerCatalog() {}

    public static List<LongDistanceTransformerProfile> endpoints() {
        return CATALOG.endpoints;
    }

    public static List<LongDistanceWireProfile> wires() {
        return CATALOG.wires;
    }

    public static LongDistanceTransformerProfile requireEndpoint(ResourceLocation id) {
        LongDistanceTransformerProfile profile = CATALOG.byEndpoint.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown long-distance transformer " + id);
        }
        return profile;
    }

    public static LongDistanceWireProfile requireWire(ResourceLocation id) {
        LongDistanceWireProfile profile = CATALOG.byWire.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown long-distance wire " + id);
        }
        return profile;
    }

    private record Catalog(
            List<LongDistanceTransformerProfile> endpoints,
            List<LongDistanceWireProfile> wires,
            Map<ResourceLocation, LongDistanceTransformerProfile> byEndpoint,
            Map<ResourceLocation, LongDistanceWireProfile> byWire) {}

    private static Catalog loadBundled() {
        Document document = CatalogJson.readBundled(
                LongDistanceTransformerCatalog.class, RESOURCE, Document.class);
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        if (document.schemaVersion != 1
                || document.expectedSize != EXPECTED_SIZE
                || document.endpoints == null
                || document.wires == null
                || document.endpoints.size() != EXPECTED_SIZE
                || document.wires.size() != EXPECTED_SIZE) {
            throw new IllegalStateException("Invalid long-distance transformer catalog");
        }
        LinkedHashMap<ResourceLocation, LongDistanceTransformerProfile> byEndpoint =
                new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, LongDistanceWireProfile> byWire =
                new LinkedHashMap<>();
        List<LongDistanceTransformerProfile> endpoints = document.endpoints.stream()
                .map(EndpointRow::toProfile)
                .toList();
        List<LongDistanceWireProfile> wires = document.wires.stream()
                .map(WireRow::toProfile)
                .toList();
        for (LongDistanceTransformerProfile profile : endpoints) {
            Objects.requireNonNull(profile);
            if (byEndpoint.put(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate long-distance transformer " + profile.id());
            }
        }
        for (LongDistanceWireProfile profile : wires) {
            if (byWire.put(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate long-distance wire " + profile.id());
            }
        }
        return new Catalog(
                List.copyOf(endpoints),
                List.copyOf(wires),
                Map.copyOf(byEndpoint),
                Map.copyOf(byWire));
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        @SerializedName("expected_size")
        private int expectedSize;
        private List<EndpointRow> endpoints;
        private List<WireRow> wires;
    }

    private static final class EndpointRow {
        private String id;
        @SerializedName("source_id")
        private int sourceId;
        @SerializedName("source_line")
        private int sourceLine;
        @SerializedName("voltage_index")
        private int voltageIndex;
        private long voltage;
        private String material;
        @SerializedName("host_transformer")
        private String hostTransformer;
        @SerializedName("lang_en")
        private String langEn;
        @SerializedName("lang_zh")
        private String langZh;

        private LongDistanceTransformerProfile toProfile() {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            ResourceLocation host = ResourceLocation.tryParse(hostTransformer);
            if (parsed == null
                    || host == null
                    || !CatalogJson.nonBlank(material)
                    || !CatalogJson.nonBlank(langEn)
                    || !CatalogJson.nonBlank(langZh)) {
                throw new IllegalStateException(
                        "Incomplete long-distance transformer " + id);
            }
            return new LongDistanceTransformerProfile(
                    parsed,
                    sourceId,
                    sourceLine,
                    voltageIndex,
                    voltage,
                    material,
                    host,
                    langEn,
                    langZh);
        }
    }

    private static final class WireRow {
        private String id;
        @SerializedName("voltage_index")
        private int voltageIndex;
        private long voltage;
        private String core;
        @SerializedName("lang_en")
        private String langEn;
        @SerializedName("lang_zh")
        private String langZh;

        private LongDistanceWireProfile toProfile() {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null
                    || !CatalogJson.nonBlank(core)
                    || !CatalogJson.nonBlank(langEn)
                    || !CatalogJson.nonBlank(langZh)) {
                throw new IllegalStateException("Incomplete long-distance wire " + id);
            }
            return new LongDistanceWireProfile(
                    parsed, voltageIndex, voltage, core, langEn, langZh);
        }
    }
}
