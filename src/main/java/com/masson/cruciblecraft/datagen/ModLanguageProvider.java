package com.masson.cruciblecraft.datagen;

import java.util.Locale;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    private final boolean chinese;

    public ModLanguageProvider(PackOutput output) {
        this(output, "en_us");
    }

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, CrucibleCraft.MODID, locale);
        this.chinese = locale.equals("zh_cn");
    }

    @Override
    protected void addTranslations() {
        if (chinese) {
            ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                    addItem(ModItems.extruderShape(shape.id()), shape.chineseName()));
            return;
        }
        add("itemGroup.cruciblecraft", "Crucible Craft");
        add("itemGroup.cruciblecraft.ores", "Crucible Craft: Ores");
        add("itemGroup.cruciblecraft.ore_processing", "Crucible Craft: Ore Processing");
        add("itemGroup.cruciblecraft.dusts", "Crucible Craft: Dusts");
        add("itemGroup.cruciblecraft.metals_gems", "Crucible Craft: Metals & Gems");
        add("itemGroup.cruciblecraft.plates", "Crucible Craft: Plates");
        add("itemGroup.cruciblecraft.parts", "Crucible Craft: Parts");
        add("itemGroup.cruciblecraft.mechanical_parts", "Crucible Craft: Mechanical Parts");
        add("itemGroup.cruciblecraft.wires", "Crucible Craft: Wires");
        add("itemGroup.cruciblecraft.cables", "Crucible Craft: Cables");
        add("itemGroup.cruciblecraft.misc", "Crucible Craft: Miscellaneous Materials");
        addBlock(ModBlocks.FIREBRICK, "Firebrick");
        addBlock(ModBlocks.FIREBOX, "Solid Fuel Firebox");
        addBlock(ModBlocks.CRUCIBLE, "Crucible");
        addBlock(ModBlocks.ANVIL, "Smithing Anvil");
        addBlock(ModBlocks.COKE_OVEN, "Coke Oven Controller");
        addItem(ModItems.RAW_CERAMIC_CRUCIBLE, "Unfired Ceramic Crucible");
        addItem(ModItems.RAW_CERAMIC_MOLD, "Unshaped Unfired Ceramic Mold");
        addItem(ModItems.RAW_INGOT_MOLD, "Unfired Ingot Mold");
        addItem(ModItems.RAW_PLATE_MOLD, "Unfired Plate Mold");
        addItem(ModItems.RAW_ROD_MOLD, "Unfired Rod Mold");
        addItem(ModItems.RAW_BOLT_MOLD, "Unfired Bolt Mold");
        addItem(ModItems.INGOT_MOLD, "Ingot Mold");
        addItem(ModItems.PLATE_MOLD, "Plate Mold");
        addItem(ModItems.ROD_MOLD, "Rod Mold");
        addItem(ModItems.BOLT_MOLD, "Bolt Mold");
        addItem(ModItems.COAL_COKE, "Coal Coke");
        addBlock(ModBlocks.BELLOWS, "Bellows");
        addItem(ModItems.CREOSOTE_BUCKET, "Creosote Bucket");
        add("fluid_type.cruciblecraft.creosote", "Creosote");
        addItem(ModItems.STEAM_BUCKET, "Steam Bucket");
        add("fluid_type.cruciblecraft.steam", "Steam");
        addBlock(ModBlocks.BRONZE_BOILER, "Bronze Boiler");
        addBlock(ModBlocks.BRONZE_STEAM_ENGINE, "Bronze Steam Engine");
        addBlock(ModBlocks.BRONZE_CRUSHER, "Bronze Crusher");
        addBlock(ModBlocks.SLUICE, "Sluice");
        addBlock(ModBlocks.BATH, "Ore Washing Bath");
        addBlock(ModBlocks.CENTRIFUGE, "Centrifuge");
        addBlock(ModBlocks.SHREDDER, "Shredder");
        addBlock(ModBlocks.SIFTER, "Sifter");
        addBlock(ModBlocks.SMELTER, "Smelter");
        addBlock(ModBlocks.MORTAR, "Powered Mortar");
        addBlock(ModBlocks.EXTRUDER, "Extruder");
        addBlock(ModBlocks.CUTTER, "Cutter");
        addBlock(ModBlocks.LATHE, "Lathe");
        addBlock(ModBlocks.ROLLINGMILL, "Rolling Mill");
        addBlock(ModBlocks.ROLLBENDER, "Roll Bender");
        addBlock(ModBlocks.WIREMILL, "Wire Mill");
        addBlock(ModBlocks.BENDER, "Bender");
        addBlock(ModBlocks.ASSEMBLER, "Assembler");
        addBlock(ModBlocks.WELDER, "Welder");
        addBlock(ModBlocks.PRESS, "Press");
        add("screen.cruciblecraft.processing.status.idle", "Idle");
        add("screen.cruciblecraft.processing.status.running", "Running");
        add("screen.cruciblecraft.processing.status.invalid_recipe", "Invalid recipe");
        add("screen.cruciblecraft.processing.status.output_blocked", "Output blocked");
        add("screen.cruciblecraft.processing.status.underpowered", "Underpowered");
        add("screen.cruciblecraft.processing.tank", "%s/%s mB");
        add("container.cruciblecraft.bronze_crusher", "Bronze Crusher");
        add("emi.category.cruciblecraft.crusher", "Crusher");
        add("jade.cruciblecraft.boiler", "Water: %s/%s mB, Steam: %s/%s mB, Heat: %s/80 HU");
        add("jade.cruciblecraft.steam_engine", "Steam: %s/%s mB, KU: %s/%s (%s stroke)");
        add("jade.cruciblecraft.crusher", "Power: %s KU/t, Progress: %s/%s (%s)");
        addItem(ModItems.SMITHING_HAMMER, "Smithing Hammer");
        addItem(ModItems.UNKNOWN_MATERIAL, "Unknown Material");
        ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                addItem(ModItems.extruderShape(shape.id()), shape.englishName()));
        ModBlocks.oreBlockPaths().keySet().forEach(key -> addBlock(
                ModBlocks.oreBlock(key.materialId(), key.host()),
                (key.host() == Host.DEEPSLATE ? "Deepslate " : "")
                        + title(key.materialId()) + " Ore"));
        add("tooltip.cruciblecraft.unknown_material", "Missing material: %s (%s)");
        add("tooltip.cruciblecraft.machine_material", "Casing: %s (material tier %s)");
        add("tooltip.cruciblecraft.tool_material", "Head: %s (material tier %s)");
        add("tooltip.cruciblecraft.durability", "Durability: %s / %s");
        add("tooltip.cruciblecraft.max_temperature", "Maximum temperature: %s °C");
        add("message.cruciblecraft.materials_changed", "CrucibleCraft's material definitions changed since this world was last opened. Missing materials are preserved but machines using them are paused.");
        add(
                "disconnect.cruciblecraft.material_mismatch",
                "CrucibleCraft material definitions do not match the server: %s. Install the same addons and material definitions as the server.");
        add(
                "disconnect.cruciblecraft.material_handshake_missing",
                "The client cannot perform CrucibleCraft's material compatibility check. Install the same CrucibleCraft version as the server.");
        add(
                "disconnect.cruciblecraft.material_handshake_too_large",
                "The server's CrucibleCraft material configuration has %s entries, exceeding the supported limit of %s.");

        for (var form : MaterialPrefixCatalog.values()) {
            add(
                    "item.cruciblecraft.material_form." + form.serializedName(),
                    "%s " + title(form.serializedName()));
        }
        MaterialCatalog.startupValues().forEach(material ->
                add(material.translationKey(), title(material.id())));
        if (!MaterialCatalog.contains("stone")) {
            add("material.cruciblecraft.stone", "Stone");
        }

        add("message.cruciblecraft.firebox_fueled", "Firebox fueled: %s HU stored (%s seconds remaining)");
        add("message.cruciblecraft.firebox_fuel_rejected", "The firebox cannot accept this fuel right now");
        add("message.cruciblecraft.air_injection_started", "Airflow started; decarburization is underway");
        add("message.cruciblecraft.air_injection_continued", "Airflow duration extended");
        add("message.cruciblecraft.air_injection_too_cold", "The iron charge must be molten before blowing air");
        add("message.cruciblecraft.invalid_steel_charge", "Steelmaking requires exactly three parts iron to one part carbon");
        add("message.cruciblecraft.bellows_started", "Bellows stroke started");
        add("message.cruciblecraft.bellows_active", "The bellows are already moving");
        add("message.cruciblecraft.coke_oven_ignited", "Coke oven ignited");
        add("message.cruciblecraft.coke_oven_cannot_ignite", "The structure must be complete and heated from below");
        add("container.cruciblecraft.coke_oven", "Coke Oven");
        add("screen.cruciblecraft.coke_oven.invalid_structure", "Invalid structure");
        add("screen.cruciblecraft.coke_oven.no_heat", "No heat");
        add("screen.cruciblecraft.coke_oven.creosote", "Creosote: %s / %s mB");
        add("jade.cruciblecraft.coke_oven.structure", "Structure: %s");
        add("jade.cruciblecraft.coke_oven.valid", "Valid");
        add("jade.cruciblecraft.coke_oven.invalid", "Invalid");
        add("jade.cruciblecraft.coke_oven.progress", "Progress: %s / %s ticks");
        add("jade.cruciblecraft.coke_oven.creosote", "Creosote: %s / %s mB");
        add("emi.category.cruciblecraft.coke_oven", "Coke Oven");
        add("message.cruciblecraft.material_inserted", "Material added to crucible");
        add("message.cruciblecraft.crucible_full", "Crucible is full");
        add("message.cruciblecraft.crucible_tier_too_low",
                "This crucible casing cannot process that material tier");
        add("message.cruciblecraft.inexact_material_amount",
                "That amount cannot be decomposed into whole material units");
        add("message.cruciblecraft.invalid_material",
                "This material cannot be added to the crucible");
        add("message.cruciblecraft.material_cast", "Cast one %s ingot");
        add("message.cruciblecraft.not_ready", "Composition is not castable or is below melting temperature");
        add("message.cruciblecraft.mold_filled", "Molten material poured into the mold");
        add("message.cruciblecraft.mold_cooling", "Mold is cooling: %s °C");
        add("message.cruciblecraft.mold_no_molten_material", "No adjacent crucible has suitable molten material");
        add(
                "message.cruciblecraft.crucible_status",
                "Temperature: %s °C | %s");
        add("tooltip.cruciblecraft.temperature", "Temperature: %s %s");
        add("jade.cruciblecraft.temperature", "Temperature: %s %s");
        add("jade.cruciblecraft.firebox_heat", "Heat: %s HU (%s seconds remaining)");
        add("jade.cruciblecraft.firebox_output", "Output: %s HU/t");
        add("jade.cruciblecraft.casing", "Casing: %s (tier %s, process tier %s)");
        add("jade.cruciblecraft.machine_material", "Material: %s (tier %s)");
        add("jade.cruciblecraft.max_temperature", "Maximum temperature: %s %s");
        add("jade.cruciblecraft.contents", "%s (%s/%s u)");
        add("jade.cruciblecraft.anvil_workpiece", "Workpiece: %s");
        add("jade.cruciblecraft.anvil_slot", "Slot %s: %sx %s");
        add("jade.cruciblecraft.anvil_durability", "Durability: %s / %s");
        add("jade.cruciblecraft.anvil_progress", "Hammer strikes: %s");
        add("jade.cruciblecraft.mold_shape", "Mold: %s");
        add("jade.cruciblecraft.mold_contents", "Contents: %sx %s");
        add("jade.cruciblecraft.mold_state", "State: %s at %s °C");
        add("jade.cruciblecraft.mold_empty", "Empty");
        add("jade.cruciblecraft.mold_solid", "Solidified");
        add("jade.cruciblecraft.mold_cooling", "Cooling");
        add("message.cruciblecraft.anvil_inserted", "Workpiece placed on the anvil");
        add("message.cruciblecraft.anvil_rejected", "This item has no matching anvil recipe");
        add("message.cruciblecraft.anvil_no_recipe", "Place a valid workpiece on the anvil first");
        add("message.cruciblecraft.anvil_progress", "Hammering: %s/%s");
        add("message.cruciblecraft.anvil_completed", "Forged %s");
        add("emi.category.cruciblecraft.crucible", "Crucible Alloying");
        add("emi.category.cruciblecraft.anvil", "Anvil Working");
        add("emi.category.cruciblecraft.mold_casting", "Ceramic Mold Casting");

        add("cruciblecraft.configuration.title", "Crucible Craft");
        add("cruciblecraft.configuration.section.cruciblecraft.client.toml", "Client");
        add("cruciblecraft.configuration.section.cruciblecraft.client.toml.title", "Client");
        add("cruciblecraft.configuration.temperatureUnit", "Temperature Unit");
    }

    private static String title(String value) {
        String spaced = value.replace('_', ' ');
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }
}
