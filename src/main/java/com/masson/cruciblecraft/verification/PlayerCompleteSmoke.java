package com.masson.cruciblecraft.verification;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.registry.ModCreativeTabs;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Shared player-complete smoke snapshot for GameTestServer and runClient. */
public final class PlayerCompleteSmoke {
    private static final String FLUID_CAPABILITY =
            "logistics/fluid-network/basic-transfer";
    private static final String ITEM_CAPABILITY =
            "logistics/item-network-core";
    private static final String GENERIC_CAPABILITY =
            "logistics/generic-network/core";
    private static final String CORE_CAPABILITY =
            "logistics/logistics-core";
    private static final String SURFACE_RESOURCE =
            "/cruciblecraft/player_complete_surfaces.json";

    private record Surface(
            String capability,
            List<String> registryIds,
            List<ResourceLocation> definitionIds) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EMI_PLUGIN =
            "com.masson.cruciblecraft.compat.emi.CrucibleCraftEmiPlugin";
    private static final Map<String, Surface> SURFACES = loadSurfaces();

    private PlayerCompleteSmoke() {}

    private static String configuredCapability() {
        return System.getProperty(
                "cruciblecraft.playerCapability",
                FLUID_CAPABILITY);
    }

    private static Map<String, Surface> loadSurfaces() {
        Map<String, Surface> loaded = new LinkedHashMap<>();
        try (InputStream stream = PlayerCompleteSmoke.class.getResourceAsStream(
                SURFACE_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + SURFACE_RESOURCE);
            }
            JsonObject root = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    JsonObject.class);
            JsonObject surfaces = root.getAsJsonObject("surfaces");
            for (Map.Entry<String, JsonElement> entry : surfaces.entrySet()) {
                JsonObject row = entry.getValue().getAsJsonObject();
                List<String> registryIds = strings(row.getAsJsonArray("registry_ids"));
                List<ResourceLocation> definitionIds = new ArrayList<>();
                for (String id : strings(row.getAsJsonArray("definition_ids"))) {
                    definitionIds.add(ResourceLocation.parse(id));
                }
                loaded.put(
                        entry.getKey(),
                        new Surface(entry.getKey(), registryIds, definitionIds));
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not load player-complete surfaces",
                    exception);
        }
        return Map.copyOf(loaded);
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return List.of();
        }
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return List.copyOf(values);
    }

    private static Surface surface(String capability) {
        return SURFACES.getOrDefault(
                capability,
                new Surface(capability, List.of(), List.of()));
    }

    public static JsonObject snapshot(String runtime) {
        return snapshot(runtime, configuredCapability());
    }

    public static JsonObject snapshot(String runtime, String capability) {
        Surface surface = surface(capability);
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", 1);
        root.addProperty("capability", surface.capability());
        root.addProperty("runtime", runtime);
        root.addProperty(
                "run_nonce",
                System.getProperty("cruciblecraft.smokeNonce", ""));
        root.addProperty("emi_plugin_class", EMI_PLUGIN);
        root.addProperty("modid", CrucibleCraft.MODID);
        JsonArray required = new JsonArray();
        JsonArray observed = new JsonArray();
        boolean missing = false;
        for (String id : surface.registryIds()) {
            required.add(id);
            ResourceLocation parsed = ResourceLocation.parse(id);
            if (BuiltInRegistries.ITEM.containsKey(parsed)) {
                observed.add(id);
            } else {
                missing = true;
            }
        }
        root.add("required_registry_ids", required);
        root.add("registry_ids", observed);
        boolean covers = surface.definitionIds().isEmpty()
                || surface.definitionIds().stream()
                        .allMatch(id -> CoverDefinitionCatalog.find(id).isPresent());
        root.addProperty("cover_definitions", covers);
        root.addProperty(
                "fluid_cover_definitions",
                FLUID_CAPABILITY.equals(surface.capability()) && covers);
        root.addProperty(
                "item_cover_definitions",
                ITEM_CAPABILITY.equals(surface.capability()) && covers);
        root.addProperty(
                "generic_cover_definitions",
                GENERIC_CAPABILITY.equals(surface.capability()) && covers);
        root.addProperty(
                "dump_cover_definitions",
                CORE_CAPABILITY.equals(surface.capability()) && covers);
        boolean tab = BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(
                ModCreativeTabs.MAIN.getId());
        root.addProperty("creative_tab", tab);
        boolean emiPlan = false;
        try {
            emiPlan = !ProcessingEmiRegistrationPlan.create(
                    ModProcessingMachines.CONFIGURED_MACHINES)
                    .machines()
                    .isEmpty();
        } catch (RuntimeException ignored) {
            emiPlan = false;
        }
        root.addProperty("emi_registration_plan", emiPlan);
        boolean pass = !surface.registryIds().isEmpty()
                && !missing
                && covers
                && tab
                && emiPlan;
        root.addProperty("status", pass ? "PASS" : "FAIL");
        return root;
    }

    public static void writeIfConfigured(String runtime) {
        writeIfConfigured(runtime, configuredCapability());
    }

    public static void writeIfConfigured(
            String runtime,
            String capability) {
        String raw = System.getProperty("cruciblecraft.smokeReceipt");
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(raw);
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    GSON.toJson(snapshot(runtime, capability)) + "\n",
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            CrucibleCraft.LOGGER.error(
                    "Could not write player-complete smoke receipt",
                    exception);
        }
    }
}
