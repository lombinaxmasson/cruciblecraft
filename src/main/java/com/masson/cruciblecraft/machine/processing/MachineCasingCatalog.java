package com.masson.cruciblecraft.machine.processing;

import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/** Casing items plus electrolyzer-cable and distillery-wire extras. */
public final class MachineCasingCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/machine_casings.json";
    private static final MachineCasingCatalog BUNDLED = loadBundled();

    private final List<Casing> casings;
    private final Map<ResourceLocation, Casing> byId;
    private final Map<CasingKey, Casing> byMaterialFamily;
    private final Map<String, DistilleryWire> distilleryWires;

    public static MachineCasingCatalog bundled() {
        return BUNDLED;
    }

    public static List<Casing> casings() {
        return BUNDLED.casings;
    }

    public static Casing require(ResourceLocation id) {
        return BUNDLED.requireCasing(id);
    }

    public static Casing require(String material, String energyFamily) {
        return BUNDLED.requireCasing(material, energyFamily);
    }

    public static MachineCasingCatalog load(InputStream stream) {
        return fromDocument(CatalogJson.read(
                stream, Document.class, RESOURCE));
    }

    public static MachineCasingCatalog load(Reader reader) {
        return fromDocument(CatalogJson.read(
                reader, Document.class, RESOURCE));
    }

    public static String energyFamily(EnergyType energy) {
        Objects.requireNonNull(energy, "energy");
        if (energy == EnergyType.ELECTRIC) {
            return "eu_single";
        }
        if (energy == EnergyType.TIME) {
            return null;
        }
        return "kinetic_double";
    }

    private MachineCasingCatalog(
            List<Casing> casings,
            Map<String, DistilleryWire> distilleryWires) {
        this.casings = List.copyOf(casings);
        LinkedHashMap<ResourceLocation, Casing> ids = new LinkedHashMap<>();
        LinkedHashMap<CasingKey, Casing> keys = new LinkedHashMap<>();
        for (Casing casing : this.casings) {
            if (ids.putIfAbsent(casing.id(), casing) != null) {
                throw new IllegalStateException(
                        "Duplicate machine casing " + casing.id());
            }
            CasingKey key = new CasingKey(
                    materialPath(casing.materialId().toString()),
                    casing.energyFamily());
            if (keys.putIfAbsent(key, casing) != null) {
                throw new IllegalStateException(
                        "Duplicate machine casing key " + key);
            }
        }
        this.byId = Map.copyOf(ids);
        this.byMaterialFamily = Map.copyOf(keys);
        this.distilleryWires = Map.copyOf(distilleryWires);
    }

    public List<Casing> entries() {
        return casings;
    }

    public Casing requireCasing(ResourceLocation id) {
        Casing casing = byId.get(id);
        if (casing == null) {
            throw new IllegalStateException("Unknown machine casing " + id);
        }
        return casing;
    }

    public Casing requireCasing(String material, String energyFamily) {
        Casing casing = byMaterialFamily.get(
                new CasingKey(materialPath(material), energyFamily));
        if (casing == null) {
            if ("eu_single".equals(energyFamily)) {
                throw new IllegalStateException("No EU casing for " + material);
            }
            throw new IllegalStateException(
                    "No kinetic/heat casing for " + material);
        }
        return casing;
    }

    public String electrolyzerCableMaterial(String casingMaterial) {
        Casing casing = requireCasing(casingMaterial, "eu_single");
        if (!CatalogJson.nonBlank(casing.electrolyzerCableMaterial())) {
            throw new IllegalStateException(
                    "No electrolyzer cable for " + casingMaterial);
        }
        return casing.electrolyzerCableMaterial();
    }

    public DistilleryWire distilleryWire(String machineMaterial) {
        DistilleryWire wire = distilleryWires.get(materialPath(machineMaterial));
        if (wire == null) {
            throw new IllegalStateException(
                    "No distillery wire for " + machineMaterial);
        }
        return wire;
    }

    public boolean hasCasing(String material, String energyFamily) {
        return byMaterialFamily.containsKey(
                new CasingKey(materialPath(material), energyFamily));
    }

    private static MachineCasingCatalog loadBundled() {
        return fromDocument(CatalogJson.readBundled(
                MachineCasingCatalog.class, RESOURCE, Document.class));
    }

    private static MachineCasingCatalog fromDocument(Document document) {
        if (document.schemaVersion != 1
                || document.casings == null
                || document.casings.isEmpty()) {
            throw new IllegalStateException("Invalid machine casing catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        List<Casing> casings = document.casings.stream()
                .map(CasingRow::toCasing)
                .toList();
        LinkedHashMap<String, DistilleryWire> wires = new LinkedHashMap<>();
        if (document.machineMaterialExtras != null) {
            for (WireRow row : document.machineMaterialExtras) {
                DistilleryWire wire = row.toWire();
                if (wires.putIfAbsent(wire.materialPath(), wire) != null) {
                    throw new IllegalStateException(
                            "Duplicate distillery wire for "
                                    + wire.materialPath());
                }
            }
        }
        return new MachineCasingCatalog(casings, wires);
    }

    public static String materialPath(String material) {
        Objects.requireNonNull(material, "material");
        int colon = material.indexOf(':');
        return colon >= 0 ? material.substring(colon + 1) : material;
    }

    public record Casing(
            ResourceLocation id,
            ResourceLocation materialId,
            String energyFamily,
            boolean doubled,
            boolean creativeVisible,
            String langZh,
            String langEn,
            String electrolyzerCableMaterial) {
        public Casing {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(energyFamily, "energyFamily");
            Objects.requireNonNull(langZh, "langZh");
            Objects.requireNonNull(langEn, "langEn");
        }
    }

    public record DistilleryWire(
            String materialPath,
            String wireMaterial,
            String wirePrefix) {
        public DistilleryWire {
            Objects.requireNonNull(materialPath, "materialPath");
            Objects.requireNonNull(wireMaterial, "wireMaterial");
            Objects.requireNonNull(wirePrefix, "wirePrefix");
        }
    }

    private record CasingKey(String materialPath, String energyFamily) {}

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        private List<CasingRow> casings;
        @SerializedName("machine_material_extras")
        private List<WireRow> machineMaterialExtras;
    }

    private static final class CasingRow {
        private String id;
        private String material;
        @SerializedName("energy_family")
        private String energyFamily;
        private boolean doubled;
        @SerializedName("creative_visible")
        private boolean creativeVisible;
        @SerializedName("lang_key_zh")
        private String langKeyZh;
        @SerializedName("lang_key_en")
        private String langKeyEn;
        @SerializedName("electrolyzer_cable_material")
        private String electrolyzerCableMaterial;

        private Casing toCasing() {
            ResourceLocation itemId = ResourceLocation.tryParse(id);
            ResourceLocation materialId = ResourceLocation.tryParse(material);
            if (itemId == null || materialId == null
                    || !CatalogJson.nonBlank(energyFamily)
                    || !CatalogJson.nonBlank(langKeyZh)
                    || !CatalogJson.nonBlank(langKeyEn)) {
                throw new IllegalStateException(
                        "Incomplete machine casing row " + id);
            }
            return new Casing(
                    itemId,
                    materialId,
                    energyFamily,
                    doubled,
                    creativeVisible,
                    langKeyZh,
                    langKeyEn,
                    electrolyzerCableMaterial);
        }
    }

    private static final class WireRow {
        private String material;
        @SerializedName("distillery_wire_material")
        private String distilleryWireMaterial;
        @SerializedName("distillery_wire_prefix")
        private String distilleryWirePrefix;

        private DistilleryWire toWire() {
            if (!CatalogJson.nonBlank(material)
                    || !CatalogJson.nonBlank(distilleryWireMaterial)
                    || !CatalogJson.nonBlank(distilleryWirePrefix)) {
                throw new IllegalStateException(
                        "Incomplete distillery wire extras");
            }
            return new DistilleryWire(
                    materialPath(material),
                    distilleryWireMaterial,
                    distilleryWirePrefix);
        }
    }
}
