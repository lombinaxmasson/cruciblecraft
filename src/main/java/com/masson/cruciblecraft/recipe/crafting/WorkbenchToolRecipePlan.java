package com.masson.cruciblecraft.recipe.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/**
 * GT6 workbench tool recipes projected once per eligible catalog material.
 *
 * <p>Workshop tools follow {@link ToolKind} (the same listener/prefix
 * intersection as assembler {@code ToolRules}). Simple rock tools follow
 * {@code Loader_Tools.java:279-285} {@code ANY.Stone} plus vanilla flint
 * {@code Loader_Tools.java:258-261}. Handles stay {@code minecraft:stick}
 * ({@code OD.stickAnyWood}); other handle materials are not invented.
 */
public final class WorkbenchToolRecipePlan {
    public static final String STONE_TAG = "PROPERTIES.STONE";
    public static final int NON_WORKBENCH_GENERATED_RECIPES = 1_744;
    private static final MaterialPrefix ROCK =
            new MaterialPrefix("cruciblecraft:rock");
    private static final String HAMMER_ITEM = "cruciblecraft:smithing_hammer";
    private static final String FILE_ITEM = "cruciblecraft:material_file";
    private static final String KNIFE_ITEM = "cruciblecraft:flint_knife";
    private static final String SCREWDRIVER_ITEM =
            "cruciblecraft:material_screwdriver";
    private static final String STICK = "minecraft:stick";
    private static final String FLINT = "minecraft:flint";

    private WorkbenchToolRecipePlan() {}

