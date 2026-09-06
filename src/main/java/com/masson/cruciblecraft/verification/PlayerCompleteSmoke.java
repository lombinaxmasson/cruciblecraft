package com.masson.cruciblecraft.verification;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.logistics.core.LogisticsDumpKinds;
import com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuKinds;
import com.masson.cruciblecraft.logistics.fluidnet.FluidNetworkKinds;
import com.masson.cruciblecraft.logistics.genericnet.GenericNetworkKinds;
import com.masson.cruciblecraft.logistics.itemnet.ItemNetworkKinds;
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
    private static final String DISPLAY_CAPABILITY =
            "logistics/display-cpu";
    private static final String CONVERTER_CAPABILITY =
            "energy/converter-catalog";
    private static final String BATTERIES_CAPABILITY =
            "energy/batteries";
    private static final String TRANSFORMERS_CAPABILITY =
            "energy/transformers";
    private record Surface(
            String capability,
            List<String> registryIds,
            List<ResourceLocation> definitionIds) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EMI_PLUGIN =
            "com.masson.cruciblecraft.compat.emi.CrucibleCraftEmiPlugin";

    private PlayerCompleteSmoke() {}

    private static String configuredCapability() {
        return System.getProperty(
                "cruciblecraft.playerCapability",
                FLUID_CAPABILITY);
    }

    private static Surface surface(String capability) {
        if (ITEM_CAPABILITY.equals(capability)) {
            return new Surface(
                    ITEM_CAPABILITY,
                    List.of(
                            "cruciblecraft:logistics_item_storage_cover",
                            "cruciblecraft:logistics_item_import_cover",
                            "cruciblecraft:logistics_item_export_cover"),
                    List.of(
                            ItemNetworkKinds.STORAGE,
                            ItemNetworkKinds.IMPORT,
                            ItemNetworkKinds.EXPORT));
        }
        if (FLUID_CAPABILITY.equals(capability)) {
            return new Surface(
                    FLUID_CAPABILITY,
                    List.of(
                            "cruciblecraft:logistics_fluid_storage_cover",
                            "cruciblecraft:logistics_fluid_import_cover",
                            "cruciblecraft:logistics_fluid_export_cover"),
                    List.of(
                            FluidNetworkKinds.STORAGE,
                            FluidNetworkKinds.IMPORT,
                            FluidNetworkKinds.EXPORT));
        }
        if (GENERIC_CAPABILITY.equals(capability)) {
            return new Surface(
                    GENERIC_CAPABILITY,
                    List.of(
                            "cruciblecraft:logistics_generic_storage_cover",
                            "cruciblecraft:logistics_generic_import_cover",
                            "cruciblecraft:logistics_generic_export_cover"),
                    List.of(
                            GenericNetworkKinds.STORAGE,
                            GenericNetworkKinds.IMPORT,
                            GenericNetworkKinds.EXPORT));
        }
        if (CORE_CAPABILITY.equals(capability)) {
            return new Surface(
                    CORE_CAPABILITY,
                    List.of("cruciblecraft:logistics_generic_dump_cover"),
                    List.of(LogisticsDumpKinds.DUMP));
        }
        if (DISPLAY_CAPABILITY.equals(capability)) {
            return new Surface(
                    DISPLAY_CAPABILITY,
                    List.of(
                            "cruciblecraft:logistics_display_cpu_logic_cover",
                            "cruciblecraft:logistics_display_cpu_control_cover",
                            "cruciblecraft:logistics_display_cpu_storage_cover",
                            "cruciblecraft:logistics_display_cpu_conversion_cover"),
                    List.of(
                            DisplayCpuKinds.LOGIC,
                            DisplayCpuKinds.CONTROL,
                            DisplayCpuKinds.STORAGE,
                            DisplayCpuKinds.CONVERSION));
        }
        if (CONVERTER_CAPABILITY.equals(capability)) {
            return new Surface(
                    CONVERTER_CAPABILITY,
                    List.of(
                            "cruciblecraft:bronze_burning_box_solid",
                            "cruciblecraft:bronze_burning_box_gas",
                            "cruciblecraft:bronze_boiler",
                            "cruciblecraft:bronze_steam_engine",
                            "cruciblecraft:bronze_fuel_engine",
                            "cruciblecraft:bronze_dynamo",
                            "cruciblecraft:steel_galvanized_electric_motor"),
                    List.of());
        }
        if (BATTERIES_CAPABILITY.equals(capability)) {
            return new Surface(
                    BATTERIES_CAPABILITY,
                    List.of(
                            "cruciblecraft:lead_acid_battery_ulv",
                            "cruciblecraft:alkaline_battery_lv",
                            "cruciblecraft:nickel_cadmium_battery_mv",
                            "cruciblecraft:lithium_cobalt_battery_hv",
                            "cruciblecraft:lithium_manganese_battery_ev",
                            "cruciblecraft:red_energium_crystal_ulv",
                            "cruciblecraft:cyan_energium_crystal_iv"),
                    List.of());
        }
        if (TRANSFORMERS_CAPABILITY.equals(capability)) {
            return new Surface(
                    TRANSFORMERS_CAPABILITY,
                    List.of(
                            "cruciblecraft:electric_transformer_ulv_lv",
                            "cruciblecraft:electric_transformer_lv_mv",
                            "cruciblecraft:electric_transformer_mv_hv",
                            "cruciblecraft:electric_transformer_hv_ev",
                            "cruciblecraft:electric_transformer_ev_iv",
                            "cruciblecraft:electric_transformer_iv_luv",
                            "cruciblecraft:electric_transformer_luv_zpm",
                            "cruciblecraft:electric_transformer_zpm_uv",
                            "cruciblecraft:electric_transformer_uv_puv1"),
                    List.of());
        }
        return new Surface(capability, List.of(), List.of());
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
