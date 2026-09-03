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
import com.masson.cruciblecraft.logistics.fluidnet.FluidNetworkKinds;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.registry.ModCreativeTabs;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Shared player-complete smoke snapshot for GameTestServer and runClient. */
public final class PlayerCompleteSmoke {
    public static final List<String> REQUIRED_REGISTRY_IDS = List.of(
            "cruciblecraft:logistics_fluid_storage_cover",
            "cruciblecraft:logistics_fluid_import_cover",
            "cruciblecraft:logistics_fluid_export_cover");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EMI_PLUGIN =
            "com.masson.cruciblecraft.compat.emi.CrucibleCraftEmiPlugin";

    private PlayerCompleteSmoke() {}

    public static JsonObject snapshot(String runtime) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", 1);
        root.addProperty("capability", "logistics/fluid-network/basic-transfer");
        root.addProperty("runtime", runtime);
        root.addProperty(
                "run_nonce",
                System.getProperty("cruciblecraft.smokeNonce", ""));
        root.addProperty("emi_plugin_class", EMI_PLUGIN);
        root.addProperty("modid", CrucibleCraft.MODID);
        JsonArray required = new JsonArray();
        JsonArray observed = new JsonArray();
        boolean missing = false;
        for (String id : REQUIRED_REGISTRY_IDS) {
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
        boolean covers = CoverDefinitionCatalog.find(FluidNetworkKinds.STORAGE)
                .isPresent()
                && CoverDefinitionCatalog.find(FluidNetworkKinds.IMPORT)
                        .isPresent()
                && CoverDefinitionCatalog.find(FluidNetworkKinds.EXPORT)
                        .isPresent();
        root.addProperty("fluid_cover_definitions", covers);
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
        boolean pass = !missing && covers && tab && emiPlan;
        root.addProperty("status", pass ? "PASS" : "FAIL");
        return root;
    }

    public static void writeIfConfigured(String runtime) {
        String raw = System.getProperty("cruciblecraft.smokeReceipt");
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(raw);
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    GSON.toJson(snapshot(runtime)) + "\n",
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            CrucibleCraft.LOGGER.error(
                    "Could not write player-complete smoke receipt",
                    exception);
        }
    }
}