    public record Recipe(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material) {
        public JsonObject toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("type", "cruciblecraft:shaped_catalyst");
            if (!catalysts.isEmpty()) {
                root.add("catalysts", itemKeys(catalysts));
            }
            root.add("ingredients", itemKeys(ingredients));
            JsonArray patternJson = new JsonArray();
            pattern.forEach(patternJson::add);
            root.add("pattern", patternJson);
            JsonObject components = new JsonObject();
            components.addProperty("cruciblecraft:tool_material", material);
            JsonObject result = new JsonObject();
            result.add("components", components);
            result.addProperty("count", 1);
            result.addProperty("id", resultId);
            root.add("result", result);
            return root;
        }
    }

    public static List<Recipe> plan(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        LinkedHashMap<String, Recipe> byPath = new LinkedHashMap<>();
        materials.stream()
                .sorted(Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> recipesFor(
                        material, formsOf(material, registeredForms))
                        .forEach(recipe -> putUnique(byPath, recipe)));
        vanillaFlintHarvest().forEach(recipe -> putUnique(byPath, recipe));
        return List.copyOf(byPath.values());
    }

    private static void putUnique(
            LinkedHashMap<String, Recipe> byPath, Recipe recipe) {
        Recipe previous = byPath.put(recipe.path(), recipe);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate workbench tool path " + recipe.path()
                            + " (" + previous.material()
                            + " vs " + recipe.material() + ")");
        }
    }

    private static Set<MaterialPrefix> formsOf(
            MaterialDefinition material,
            Map<String, List<MaterialPrefix>> registeredForms) {
        List<MaterialPrefix> forms = registeredForms.get(material.id());
        if (forms == null) {
            return Set.of();
        }
        return Set.copyOf(forms);
    }

    private static List<Recipe> recipesFor(
            MaterialDefinition material, Set<MaterialPrefix> forms) {
        List<Recipe> recipes = new ArrayList<>();
        addWorkshop(recipes, material, forms);
        addSimpleRockTools(recipes, material, forms);
        return recipes;
    }

    private static void addWorkshop(
            List<Recipe> recipes,
            MaterialDefinition material,
            Set<MaterialPrefix> forms) {
        String id = material.id();
        if (eligible(material, ToolKind.FILE) && forms.contains(MaterialPrefixes.PLATE)) {
            recipes.add(recipe(
                    path(id, "file"),
                    pad3x3(" P ", " Pk"),
                    map("P", item(material, MaterialPrefixes.PLATE)),
                    map("k", KNIFE_ITEM),
                    "cruciblecraft:material_file",
                    id));
        }
        if (eligible(material, ToolKind.WRENCH)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(recipe(
                        path(id, "wrench"),
                        List.of("PhP", " P ", " P "),
                        map("P", item(material, MaterialPrefixes.PLATE)),
                        map("h", HAMMER_ITEM),
                        "cruciblecraft:material_wrench",
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(recipe(
                        path(id, "wrench"),
                        List.of("CfC", " C ", " C "),
                        map("C", item(material, MaterialPrefixes.GEM)),
                        map("f", FILE_ITEM),
                        "cruciblecraft:material_wrench",
                        id));
            }
        }
        if (eligible(material, ToolKind.SCREWDRIVER)
                && forms.contains(MaterialPrefixes.ROD)) {
            recipes.add(recipe(
                    path(id, "screwdriver"),
                    pad3x3("hS", "Sf"),
                    map("S", item(material, MaterialPrefixes.ROD)),
                    map("h", HAMMER_ITEM, "f", FILE_ITEM),
                    "cruciblecraft:material_screwdriver",
                    id));
        }
        if (eligible(material, ToolKind.SAW)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(recipe(
                        path(id, "saw"),
                        pad3x3("PP", "fh"),
                        map("P", item(material, MaterialPrefixes.PLATE)),
                        map("f", FILE_ITEM, "h", HAMMER_ITEM),
                        "cruciblecraft:material_saw",
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(recipe(
                        path(id, "saw"),
                        pad3x3("CC", "f "),
                        map("C", item(material, MaterialPrefixes.GEM)),
                        map("f", FILE_ITEM),
                        "cruciblecraft:material_saw",
                        id));
            }
        }
        if (eligible(material, ToolKind.CHISEL)
                && forms.contains(MaterialPrefixes.ROD)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(recipe(
                        path(id, "chisel"),
                        pad3x3("hPf", " S "),
                        map(
                                "P", item(material, MaterialPrefixes.PLATE),
                                "S", item(material, MaterialPrefixes.ROD)),
                        map("h", HAMMER_ITEM, "f", FILE_ITEM),
                        "cruciblecraft:material_chisel",
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(recipe(
                        path(id, "chisel"),
                        pad3x3("Cf", "S "),
                        map(
                                "C", item(material, MaterialPrefixes.GEM),
                                "S", item(material, MaterialPrefixes.ROD)),
                        map("f", FILE_ITEM),
                        "cruciblecraft:material_chisel",
                        id));
            }
        }
        if (eligible(material, ToolKind.WIRE_CUTTER)
                && forms.contains(MaterialPrefixes.ROD)
                && forms.contains(MaterialPrefixes.SCREW)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(recipe(
                        path(id, "wire_cutter"),
                        List.of("PfP", "hPd", "STS"),
                        map(
                                "P", item(material, MaterialPrefixes.PLATE),
                                "S", item(material, MaterialPrefixes.ROD),
                                "T", item(material, MaterialPrefixes.SCREW)),
                        map(
                                "f", FILE_ITEM,
                                "h", HAMMER_ITEM,
                                "d", SCREWDRIVER_ITEM),
                        "cruciblecraft:material_wire_cutter",
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(recipe(
                        path(id, "wire_cutter"),
                        List.of("CfC", "hCd", "STS"),
                        map(
                                "C", item(material, MaterialPrefixes.GEM),
                                "S", item(material, MaterialPrefixes.ROD),
                                "T", item(material, MaterialPrefixes.SCREW)),
                        map(
                                "f", FILE_ITEM,
                                "h", HAMMER_ITEM,
                                "d", SCREWDRIVER_ITEM),
                        "cruciblecraft:material_wire_cutter",
                        id));
            }
        }
        if (eligible(material, ToolKind.MONKEY_WRENCH)
                && forms.contains(MaterialPrefixes.SCREW)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(recipe(
                        path(id, "monkey_wrench"),
                        List.of("PPd", "hPT", " P "),
                        map(
                                "P", item(material, MaterialPrefixes.PLATE),
                                "T", item(material, MaterialPrefixes.SCREW)),
                        map("d", SCREWDRIVER_ITEM, "h", HAMMER_ITEM),
                        "cruciblecraft:material_monkey_wrench",
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(recipe(
                        path(id, "monkey_wrench"),
                        List.of("CCd", "fCT", " C "),
                        map(
                                "C", item(material, MaterialPrefixes.GEM),
                                "T", item(material, MaterialPrefixes.SCREW)),
                        map("d", SCREWDRIVER_ITEM, "f", FILE_ITEM),
                        "cruciblecraft:material_monkey_wrench",
                        id));
            }
        }
        if (eligible(material, ToolKind.SMITHING_HAMMER)) {
            if (forms.contains(MaterialPrefixes.INGOT)) {
                recipes.add(recipe(
                        path(id, "smithing_hammer"),
                        List.of("II ", "IIh", "II "),
                        map("I", item(material, MaterialPrefixes.INGOT)),
                        map("h", HAMMER_ITEM),
                        HAMMER_ITEM,
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(recipe(
                        path(id, "smithing_hammer"),
                        List.of("GG ", "GGf", "GG "),
                        map("G", item(material, MaterialPrefixes.GEM)),
                        map("f", FILE_ITEM),
                        HAMMER_ITEM,
                        id));
            }
        }
    }

    private static void addSimpleRockTools(
            List<Recipe> recipes,
            MaterialDefinition material,
            Set<MaterialPrefix> forms) {
        if (!forms.contains(ROCK) || !hasTag(material, STONE_TAG)) {
            return;
        }
        String id = material.id();
        String rock = item(material, ROCK);
        Map<String, String> headAndStick = map("X", rock, "S", STICK);
        if (eligible(material, ToolKind.AXE)) {
            recipes.add(recipe(
                    path(id, "axe"),
                    pad3x3("XX", "XS"),
                    headAndStick,
                    Map.of(),
                    "cruciblecraft:material_axe",
                    id));
        }
        if (eligible(material, ToolKind.HOE)) {
            recipes.add(recipe(
                    path(id, "hoe"),
                    pad3x3("XX", " S"),
                    headAndStick,
                    Map.of(),
                    "cruciblecraft:material_hoe",
                    id));
        }
        if (eligible(material, ToolKind.SHOVEL)) {
            recipes.add(recipe(
                    path(id, "shovel"),
                    pad3x3("X", "S"),
                    headAndStick,
                    Map.of(),
                    "cruciblecraft:material_shovel",
                    id));
        }
        if (eligible(material, ToolKind.PICKAXE)) {
            recipes.add(recipe(
                    path(id, "pickaxe"),
                    pad3x3("XXX", " S "),
                    headAndStick,
                    Map.of(),
                    "cruciblecraft:material_pickaxe",
                    id));
        }
        if (eligible(material, ToolKind.SMITHING_HAMMER)
                && recipes.stream().noneMatch(
                        recipe -> path(id, "smithing_hammer").equals(recipe.path()))) {
            recipes.add(recipe(
                    path(id, "smithing_hammer"),
                    List.of("XX ", "XXS", "XX "),
                    headAndStick,
                    Map.of(),
                    HAMMER_ITEM,
                    id));
        }
    }

    private static List<Recipe> vanillaFlintHarvest() {
        Map<String, String> flintAndStick = map("X", FLINT, "S", STICK);
        return List.of(
                recipe(
                        "tools/flint_pickaxe",
                        pad3x3("XXX", " S "),
                        flintAndStick,
                        Map.of(),
                        "cruciblecraft:material_pickaxe",
                        "flint"),
                recipe(
                        "tools/flint_axe",
                        pad3x3("XX", "XS"),
                        flintAndStick,
                        Map.of(),
                        "cruciblecraft:material_axe",
                        "flint"),
                recipe(
                        "tools/flint_shovel",
                        pad3x3("X", "S"),
                        flintAndStick,
                        Map.of(),
                        "cruciblecraft:material_shovel",
                        "flint"));
    }

    private static boolean eligible(MaterialDefinition material, ToolKind kind) {
        return material.gt6Metadata()
                .filter(metadata -> kind.isEligible(
                        material.id(),
                        metadata.tool(),
                        metadata.materialTags()))
                .isPresent();
    }

    private static boolean hasTag(MaterialDefinition material, String tag) {
        return material.gt6Metadata()
                .filter(metadata -> metadata.materialTags().contains(tag))
                .isPresent();
    }

    private static String item(MaterialDefinition material, MaterialPrefix form) {
        return MaterialLookup.resolveItemId(material, form, Map.of()).toString();
    }

    private static String path(String material, String tool) {
        return "tools/" + material + "/" + tool;
    }

    private static Recipe recipe(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material) {
        return new Recipe(
                path,
                List.copyOf(pattern),
                Map.copyOf(new LinkedHashMap<>(ingredients)),
                Map.copyOf(new LinkedHashMap<>(catalysts)),
                resultId,
                material);
    }

    private static JsonObject itemKeys(Map<String, String> keys) {
        JsonObject object = new JsonObject();
        keys.forEach((symbol, itemId) -> {
            JsonObject item = new JsonObject();
            item.addProperty("item", itemId);
            object.add(symbol, item);
        });
        return object;
    }

    private static Map<String, String> map(String k1, String v1) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(k1, v1);
        return values;
    }

    private static Map<String, String> map(
            String k1, String v1, String k2, String v2) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(k1, v1);
        values.put(k2, v2);
        return values;
    }

    private static Map<String, String> map(
            String k1, String v1, String k2, String v2, String k3, String v3) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(k1, v1);
        values.put(k2, v2);
        values.put(k3, v3);
        return values;
    }

    public static List<String> pad3x3(String... rows) {
        String[] padded = {"   ", "   ", "   "};
        for (int index = 0; index < rows.length && index < 3; index++) {
            String row = rows[index];
            if (row.length() > 3) {
                throw new IllegalArgumentException(
                        "pattern row wider than 3: " + row);
            }
            padded[index] = (row + "   ").substring(0, 3);
        }
        return List.of(padded);
    }
}
