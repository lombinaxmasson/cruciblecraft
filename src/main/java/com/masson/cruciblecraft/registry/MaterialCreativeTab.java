package com.masson.cruciblecraft.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/** Semantic, mutually exclusive grouping for material creative-tab entries. */
public enum MaterialCreativeTab {
    ORES("ores"),
    ORE_PROCESSING("ore_processing"),
    DUSTS("dusts"),
    METALS_GEMS("metals_gems"),
    PLATES("plates"),
    PARTS("parts"),
    MECHANICAL_PARTS("mechanical_parts"),
    WIRES("wires"),
    CABLES("cables"),
    PIPES("pipes"),
    MISC("misc");

    private final String registryName;

    MaterialCreativeTab(String registryName) {
        this.registryName = registryName;
    }

    public String registryName() {
        return registryName;
    }

    public String translationKey() {
        return "itemGroup." + CrucibleCraft.MODID + "." + registryName;
    }

    public static MaterialCreativeTab forPrefix(MaterialPrefix prefix) {
        return switch (prefix.id()) {
            case "cruciblecraft:ore" -> ORES;
            case "cruciblecraft:raw_ore",
                    "cruciblecraft:crushed_ore",
                    "cruciblecraft:tiny_crushed_ore",
                    "cruciblecraft:washed_crushed_ore",
                    "cruciblecraft:centrifuged_crushed_ore",
                    "cruciblecraft:tiny_centrifuged_crushed_ore",
                    "cruciblecraft:tiny_washed_crushed_ore",
                    "cruciblecraft:purified_dust",
                    "cruciblecraft:rock" -> ORE_PROCESSING;
            case "cruciblecraft:dust",
                    "cruciblecraft:small_dust",
                    "cruciblecraft:tiny_dust",
                    "cruciblecraft:dust_div72" -> DUSTS;
            case "cruciblecraft:block",
                    "cruciblecraft:machine_casing",
                    "cruciblecraft:machine_casing_double",
                    "cruciblecraft:machine_casing_dense",
                    "cruciblecraft:ingot",
                    "cruciblecraft:double_ingot",
                    "cruciblecraft:triple_ingot",
                    "cruciblecraft:ingot_hot",
                    "cruciblecraft:nugget",
                    "cruciblecraft:gem",
                    "cruciblecraft:gem_exquisite",
                    "cruciblecraft:gem_flawless",
                    "cruciblecraft:gem_flawed",
                    "cruciblecraft:gem_chipped",
                    "cruciblecraft:gem_legendary",
                    "cruciblecraft:quadruple_ingot",
                    "cruciblecraft:quintuple_ingot" -> METALS_GEMS;
            case "cruciblecraft:plate",
                    "cruciblecraft:plate_gem",
                    "cruciblecraft:curved_plate",
                    "cruciblecraft:tiny_plate",
                    "cruciblecraft:tiny_plate_gem",
                    "cruciblecraft:foil",
                    "cruciblecraft:double_plate",
                    "cruciblecraft:triple_plate",
                    "cruciblecraft:quadruple_plate",
                    "cruciblecraft:quintuple_plate",
                    "cruciblecraft:dense_plate" -> PLATES;
            case "cruciblecraft:rod",
                    "cruciblecraft:long_rod",
                    "cruciblecraft:bolt",
                    "cruciblecraft:screw",
                    "cruciblecraft:ring" -> PARTS;
            case "cruciblecraft:spring",
                    "cruciblecraft:small_spring",
                    "cruciblecraft:gear",
                    "cruciblecraft:small_gear",
                    "cruciblecraft:rotor" -> MECHANICAL_PARTS;
            case "cruciblecraft:fine_wire",
                    "cruciblecraft:double_wire",
                    "cruciblecraft:quadruple_wire",
                    "cruciblecraft:octuple_wire",
                    "cruciblecraft:dodecuple_wire",
                    "cruciblecraft:hexadecuple_wire" -> WIRES;
            // Placeable conductors share one page (wire + cable + multi-cable);
            // pipes get their own page.
            case "cruciblecraft:wire",
                    "cruciblecraft:cable",
                    "cruciblecraft:double_cable",
                    "cruciblecraft:quadruple_cable",
                    "cruciblecraft:octuple_cable",
                    "cruciblecraft:dodecuple_cable" -> CABLES;
            case "cruciblecraft:tiny_fluid_pipe",
                    "cruciblecraft:small_fluid_pipe",
                    "cruciblecraft:fluid_pipe",
                    "cruciblecraft:large_fluid_pipe",
                    "cruciblecraft:huge_fluid_pipe",
                    "cruciblecraft:item_pipe",
                    "cruciblecraft:large_item_pipe",
                    "cruciblecraft:huge_item_pipe" -> PIPES;
            default -> MISC;
        };
    }

