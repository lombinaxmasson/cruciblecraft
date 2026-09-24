package com.masson.cruciblecraft.registry;

import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Old T44 {@code mass_storage_barrel_*} / {@code mass_storage_box_*} ids become
 * Item Barrel / Plastic Storage Box. Worlds keep the placed blocks.
 */
@EventBusSubscriber(modid = CrucibleCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class StorageRegistryAliases {
    private static final String[] BARRELS = {
            "6983", "6984", "6985", "6986", "6987", "6988", "6989",
            "6990", "6991", "6992", "6997", "6998", "6999"
    };
    private static final String[] BOXES = {"6993", "6994", "6995", "6996"};
    private static final Set<String> LEGACY_STORAGE_INGOT_MATERIALS = Set.of(
            "abyssalnite", "adamantium", "alumina", "aluminium", "aluminium_brass", "aluminium_fluoride",
            "alumite", "ancient_debris", "angmallen", "annealed_copper", "anthracite", "antimony",
            "ardite", "arsenic", "arsenic_bronze", "arsenic_copper", "astatine", "astral_silver",
            "atlarus", "barium", "battery_alloy", "beryllium", "beryllium7", "beryllium8",
            "bismuth", "bismuth_bronze", "black_bronze", "black_steel", "blue_alloy", "blue_steel",
            "boron", "boron11", "brass", "bronze", "caesium", "calcite",
            "calcium", "calcium_chloride", "cast_iron", "celenegil", "charcoal", "cheese",
            "chocolate", "chromium", "coal", "coal_coke", "cobalt", "cobalt_brass",
            "constantan", "copper", "coralium", "cryolite", "damascus_steel", "dark_thaumium",
            "desh", "draconium", "draconium_awakened", "dreadium", "efrine", "electrotine_alloy",
            "electrum", "enderium", "enderium_base", "ethaxium", "ferrous_chloride", "fiery_steel",
            "fireleaf", "flamascus_steel", "fluorite", "francium", "frozen_iron", "germanium",
            "gilded_iron", "glowstone_refined", "gold198", "gold_inductive", "hepatizon", "hslasteel",
            "hsse", "hssg", "hsss", "invar", "iridium", "iron",
            "iron_compressed", "iron_magnetic", "ironwood", "kanthal", "knightmetal", "kreknorite",
            "lead", "lithium", "lithium6", "lithium_chlorate", "lithium_chloride", "lithium_perchlorate",
            "lumium", "magnalium", "magnesium", "magnesium_carbonate", "magnesium_chloride", "manganese",
            "manganese_chloride", "manyullyn", "meteoflame_black_steel", "meteoflame_blue_steel", "meteoflame_red_steel", "meteoflame_steel",
            "meteoric_black_steel", "meteoric_blue_steel", "meteoric_iron", "meteoric_red_steel", "meteoric_steel", "meteorite",
            "midasium", "mithril", "molybdenum", "naquadah", "naquadah_enriched", "naquadria",
            "neodymium", "neodymium_magnetic", "netherite", "nichrome", "nickel", "nikoline_alloy",
            "obsidian_refined", "octine", "orichalcum", "osmiridium", "osmium_elemental", "pig_iron",
            "platinum", "potassium", "potassium_carbonate", "purple_alloy", "pyrolusite", "radium",
            "red_alloy", "red_steel", "redstone_alloy", "rose_gold", "rubber", "rubidium",
            "rutile", "signalum", "silicon", "silver", "sodium", "sodium_carbonate",
            "soldering_alloy", "spectre_iron", "stainless_steel", "steel", "steel_galvanized", "steel_magnetic",
            "steeleaf", "sterling_silver", "strontium", "syrmorite", "tellurium", "thaumium",
            "tin", "tin_alloy", "titanium", "titanium_gold", "trinium", "tungsten",
            "tungsten_sintered", "tungsten_trioxide", "tungstensteel", "ultimet", "vanadium", "vanadium_steel",
            "void_metal", "wax", "wax_amnesic", "wax_bee", "wax_magic", "wax_paraffin",
            "wax_plant", "wax_refractory", "wax_soulful", "workers_alloy", "wrought_iron", "zinc");

    private StorageRegistryAliases() {}

    @SubscribeEvent
    public static void addAliases(RegisterEvent event) {
        aliasRegistry(event.getRegistry(Registries.BLOCK));
        aliasRegistry(event.getRegistry(Registries.ITEM));
    }

    private static void aliasRegistry(Registry<?> registry) {
        if (registry == null) {
            return;
        }
        for (String meta : BARRELS) {
            registry.addAlias(
                    id("mass_storage_barrel_" + meta),
                    id("item_barrel_" + meta));
        }
        for (String meta : BOXES) {
            registry.addAlias(
                    id("mass_storage_box_" + meta),
                    id("plastic_storage_box_" + meta));
        }
        for (String material : LEGACY_STORAGE_INGOT_MATERIALS) {
            registry.addAlias(
                    id(material + "/storage_ingot"),
                    id(material + "/block"));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }
}
