package com.masson.cruciblecraft.machine.processing;

import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/**
 * Electrolyzer-cable and distillery-wire extras. Casing items are material
 * forms {@code {material}/machine_casing} and {@code .../machine_casing_double}.
 */
public final class MachineCasingCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/machine_casings.json";
    private static final MachineCasingCatalog BUNDLED = loadBundled();

    private final Map<String, String> electrolyzerCables;
    private final Map<String, DistilleryWire> distilleryWires;

    public static MachineCasingCatalog bundled() {
        return BUNDLED;
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

    public static ResourceLocation casingItemId(
            String material, String energyFamily) {
        String path = materialPath(material);
        String form = switch (energyFamily) {
            case "eu_single" -> "machine_casing";
            case "kinetic_double" -> "machine_casing_double";
            default -> throw new IllegalStateException(
                    "Unknown casing energy family " + energyFamily);
        };
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, path + "/" + form);
    }

    public static String materialPathFromCasingItem(ResourceLocation casingItem) {
        Objects.requireNonNull(casingItem, "casingItem");
        String path = casingItem.getPath();
        int slash = path.indexOf('/');
        return slash < 0 ? path : path.substring(0, slash);
    }

    private MachineCasingCatalog(
            Map<String, String> electrolyzerCables,
            Map<String, DistilleryWire> distilleryWires) {
        this.electrolyzerCables = Map.copyOf(electrolyzerCables);
        this.distilleryWires = Map.copyOf(distilleryWires);
    }

    public String electrolyzerCableMaterial(String casingMaterial) {
        String cable = electrolyzerCables.get(materialPath(casingMaterial));
        if (!CatalogJson.nonBlank(cable)) {
            throw new IllegalStateException(
                    "No electrolyzer cable for " + casingMaterial);
        }
        return cable;
    }

    public DistilleryWire distilleryWire(String machineMaterial) {
        DistilleryWire wire = distilleryWires.get(materialPath(machineMaterial));
        if (wire == null) {
            throw new IllegalStateException(
                    "No distillery wire for " + machineMaterial);
        }
        return wire;
    }

    public boolean hasElectrolyzerCable(String material) {
        return electrolyzerCables.containsKey(materialPath(material));
    }

    private static MachineCasingCatalog loadBundled() {
        return fromDocument(CatalogJson.readBundled(
                MachineCasingCatalog.class, RESOURCE, Document.class));
    }

    private static MachineCasingCatalog fromDocument(Document document) {
        if (document.schemaVersion != 1) {
            throw new IllegalStateException("Invalid machine casing catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        LinkedHashMap<String, String> cables = new LinkedHashMap<>();
        if (document.electrolyzerCables != null) {
            for (CableRow row : document.electrolyzerCables) {
                if (!CatalogJson.nonBlank(row.material)
                        || !CatalogJson.nonBlank(row.cableMaterial)) {
                    throw new IllegalStateException(
                            "Incomplete electrolyzer cable extras");
                }
                String path = materialPath(row.material);
                if (cables.putIfAbsent(path, row.cableMaterial) != null) {
                    throw new IllegalStateException(
                            "Duplicate electrolyzer cable for " + path);
                }
            }
        }
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
        return new MachineCasingCatalog(cables, wires);
    }

    public static String materialPath(String material) {
        Objects.requireNonNull(material, "material");
        int colon = material.indexOf(':');
        return colon >= 0 ? material.substring(colon + 1) : material;
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

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        @SerializedName("electrolyzer_cables")
        private List<CableRow> electrolyzerCables;
        @SerializedName("machine_material_extras")
        private List<WireRow> machineMaterialExtras;
    }

    private static final class CableRow {
        private String material;
        @SerializedName("cable_material")
        private String cableMaterial;
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
