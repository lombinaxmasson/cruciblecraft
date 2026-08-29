package com.masson.cruciblecraft.machine.processing;

import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;

import net.minecraft.resources.ResourceLocation;

/** Allowed materials, processing tiers and durability for crucible/anvil/hammer. */
public final class DeviceMaterialCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/device_materials.json";
    private static final DeviceMaterialCatalog BUNDLED = loadBundled();

    private final Map<Device, DeviceSpec> devices;

    public static DeviceMaterialCatalog bundled() {
        return BUNDLED;
    }

    public static DeviceSpec require(Device device) {
        return BUNDLED.requireDevice(device);
    }

    public static DeviceMaterialCatalog load(InputStream stream) {
        return fromDocument(CatalogJson.read(
                stream, Document.class, RESOURCE));
    }

    public static DeviceMaterialCatalog load(Reader reader) {
        return fromDocument(CatalogJson.read(
                reader, Document.class, RESOURCE));
    }

    private DeviceMaterialCatalog(Map<Device, DeviceSpec> devices) {
        this.devices = Map.copyOf(devices);
        if (!this.devices.containsKey(Device.CRUCIBLE)
                || !this.devices.containsKey(Device.ANVIL)
                || !this.devices.containsKey(Device.HAMMER)) {
            throw new IllegalStateException(
                    "Device material catalog must declare crucible, anvil and hammer");
        }
    }

    public DeviceSpec requireDevice(Device device) {
        DeviceSpec spec = devices.get(device);
        if (spec == null) {
            throw new IllegalStateException("Unknown device " + device);
        }
        return spec;
    }

    private static DeviceMaterialCatalog loadBundled() {
        return fromDocument(CatalogJson.readBundled(
                DeviceMaterialCatalog.class, RESOURCE, Document.class));
    }

    private static DeviceMaterialCatalog fromDocument(Document document) {
        if (document.schemaVersion != 1 || document.devices == null) {
            throw new IllegalStateException("Invalid device material catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        LinkedHashMap<Device, DeviceSpec> devices = new LinkedHashMap<>();
        devices.put(Device.CRUCIBLE, document.devices.crucible.toSpec(Device.CRUCIBLE));
        devices.put(Device.ANVIL, document.devices.anvil.toSpec(Device.ANVIL));
        devices.put(Device.HAMMER, document.devices.hammer.toSpec(Device.HAMMER));
        return new DeviceMaterialCatalog(devices);
    }

    public record DeviceSpec(
            Device device,
            String defaultMaterial,
            List<Material> materials,
            Map<String, Material> byId) {
        public DeviceSpec {
            Objects.requireNonNull(device, "device");
            Objects.requireNonNull(defaultMaterial, "defaultMaterial");
            materials = List.copyOf(materials);
            byId = Map.copyOf(byId);
            if (!byId.containsKey(defaultMaterial)) {
                throw new IllegalStateException(
                        device + " default material is not in the catalog");
            }
        }

        public boolean isAllowed(String materialId) {
            return materialId != null && byId.containsKey(materialId);
        }

        public Material require(String materialId) {
            Material material = byId.get(materialId);
            if (material == null) {
                throw new IllegalArgumentException(
                        "Unsupported " + device.name().toLowerCase()
                                + " material: " + materialId);
            }
            return material;
        }

        public List<Material> creativeVisible() {
            return materials.stream()
                    .filter(Material::creativeVisible)
                    .toList();
        }
    }

    public record Material(
            String materialId,
            int processingTier,
            Optional<Long> durability,
            boolean creativeVisible) {
        public Material {
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(durability, "durability");
            if (processingTier < 0) {
                throw new IllegalArgumentException(
                        "processing tier must be non-negative");
            }
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        private DevicesRow devices;
    }

    private static final class DevicesRow {
        private DeviceRow crucible;
        private DeviceRow anvil;
        private DeviceRow hammer;
    }

    private static final class DeviceRow {
        @SerializedName("default_material")
        private String defaultMaterial;
        private List<MaterialRow> materials;

        private DeviceSpec toSpec(Device device) {
            if (!CatalogJson.nonBlank(defaultMaterial)
                    || materials == null
                    || materials.isEmpty()) {
                throw new IllegalStateException(
                        "Incomplete device material rows for " + device);
            }
            LinkedHashMap<String, Material> byId = new LinkedHashMap<>();
            List<Material> parsed = materials.stream()
                    .map(MaterialRow::toMaterial)
                    .toList();
            for (Material material : parsed) {
                if (byId.putIfAbsent(material.materialId(), material) != null) {
                    throw new IllegalStateException(
                            "Duplicate " + device + " material "
                                    + material.materialId());
                }
            }
            return new DeviceSpec(device, defaultMaterial, parsed, byId);
        }
    }

    private static final class MaterialRow {
        @SerializedName("material_id")
        private String materialId;
        @SerializedName("processing_tier")
        private int processingTier;
        private Long durability;
        @SerializedName("creative_visible")
        private boolean creativeVisible;

        private Material toMaterial() {
            if (!CatalogJson.nonBlank(materialId)) {
                throw new IllegalStateException("Device material id is blank");
            }
            return new Material(
                    materialId,
                    processingTier,
                    Optional.ofNullable(durability),
                    creativeVisible);
        }
    }
}
