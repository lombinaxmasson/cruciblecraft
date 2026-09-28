package com.masson.cruciblecraft.gui;

import java.util.Locale;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * Maps CC machine / screen ids to GT6-ported GUI textures under
 * {@code textures/gui/machines/}. Filenames are lowercase — ResourceLocation
 * paths cannot contain uppercase letters.
 */
public final class MachineGuiTextures {
    private static final ResourceLocation FALLBACK = texture("crafting");

    /** CC registry path (or logical alias) → lowercase GT6 GUI stem. */
    private static final Map<String, String> BY_PATH = Map.ofEntries(
            Map.entry("coke_oven", "cokeoven"),
            Map.entry("bronze_crusher", "crusher"),
            Map.entry("crusher", "crusher"),
            Map.entry("sluice", "sluice"),
            Map.entry("bath", "bath"),
            Map.entry("centrifuge", "centrifuge"),
            Map.entry("steel_centrifuge", "centrifuge"),
            Map.entry("titanium_centrifuge", "centrifuge"),
            Map.entry("large_centrifuge", "centrifuge"),
            Map.entry("large_mixer", "mixer"),
            Map.entry("large_electrolyzer", "electrolyzer"),
            Map.entry("large_oven", "oven"),
            Map.entry("large_crusher", "crusher"),
            Map.entry("large_shredder", "shredder"),
            Map.entry("large_sluice", "sluice"),
            Map.entry("large_squeezer", "squeezer"),
            Map.entry("large_bath", "bath"),
            Map.entry("coagulator", "coagulator"),
            Map.entry("large_coagulator", "coagulator"),
            Map.entry("shredder", "shredder"),
            Map.entry("steel_shredder", "shredder"),
            Map.entry("titanium_shredder", "shredder"),
            Map.entry("sifter", "sifter"),
            Map.entry("steel_sifter", "sifter"),
            Map.entry("titanium_sifter", "sifter"),
            Map.entry("slicer", "slicer"),
            Map.entry("aluminium_slicer", "slicer"),
            Map.entry("stainless_steel_slicer", "slicer"),
            Map.entry("chromium_slicer", "slicer"),
            Map.entry("titanium_slicer", "slicer"),
            Map.entry("laminator", "laminator"),
            Map.entry("invar_laminator", "laminator"),
            Map.entry("titanium_laminator", "laminator"),
            Map.entry("tungsten_carbide_laminator", "laminator"),
            Map.entry("pressurewasher", "pressurewasher"),
            Map.entry("steel_pressurewasher", "pressurewasher"),
            Map.entry("titanium_pressurewasher", "pressurewasher"),
            Map.entry("tungstensteel_pressurewasher", "pressurewasher"),
            Map.entry("loom", "loom"),
            Map.entry("steel_loom", "loom"),
            Map.entry("titanium_loom", "loom"),
            Map.entry("tungstensteel_loom", "loom"),
            Map.entry("electricloom", "electricloom"),
            Map.entry("aluminium_electricloom", "electricloom"),
            Map.entry("stainless_steel_electricloom", "electricloom"),
            Map.entry("chromium_electricloom", "electricloom"),
            Map.entry("titanium_electricloom", "electricloom"),
            Map.entry("injector", "injector"),
            Map.entry("aluminium_injector", "injector"),
            Map.entry("stainless_steel_injector", "injector"),
            Map.entry("chromium_injector", "injector"),
            Map.entry("titanium_injector", "injector"),
            Map.entry("nanofab", "nanofab"),
            Map.entry("aluminium_nanofab", "nanofab"),
            Map.entry("stainless_steel_nanofab", "nanofab"),
            Map.entry("chromium_nanofab", "nanofab"),
            Map.entry("titanium_nanofab", "nanofab"),
            Map.entry("smelter", "smelter"),
            Map.entry("invar_smelter", "smelter"),
            Map.entry("titanium_smelter", "smelter"),
            Map.entry("melter", "melter"),
            Map.entry("mortar", "mortar"),
            Map.entry("extruder", "extruder"),
            Map.entry("cutter", "cutter"),
            Map.entry("lathe", "lathe"),
            Map.entry("steel_lathe", "lathe"),
            Map.entry("titanium_lathe", "lathe"),
            Map.entry("rollingmill", "rollingmill"),
            Map.entry("steel_rollingmill", "rollingmill"),
            Map.entry("titanium_rollingmill", "rollingmill"),
            Map.entry("rollformer", "rollformer"),
            Map.entry("steel_rollformer", "rollformer"),
            Map.entry("titanium_rollformer", "rollformer"),
            Map.entry("tungstensteel_rollformer", "rollformer"),
            Map.entry("sanding", "sanding"),
            Map.entry("steel_sanding", "sanding"),
            Map.entry("titanium_sanding", "sanding"),
            Map.entry("tungstensteel_sanding", "sanding"),
            Map.entry("oven", "oven"),
            Map.entry("invar_oven", "oven"),
            Map.entry("titanium_oven", "oven"),
            Map.entry("tungsten_carbide_oven", "oven"),
            Map.entry("clustermill", "clustermill"),
            Map.entry("steel_clustermill", "clustermill"),
            Map.entry("titanium_clustermill", "clustermill"),
            Map.entry("tungstensteel_clustermill", "clustermill"),
            Map.entry("rollbender", "rollbender"),
            Map.entry("bender", "rollbender"),
            Map.entry("wiremill", "wiremill"),
            Map.entry("steel_wiremill", "wiremill"),
            Map.entry("titanium_wiremill", "wiremill"),
            Map.entry("assembler", "crafting"),
            Map.entry("laser_welder", "welder"),
            Map.entry("aluminium_laser_welder", "welder"),
            Map.entry("stainless_steel_laser_welder", "welder"),
            Map.entry("chromium_laser_welder", "welder"),
            Map.entry("titanium_laser_welder", "welder"),
            Map.entry("squeezer", "squeezer"),
            Map.entry("steel_squeezer", "squeezer"),
            Map.entry("titanium_squeezer", "squeezer"),
            Map.entry("tungstensteel_squeezer", "squeezer"),
            Map.entry("laser_engraver", "laserengraver"),
            Map.entry("aluminium_laser_engraver", "laserengraver"),
            Map.entry("stainless_steel_laser_engraver", "laserengraver"),
            Map.entry("chromium_laser_engraver", "laserengraver"),
            Map.entry("titanium_laser_engraver", "laserengraver"),
            Map.entry("press", "press"),
            Map.entry("steel_press", "press"),
            Map.entry("titanium_press", "press"),
            Map.entry("electrolyzer", "electrolyzer"),
            Map.entry("aluminium_electrolyzer", "electrolyzer"),
            Map.entry("stainless_steel_electrolyzer", "electrolyzer"),
            Map.entry("mixer", "mixer"),
            Map.entry("electric_mixer", "mixer"),
            Map.entry("cryo_mixer", "cryomixer"),
            Map.entry("distillery", "distillery"),
            Map.entry("invar_distillery", "distillery"),
            Map.entry("titanium_distillery", "distillery"),
            Map.entry("distillation_tower", "distillationtower"),
            Map.entry("cryo_distillation_tower", "cryodistillationtower"),
            Map.entry("autoclave", "autoclave"),
            Map.entry("large_autoclave", "autoclave"),
            Map.entry("implosion_compressor", "implosioncompressor"),
            Map.entry("large_fermenter", "fermenter"),
            Map.entry("fermenter", "fermenter"),
            Map.entry("drying", "dryer"),
            Map.entry("invar_drying", "dryer"),
            Map.entry("titanium_drying", "dryer"),
            Map.entry("compressor", "compressor"),
            Map.entry("generifier", "generifier"));

