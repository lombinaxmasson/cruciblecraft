package com.masson.cruciblecraft.machine.processing;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;

import net.minecraft.resources.ResourceLocation;

/**
 * Test-only overlay loader. Production static catalogs are never mutated.
 */
public final class CatalogTestSupport {
    public static final String OVERLAY_ROOT =
            "/data/cruciblecraft/repair_overlay/";

    public static Loaded loadProduction() {
        return new Loaded(
                MachineTierCatalog.loadIsolated(
                        requireResource("/data/cruciblecraft/machine_tiers.json")),
                MachineKindCatalog.bundled(),
                MachineCasingCatalog.bundled(),
                MachineAcquisitionCatalog.bundled(),
                DeviceMaterialCatalog.bundled());
    }

    public static Loaded loadOverlay() {
        return loadOverlay(OVERLAY_ROOT);
    }

    public static Loaded loadOverlay(String overlayRoot) {
        Objects.requireNonNull(overlayRoot, "overlayRoot");
        MachineTierCatalog.IsolatedCatalog tiers = MachineTierCatalog.loadIsolated(
                mergedStream(
                        "/data/cruciblecraft/machine_tiers.json",
                        overlayRoot + "machine_tiers.json",
                        CatalogTestSupport::mergeMachineTiers));
        MachineKindCatalog kinds = MachineKindCatalog.load(
                mergedStream(
                        "/data/cruciblecraft/machine_kinds.json",
                        overlayRoot + "machine_kinds.json",
                        CatalogTestSupport::mergeKinds));
        MachineCasingCatalog casings = MachineCasingCatalog.load(
                mergedStream(
                        "/data/cruciblecraft/machine_casings.json",
                        overlayRoot + "machine_casings.json",
                        CatalogTestSupport::mergeCasings));
        MachineAcquisitionCatalog acquisition = MachineAcquisitionCatalog.load(
                mergedStream(
                        "/data/cruciblecraft/machine_acquisition.json",
                        overlayRoot + "machine_acquisition.json",
                        CatalogTestSupport::mergeAcquisition));
        DeviceMaterialCatalog devices = DeviceMaterialCatalog.load(
                mergedStream(
                        "/data/cruciblecraft/device_materials.json",
                        overlayRoot + "device_materials.json",
                        CatalogTestSupport::mergeDevices));
        return new Loaded(tiers, kinds, casings, acquisition, devices);
    }

    public record Loaded(
            MachineTierCatalog.IsolatedCatalog tiers,
            MachineKindCatalog kinds,
            MachineCasingCatalog casings,
            MachineAcquisitionCatalog acquisition,
            DeviceMaterialCatalog devices) {
        public Loaded {
            Objects.requireNonNull(tiers, "tiers");
            Objects.requireNonNull(kinds, "kinds");
            Objects.requireNonNull(casings, "casings");
            Objects.requireNonNull(acquisition, "acquisition");
            Objects.requireNonNull(devices, "devices");
        }

        public boolean containsVariant(String id) {
            return tiers.byId().containsKey(ResourceLocation.parse(id));
        }

        public DeviceMaterialCatalog.DeviceSpec crucible() {
            return devices.requireDevice(Device.CRUCIBLE);
        }
    }

    private static InputStream mergedStream(
            String productionResource,
            String overlayResource,
            Merger merger) {
        JsonObject production = readObject(productionResource);
        JsonObject overlay = readOptionalObject(overlayResource);
        JsonObject merged = overlay == null
                ? production
                : merger.merge(production, overlay);
        byte[] bytes = CatalogJson.GSON.toJson(merged).getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(bytes);
    }

    private static JsonObject mergeMachineTiers(
            JsonObject production, JsonObject overlay) {
        JsonObject merged = production.deepCopy();
        appendArray(merged, overlay, "variants");
        JsonObject source = merged.getAsJsonObject("source");
        JsonObject overlaySource = overlay.getAsJsonObject("source");
        if (source != null && overlaySource != null
                && overlaySource.has("variant_rows")) {
            JsonObject rows = source.getAsJsonObject("variant_rows");
            overlaySource.getAsJsonObject("variant_rows").entrySet()
                    .forEach(entry -> rows.add(entry.getKey(), entry.getValue()));
        }
        return merged;
    }

    private static JsonObject mergeKinds(
            JsonObject production, JsonObject overlay) {
        JsonObject merged = production.deepCopy();
        appendArray(merged, overlay, "kinds");
        if (overlay.has("material_lang")) {
            JsonObject lang = merged.getAsJsonObject("material_lang");
            overlay.getAsJsonObject("material_lang").entrySet()
                    .forEach(entry -> lang.add(entry.getKey(), entry.getValue()));
        }
        return merged;
    }

    private static JsonObject mergeCasings(
            JsonObject production, JsonObject overlay) {
        JsonObject merged = production.deepCopy();
        appendArray(merged, overlay, "electrolyzer_cables");
        appendArray(merged, overlay, "machine_material_extras");
        return merged;
    }

    private static JsonObject mergeAcquisition(
            JsonObject production, JsonObject overlay) {
        JsonObject merged = production.deepCopy();
        appendArray(merged, overlay, "overrides");
        return merged;
    }

    private static JsonObject mergeDevices(
            JsonObject production, JsonObject overlay) {
        JsonObject merged = production.deepCopy();
        JsonObject overlayDevices = overlay.getAsJsonObject("devices");
        if (overlayDevices == null) {
            return merged;
        }
        JsonObject devices = merged.getAsJsonObject("devices");
        overlayDevices.entrySet().forEach(entry -> {
            JsonObject target = devices.getAsJsonObject(entry.getKey());
            JsonObject extra = entry.getValue().getAsJsonObject();
            if (extra.has("materials")) {
                JsonArray materials = target.getAsJsonArray("materials");
                extra.getAsJsonArray("materials").forEach(materials::add);
            }
        });
        return merged;
    }

    private static void appendArray(
            JsonObject merged, JsonObject overlay, String key) {
        if (!overlay.has(key)) {
            return;
        }
        JsonArray target = merged.getAsJsonArray(key);
        if (target == null) {
            merged.add(key, overlay.get(key).deepCopy());
            return;
        }
        overlay.getAsJsonArray(key).forEach(target::add);
    }

    private static JsonObject readObject(String resource) {
        try (InputStream stream = requireResource(resource);
                InputStreamReader reader = new InputStreamReader(
                        stream, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Catalog is not an object " + resource);
            }
            return element.getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not read catalog " + resource, exception);
        }
    }

    private static JsonObject readOptionalObject(String resource) {
        InputStream stream = CatalogTestSupport.class.getResourceAsStream(resource);
        if (stream == null) {
            return null;
        }
        try (stream; InputStreamReader reader = new InputStreamReader(
                stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not read overlay " + resource, exception);
        }
    }

    private static InputStream requireResource(String resource) {
        InputStream stream = CatalogTestSupport.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("Missing resource " + resource);
        }
        return stream;
    }

    @FunctionalInterface
    private interface Merger {
        JsonObject merge(JsonObject production, JsonObject overlay);
    }

    private CatalogTestSupport() {}
}