    public static boolean isToolHeadPrefix(MaterialPrefix prefix) {
        return prefix.serializedName().startsWith("tool_head");
    }

    /**
     * Pure registry-id plan used to verify the same first-owner de-duplication
     * policy as the runtime tabs.
     */
    public static Map<MaterialCreativeTab, List<String>> planEntryIds(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        return planEntryIds(materials, registeredForms, Map.of());
    }

    public static Map<MaterialCreativeTab, List<String>> planEntryIds(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms,
            Map<String, String> unificationPreferences) {
        EnumMap<MaterialCreativeTab, List<String>> entries =
                new EnumMap<>(MaterialCreativeTab.class);
        for (MaterialCreativeTab tab : values()) {
            entries.put(tab, new ArrayList<>());
        }
        Set<String> claimed = new LinkedHashSet<>();

        for (MaterialDefinition material : materials) {
            List<MaterialPrefix> forms = requireForms(material, registeredForms);
            if (forms.contains(MaterialPrefixes.ORE)) {
                claim(
                        entries,
                        claimed,
                        ORES,
                        CrucibleCraft.MODID + ":" + material.id() + "_ore");
                claim(
                        entries,
                        claimed,
                        ORES,
                        CrucibleCraft.MODID + ":deepslate_" + material.id() + "_ore");
            }
        }
        for (MaterialDefinition material : materials) {
            for (MaterialPrefix prefix : requireForms(material, registeredForms)) {
                if (prefix.equals(MaterialPrefixes.ORE) || isToolHeadPrefix(prefix)) {
                    continue;
                }
                claim(
                        entries,
                        claimed,
                        forPrefix(prefix),
                        MaterialLookup.resolveItemId(
                                material,
                                prefix,
                                unificationPreferences).toString());
            }
        }

        EnumMap<MaterialCreativeTab, List<String>> frozen =
                new EnumMap<>(MaterialCreativeTab.class);
        entries.forEach((tab, ids) -> frozen.put(tab, List.copyOf(ids)));
        return Collections.unmodifiableMap(frozen);
    }

    public static List<String> toolHeadEntryIds(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms,
            Map<String, String> unificationPreferences) {
        LinkedHashSet<String> claimed = new LinkedHashSet<>();
        List<String> ids = new ArrayList<>();
        for (MaterialDefinition material : materials) {
            for (MaterialPrefix prefix : requireForms(material, registeredForms)) {
                if (!isToolHeadPrefix(prefix)) {
                    continue;
                }
                String itemId = MaterialLookup.resolveItemId(
                        material,
                        prefix,
                        unificationPreferences).toString();
                if (claimed.add(itemId)) {
                    ids.add(itemId);
                }
            }
        }
        return List.copyOf(ids);
    }

    private static List<MaterialPrefix> requireForms(
            MaterialDefinition material,
            Map<String, List<MaterialPrefix>> registeredForms) {
        List<MaterialPrefix> forms = registeredForms.get(material.id());
        if (forms == null) {
            throw new IllegalArgumentException(
                    "Missing registered forms for material " + material.id());
        }
        return forms;
    }

    private static void claim(
            Map<MaterialCreativeTab, List<String>> entries,
            Set<String> claimed,
            MaterialCreativeTab tab,
            String itemId) {
        if (claimed.add(itemId)) {
            entries.get(tab).add(itemId);
        }
    }
}