    private MachineGuiTextures() {}

    public static ResourceLocation forMachine(ResourceLocation machineId) {
        if (machineId == null) {
            return FALLBACK;
        }
        return forPath(machineId.getPath());
    }

    public static ResourceLocation forPath(String path) {
        if (path == null || path.isEmpty()) {
            return FALLBACK;
        }
        String key = path.toLowerCase(Locale.ROOT);
        String stem = BY_PATH.get(key);
        if (stem == null && (key.equals("cryo_mixer") || key.endsWith("_cryo_mixer"))) {
            stem = "cryomixer";
        }
        if (stem == null && (key.equals("burn_mixer") || key.endsWith("_burn_mixer"))) {
            stem = "burnmixer";
        }
        if (stem == null
                && (key.equals("catalytic_cracker") || key.endsWith("_catalytic_cracker"))) {
            stem = "catalyticcracking";
        }
        if (stem == null
                && (key.equals("crystallisation_crucible")
                        || key.endsWith("_crystallisation_crucible"))) {
            stem = "crystallisationcrucible";
        }
        if (stem == null
                && (key.equals("steam_cracker") || key.endsWith("_steam_cracker"))) {
            stem = "steamcracking";
        }
        if (stem == null && (key.equals("mixer") || key.endsWith("_mixer"))) {
            stem = "mixer";
        }
        if (stem == null) {
            stem = key.replace("_", "");
        }
        return texture(stem);
    }

    private static ResourceLocation texture(String stem) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "textures/gui/machines/"
                        + stem.toLowerCase(Locale.ROOT)
                        + ".png");
    }
}
