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
    RAW_ORES("raw_ores"),
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
        String path = prefixPath(prefix);
        if (path.startsWith("tool_head")) {
            return MISC;
        }
        return switch (path) {
            case "ore" -> ORES;
            case "raw_ore", "rock" -> RAW_ORES;
            case "crushed_ore",
                    "tiny_crushed_ore",
                    "washed_crushed_ore",
                    "centrifuged_crushed_ore",
                    "tiny_centrifuged_crushed_ore",
                    "tiny_washed_crushed_ore",
                    "purified_dust" -> ORE_PROCESSING;
            case "dust",
                    "small_dust",
                    "tiny_dust",
                    "dust_div72",
                    "storage_dust" -> DUSTS;
            case "block",
                    "machine_casing",
                    "machine_casing_double",
                    "machine_casing_quadruple",
                    "machine_casing_dense",
                    "small_casing",
                    "ingot",
                    "double_ingot",
                    "triple_ingot",
                    "ingot_hot",
                    "nugget",
                    "billet",
                    "chunk",
                    "storage_ingot",
                    "boule",
                    "gem",
                    "gem_exquisite",
                    "gem_flawless",
                    "gem_flawed",
                    "gem_chipped",
                    "gem_legendary",
                    "quadruple_ingot",
                    "quintuple_ingot" -> METALS_GEMS;
            case "plate",
                    "plate_gem",
                    "curved_plate",
                    "tiny_plate",
                    "tiny_plate_gem",
                    "foil",
                    "double_plate",
                    "triple_plate",
                    "quadruple_plate",
                    "quintuple_plate",
                    "dense_plate",
                    "storage_plate" -> PLATES;
            case "rod",
                    "long_rod",
                    "bolt",
                    "screw",
                    "ring",
                    "capcellcon",
                    "chain",
                    "round",
                    "lens" -> PARTS;
            case "spring",
                    "small_spring",
                    "gear",
                    "small_gear",
                    "rotor",
                    "minecart_wheels" -> MECHANICAL_PARTS;
            // Placeable conductors share one page (bare wire + cable).
            case "wire",
                    "cable",
                    "double_cable",
                    "quadruple_cable",
                    "octuple_cable",
                    "dodecuple_cable" -> CABLES;
            default -> {
                if ("fine_wire".equals(path) || path.endsWith("_wire")) {
                    yield WIRES;
                }
                if (path.endsWith("_pipe")) {
                    yield PIPES;
                }
                yield MISC;
            }
        };
    }

    public static boolean isToolHeadPrefix(MaterialPrefix prefix) {
        String path = prefixPath(prefix);
        if (path.startsWith("tool_head")) {
            return true;
        }
        try {
            return prefix.serializedName().startsWith("tool_head");
        } catch (RuntimeException ignored) {
            return false;
        }
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

    private static String prefixPath(MaterialPrefix prefix) {
        String id = prefix.id();
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
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
