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
 * <p>{@code Loader_Tools} {@code aUseNormalHandle=T} crafts a tool head, then
 * {@code AdvancedCraftingTool} shapeless-assembles the head with a wooden
 * stick. {@code aUseNormalHandle=F} (wrench, monkey wrench, wire cutter) and
 * simple rock/flint crafts stay finished tools. Handles stay
 * {@code minecraft:stick}; other handle materials are not invented.
 *
 * <p>GT6 {@code CR.DEF_MIR} rock/flint crafts set {@code mirrored}.
 * Flint harvest uses {@code OD.itemFlint}, not {@code OP.rockGt}.
 * Bone club uses {@code Items.bone}. Petrified-wood rock hammer is the
 * dedicated GT6 extra loop; {@code PROPERTIES.WOOD} would skip the
 * smithing-hammer listener.
 */
public final class WorkbenchToolRecipePlan {
    public static final String STONE_TAG = "PROPERTIES.STONE";
    // Includes the 28 MTE fluid-attachment cooking recipes, the static
    // tool-pattern resources emitted outside the runtime workbench plan,
    // and the bathing pot table crafting recipe.
    public static final int NON_WORKBENCH_GENERATED_RECIPES = 5_434;
    private static final MaterialPrefix ROCK =
            new MaterialPrefix("cruciblecraft:rock");
    private static final MaterialPrefix PLATE_GEM =
            new MaterialPrefix("cruciblecraft:plate_gem");
    private static final MaterialPrefix TOOL_HEAD_PICKAXE =
            new MaterialPrefix("cruciblecraft:tool_head_pickaxe");
    private static final MaterialPrefix TOOL_HEAD_SHOVEL =
            new MaterialPrefix("cruciblecraft:tool_head_shovel");
    private static final MaterialPrefix TOOL_HEAD_AXE =
            new MaterialPrefix("cruciblecraft:tool_head_axe");
    private static final MaterialPrefix TOOL_HEAD_HOE =
            new MaterialPrefix("cruciblecraft:tool_head_hoe");
    private static final MaterialPrefix TOOL_HEAD_SWORD =
            new MaterialPrefix("cruciblecraft:tool_head_sword");
    private static final MaterialPrefix TOOL_HEAD_FILE =
            new MaterialPrefix("cruciblecraft:tool_head_file");
    private static final MaterialPrefix TOOL_HEAD_CHISEL =
            new MaterialPrefix("cruciblecraft:tool_head_chisel");
    private static final MaterialPrefix TOOL_HEAD_SAW =
            new MaterialPrefix("cruciblecraft:tool_head_saw");
    private static final MaterialPrefix TOOL_HEAD_SCREWDRIVER =
            new MaterialPrefix("cruciblecraft:tool_head_screwdriver");
    private static final MaterialPrefix TOOL_HEAD_HAMMER =
            new MaterialPrefix("cruciblecraft:tool_head_hammer");
    private static final MaterialPrefix TOOL_HEAD_SPADE =
            new MaterialPrefix("cruciblecraft:tool_head_spade");
    private static final MaterialPrefix TOOL_HEAD_AXE_DOUBLE =
            new MaterialPrefix("cruciblecraft:tool_head_axe_double");
    private static final MaterialPrefix TOOL_HEAD_SENSE =
            new MaterialPrefix("cruciblecraft:tool_head_sense");
    private static final MaterialPrefix TOOL_HEAD_PLOW =
            new MaterialPrefix("cruciblecraft:tool_head_plow");
    private static final MaterialPrefix TOOL_HEAD_CONSTRUCTION =
            new MaterialPrefix("cruciblecraft:tool_head_construction_pickaxe");
    private static final MaterialPrefix TOOL_HEAD_BUILDERWAND =
            new MaterialPrefix("cruciblecraft:tool_head_builderwand");
    private static final MaterialPrefix TOOL_HEAD_ARROW =
            new MaterialPrefix("cruciblecraft:tool_head_arrow");
    private static final String HAMMER_ITEM = "cruciblecraft:smithing_hammer";
    private static final String FILE_ITEM = "cruciblecraft:material_file";
    private static final String KNIFE_ITEM = "cruciblecraft:material_knife";
    private static final String SCREWDRIVER_ITEM =
            "cruciblecraft:material_screwdriver";
    private static final String SAW_ITEM = "cruciblecraft:material_saw";
    private static final String WIRE_CUTTER_ITEM =
            "cruciblecraft:material_wire_cutter";
    private static final String SOFT_HAMMER_ITEM =
            "cruciblecraft:material_soft_hammer";
    private static final String RUBBER_PLATE = "cruciblecraft:rubber/plate";
    private static final String BLUE_DYE = "minecraft:blue_dye";
    private static final String WHITE_WOOL = "minecraft:white_wool";
    private static final Set<String> ROLLING_PIN_MATERIALS = Set.of(
            "gold",
            "aluminium",
            "chromium",
            "stainless_steel",
            "netherite",
            "syrmorite",
            "plastic");
    private static final Set<String> FLINT_TINDER_NUGGET_MATERIALS = Set.of(
            "iron",
            "steel",
            "gold",
            "netherite",
            "nickel",
            "thaumium");
    private static final String STICK = "minecraft:stick";
    private static final String FLINT = "minecraft:flint";
    private static final String BONE = "minecraft:bone";

    private WorkbenchToolRecipePlan() {}

    public record Recipe(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material,
            boolean persistToolMaterial,
            int count,
            boolean mirrored,
            long electricCapacity,
            long electricVoltage) {
        public JsonObject toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("type", "cruciblecraft:shaped_catalyst");
            if (mirrored) {
                root.addProperty("mirrored", true);
            }
            if (!catalysts.isEmpty()) {
                root.add("catalysts", toolKeys(catalysts));
            }
            root.add("ingredients", itemKeys(ingredients));
            JsonArray patternJson = new JsonArray();
            pattern.forEach(patternJson::add);
            root.add("pattern", patternJson);
            JsonObject result = new JsonObject();
            JsonObject components = new JsonObject();
            if (persistToolMaterial) {
                components.addProperty("cruciblecraft:tool_material", material);
            }
            if (electricCapacity > 0L) {
                components.addProperty("cruciblecraft:electric_charge", 0);
                components.addProperty(
                        "cruciblecraft:electric_capacity", electricCapacity);
                components.addProperty(
                        "cruciblecraft:electric_voltage", electricVoltage);
            }
            if (persistToolMaterial || electricCapacity > 0L) {
                result.add("components", components);
            }
            result.addProperty("count", count);
            result.addProperty("id", resultId);
            root.add("result", result);
            return root;
        }
    }

    public record Assembly(
            String path, String headPrefix, String resultId, ToolKind kind) {
        public JsonObject toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("type", "cruciblecraft:tool_head_assembly");
            root.addProperty("head_prefix", headPrefix);
            root.addProperty("result", resultId);
            return root;
        }
    }

    /**
     * One EMI/recipe-viewer row per eligible material. GT6 registered these
     * on {@code RM.ToolHeads}; the live crafting matcher stays one shapeless
     * recipe per tool kind.
     */
    public record AssemblyVariant(
            String path,
            String headLogicalId,
            String resultId,
            String material) {}

    public static List<Assembly> assemblies() {
        return List.of(
                assembly("pickaxe", "tool_head_pickaxe",
                        "cruciblecraft:material_pickaxe", ToolKind.PICKAXE),
                assembly("shovel", "tool_head_shovel",
                        "cruciblecraft:material_shovel", ToolKind.SHOVEL),
                assembly("axe", "tool_head_axe",
                        "cruciblecraft:material_axe", ToolKind.AXE),
                assembly("hoe", "tool_head_hoe",
                        "cruciblecraft:material_hoe", ToolKind.HOE),
                assembly("sword", "tool_head_sword",
                        "cruciblecraft:material_sword", ToolKind.SWORD),
                assembly("file", "tool_head_file",
                        "cruciblecraft:material_file", ToolKind.FILE),
                assembly("chisel", "tool_head_chisel",
                        "cruciblecraft:material_chisel", ToolKind.CHISEL),
                assembly("saw", "tool_head_saw",
                        "cruciblecraft:material_saw", ToolKind.SAW),
                assembly("screwdriver", "tool_head_screwdriver",
                        "cruciblecraft:material_screwdriver",
                        ToolKind.SCREWDRIVER),
                assembly("smithing_hammer", "tool_head_hammer",
                        HAMMER_ITEM, ToolKind.SMITHING_HAMMER),
                assembly("soft_hammer", "tool_head_hammer",
                        SOFT_HAMMER_ITEM, ToolKind.SOFT_HAMMER),
                assembly("spade", "tool_head_spade",
                        "cruciblecraft:material_spade", ToolKind.SPADE),
                assembly("double_axe", "tool_head_axe_double",
                        "cruciblecraft:material_double_axe", ToolKind.DOUBLE_AXE),
                assembly("sense", "tool_head_sense",
                        "cruciblecraft:material_sense", ToolKind.SENSE),
                assembly("plow", "tool_head_plow",
                        "cruciblecraft:material_plow", ToolKind.PLOW),
                assembly("construction_pick", "tool_head_construction_pickaxe",
                        "cruciblecraft:material_construction_pick",
                        ToolKind.CONSTRUCTION_PICK),
                assembly("builder_wand", "tool_head_builderwand",
                        "cruciblecraft:material_builder_wand",
                        ToolKind.BUILDER_WAND),
                assembly("magnifying_glass", "lens",
                        "cruciblecraft:material_magnifying_glass",
                        ToolKind.MAGNIFYING_GLASS));
    }

    public static List<AssemblyVariant> assemblyVariants(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        List<AssemblyVariant> variants = new ArrayList<>();
        materials.stream()
                .sorted(Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> {
                    Set<MaterialPrefix> forms = formsOf(material, registeredForms);
                    for (Assembly assembly : assemblies()) {
                        MaterialPrefix head = new MaterialPrefix(
                                "cruciblecraft:" + assembly.headPrefix());
                        if (!forms.contains(head)
                                || !eligible(material, assembly.kind())) {
                            continue;
                        }
                        variants.add(new AssemblyVariant(
                                assembly.path() + "/" + material.id(),
                                item(material, head),
                                assembly.resultId(),
                                material.id()));
                    }
                });
        return List.copyOf(variants);
    }

    public static List<Recipe> plan(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        LinkedHashMap<String, Recipe> byPath = new LinkedHashMap<>();
        Map<String, MaterialDefinition> byId = new LinkedHashMap<>();
        materials.forEach(material -> byId.put(material.id(), material));
        materials.stream()
                .sorted(Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> {
                    Set<MaterialPrefix> forms = formsOf(material, registeredForms);
                    recipesFor(material, forms)
                            .forEach(recipe -> putUnique(byPath, recipe));
                    List<Recipe> electric = new ArrayList<>();
                    ElectricToolRecipes.add(
                            electric, material, forms, registeredForms, byId);
                    electric.forEach(recipe -> putUnique(byPath, recipe));
                });
        vanillaFlintHarvest().forEach(recipe -> putUnique(byPath, recipe));
        vanillaBoneClub().forEach(recipe -> putUnique(byPath, recipe));
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

    static Set<MaterialPrefix> formsOf(
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
        addHarvestHeads(recipes, material, forms);
        addFinishedExtras(recipes, material, forms);
        addSimpleRockTools(recipes, material, forms);
        addCrystalPlates(recipes, material, forms);
        return recipes;
    }

    private static void addWorkshop(
            List<Recipe> recipes,
            MaterialDefinition material,
            Set<MaterialPrefix> forms) {
        String id = material.id();
        if (eligible(material, ToolKind.FILE)
                && forms.contains(MaterialPrefixes.PLATE)
                && forms.contains(TOOL_HEAD_FILE)) {
            recipes.add(head(
                    path(id, "tool_head_file"),
                    pad3x3(" P ", " Pk"),
                    map("P", item(material, MaterialPrefixes.PLATE)),
                    map("k", KNIFE_ITEM),
                    item(material, TOOL_HEAD_FILE),
                    id));
        }
        if (eligible(material, ToolKind.WRENCH)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(finished(
                        path(id, "wrench"),
                        List.of("PhP", " P ", " P "),
                        map("P", item(material, MaterialPrefixes.PLATE)),
                        map("h", HAMMER_ITEM),
                        "cruciblecraft:material_wrench",
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(finished(
                        path(id, "wrench"),
                        List.of("CfC", " C ", " C "),
                        map("C", item(material, MaterialPrefixes.GEM)),
                        map("f", FILE_ITEM),
                        "cruciblecraft:material_wrench",
                        id));
            }
        }
        if (eligible(material, ToolKind.SCREWDRIVER)
                && forms.contains(MaterialPrefixes.ROD)
                && forms.contains(TOOL_HEAD_SCREWDRIVER)) {
            recipes.add(head(
                    path(id, "tool_head_screwdriver"),
                    pad3x3("hS", "Sf"),
                    map("S", item(material, MaterialPrefixes.ROD)),
                    map("h", HAMMER_ITEM, "f", FILE_ITEM),
                    item(material, TOOL_HEAD_SCREWDRIVER),
                    id));
        }
        if (eligible(material, ToolKind.SAW)
                && forms.contains(TOOL_HEAD_SAW)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(head(
                        path(id, "tool_head_saw"),
                        pad3x3("PP", "fh"),
                        map("P", item(material, MaterialPrefixes.PLATE)),
                        map("f", FILE_ITEM, "h", HAMMER_ITEM),
                        item(material, TOOL_HEAD_SAW),
                        id));
            } else if (forms.contains(PLATE_GEM)) {
                recipes.add(head(
                        path(id, "tool_head_saw"),
                        pad3x3("CC", "f "),
                        map("C", item(material, PLATE_GEM)),
                        map("f", FILE_ITEM),
                        item(material, TOOL_HEAD_SAW),
                        id));
            }
        }
        if (eligible(material, ToolKind.CHISEL)
                && forms.contains(MaterialPrefixes.ROD)
                && forms.contains(TOOL_HEAD_CHISEL)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(head(
                        path(id, "tool_head_chisel"),
                        pad3x3("hPf", " S "),
                        map(
                                "P", item(material, MaterialPrefixes.PLATE),
                                "S", item(material, MaterialPrefixes.ROD)),
                        map("h", HAMMER_ITEM, "f", FILE_ITEM),
                        item(material, TOOL_HEAD_CHISEL),
                        id));
            } else if (forms.contains(PLATE_GEM)) {
                recipes.add(head(
                        path(id, "tool_head_chisel"),
                        pad3x3("Cf", "S "),
                        map(
                                "C", item(material, PLATE_GEM),
                                "S", item(material, MaterialPrefixes.ROD)),
                        map("f", FILE_ITEM),
                        item(material, TOOL_HEAD_CHISEL),
                        id));
            }
        }
        if (eligible(material, ToolKind.WIRE_CUTTER)
                && forms.contains(MaterialPrefixes.ROD)
                && forms.contains(MaterialPrefixes.SCREW)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(finished(
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
                recipes.add(finished(
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
                recipes.add(finished(
                        path(id, "monkey_wrench"),
                        List.of("PPd", "hPT", " P "),
                        map(
                                "P", item(material, MaterialPrefixes.PLATE),
                                "T", item(material, MaterialPrefixes.SCREW)),
                        map("d", SCREWDRIVER_ITEM, "h", HAMMER_ITEM),
                        "cruciblecraft:material_monkey_wrench",
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(finished(
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
        if (eligible(material, ToolKind.SMITHING_HAMMER)
                && forms.contains(TOOL_HEAD_HAMMER)) {
            if (forms.contains(MaterialPrefixes.INGOT)) {
                recipes.add(head(
                        path(id, "tool_head_hammer"),
                        List.of("II ", "IIh", "II "),
                        map("I", item(material, MaterialPrefixes.INGOT)),
                        map("h", HAMMER_ITEM),
                        item(material, TOOL_HEAD_HAMMER),
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(head(
                        path(id, "tool_head_hammer"),
                        List.of("GG ", "GGf", "GG "),
                        map("G", item(material, MaterialPrefixes.GEM)),
                        map("f", FILE_ITEM),
                        item(material, TOOL_HEAD_HAMMER),
                        id));
            }
        }
        if (eligible(material, ToolKind.SOFT_HAMMER)
                && forms.contains(TOOL_HEAD_HAMMER)) {
            if (forms.contains(MaterialPrefixes.INGOT)) {
                recipes.add(head(
                        path(id, "tool_head_hammer"),
                        List.of("II ", "IIr", "II "),
                        map("I", item(material, MaterialPrefixes.INGOT)),
                        map("r", SOFT_HAMMER_ITEM),
                        item(material, TOOL_HEAD_HAMMER),
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(head(
                        path(id, "tool_head_hammer"),
                        List.of("GG ", "GGr", "GG "),
                        map("G", item(material, MaterialPrefixes.GEM)),
                        map("r", SOFT_HAMMER_ITEM),
                        item(material, TOOL_HEAD_HAMMER),
                        id));
            }
        }
    }

    private static void addHarvestHeads(
            List<Recipe> recipes,
            MaterialDefinition material,
            Set<MaterialPrefix> forms) {
        String id = material.id();
        boolean plateIngot = forms.contains(MaterialPrefixes.PLATE)
                && forms.contains(MaterialPrefixes.INGOT);
        boolean plate = forms.contains(MaterialPrefixes.PLATE);
        boolean plateGem = forms.contains(PLATE_GEM);
        boolean gem = forms.contains(MaterialPrefixes.GEM);
        if (eligible(material, ToolKind.PICKAXE)
                && forms.contains(TOOL_HEAD_PICKAXE)) {
            if (plateIngot) {
                recipes.add(head(
                        path(id, "tool_head_pickaxe"),
                        pad3x3("PII", "f h"),
                        map(
                                "P", item(material, MaterialPrefixes.PLATE),
                                "I", item(material, MaterialPrefixes.INGOT)),
                        map("f", FILE_ITEM, "h", HAMMER_ITEM),
                        item(material, TOOL_HEAD_PICKAXE),
                        id));
            } else if (plateGem && gem) {
                recipes.add(head(
                        path(id, "tool_head_pickaxe"),
                        pad3x3("CGG", "f  "),
                        map(
                                "C", item(material, PLATE_GEM),
                                "G", item(material, MaterialPrefixes.GEM)),
                        map("f", FILE_ITEM),
                        item(material, TOOL_HEAD_PICKAXE),
                        id));
            }
        }
        if (eligible(material, ToolKind.SHOVEL)
                && forms.contains(TOOL_HEAD_SHOVEL)) {
            if (plate) {
                recipes.add(head(
                        path(id, "tool_head_shovel"),
                        pad3x3("fPh"),
                        map("P", item(material, MaterialPrefixes.PLATE)),
                        map("f", FILE_ITEM, "h", HAMMER_ITEM),
                        item(material, TOOL_HEAD_SHOVEL),
                        id));
            } else if (plateGem) {
                recipes.add(head(
                        path(id, "tool_head_shovel"),
                        pad3x3("fC "),
                        map("C", item(material, PLATE_GEM)),
                        map("f", FILE_ITEM),
                        item(material, TOOL_HEAD_SHOVEL),
                        id));
            }
        }
        if (eligible(material, ToolKind.AXE)
                && forms.contains(TOOL_HEAD_AXE)) {
            if (plateIngot) {
                recipes.add(head(
                        path(id, "tool_head_axe"),
                        List.of("PIh", "P  ", "f  "),
                        map(
                                "P", item(material, MaterialPrefixes.PLATE),
                                "I", item(material, MaterialPrefixes.INGOT)),
                        map("h", HAMMER_ITEM, "f", FILE_ITEM),
                        item(material, TOOL_HEAD_AXE),
                        id));
            } else if (plateGem && gem) {
                recipes.add(head(
                        path(id, "tool_head_axe"),
                        List.of("CG ", "C  ", "f  "),
                        map(
                                "C", item(material, PLATE_GEM),
                                "G", item(material, MaterialPrefixes.GEM)),
                        map("f", FILE_ITEM),
                        item(material, TOOL_HEAD_AXE),
                        id));
            }
        }
        if (eligible(material, ToolKind.HOE)
                && forms.contains(TOOL_HEAD_HOE)) {
            if (plateIngot) {
                recipes.add(head(
                        path(id, "tool_head_hoe"),
                        pad3x3("PIh", "f  "),
                        map(
                                "P", item(material, MaterialPrefixes.PLATE),
                                "I", item(material, MaterialPrefixes.INGOT)),
                        map("h", HAMMER_ITEM, "f", FILE_ITEM),
                        item(material, TOOL_HEAD_HOE),
                        id));
            } else if (plateGem && gem) {
                recipes.add(head(
                        path(id, "tool_head_hoe"),
                        pad3x3("CG ", "f  "),
                        map(
                                "C", item(material, PLATE_GEM),
                                "G", item(material, MaterialPrefixes.GEM)),
                        map("f", FILE_ITEM),
                        item(material, TOOL_HEAD_HOE),
                        id));
            }
        }
        if (eligible(material, ToolKind.SWORD)
                && forms.contains(TOOL_HEAD_SWORD)) {
            if (plate) {
                recipes.add(head(
                        path(id, "tool_head_sword"),
                        pad3x3(" P ", "fPh"),
                        map("P", item(material, MaterialPrefixes.PLATE)),
                        map("f", FILE_ITEM, "h", HAMMER_ITEM),
                        item(material, TOOL_HEAD_SWORD),
                        id));
            } else if (plateGem) {
                recipes.add(head(
                        path(id, "tool_head_sword"),
                        pad3x3(" C ", "fC "),
                        map("C", item(material, PLATE_GEM)),
                        map("f", FILE_ITEM),
                        item(material, TOOL_HEAD_SWORD),
                        id));
            }
        }
        if (eligible(material, ToolKind.SPADE)
                && forms.contains(TOOL_HEAD_SPADE)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(head(
                        path(id, "tool_head_spade"),
                        pad3x3("fPh", " s "),
                        map("P", item(material, MaterialPrefixes.PLATE)),
                        map("f", FILE_ITEM, "h", HAMMER_ITEM, "s", SAW_ITEM),
                        item(material, TOOL_HEAD_SPADE),
                        id));
            } else if (forms.contains(PLATE_GEM)) {
                recipes.add(head(
                        path(id, "tool_head_spade"),
                        pad3x3("fC ", " s "),
                        map("C", item(material, PLATE_GEM)),
                        map("f", FILE_ITEM, "s", SAW_ITEM),
                        item(material, TOOL_HEAD_SPADE),
                        id));
            }
        }
        if (eligible(material, ToolKind.DOUBLE_AXE)
                && forms.contains(TOOL_HEAD_AXE_DOUBLE)
                && plateIngot) {
            recipes.add(head(
                    path(id, "tool_head_axe_double"),
                    List.of("PIP", "P P", "f h"),
                    map(
                            "P", item(material, MaterialPrefixes.PLATE),
                            "I", item(material, MaterialPrefixes.INGOT)),
                    map("f", FILE_ITEM, "h", HAMMER_ITEM),
                    item(material, TOOL_HEAD_AXE_DOUBLE),
                    id));
        } else if (eligible(material, ToolKind.DOUBLE_AXE)
                && forms.contains(TOOL_HEAD_AXE_DOUBLE)
                && plateGem
                && gem) {
            recipes.add(head(
                    path(id, "tool_head_axe_double"),
                    List.of("CGC", "C C", "f  "),
                    map(
                            "C", item(material, PLATE_GEM),
                            "G", item(material, MaterialPrefixes.GEM)),
                    map("f", FILE_ITEM),
                    item(material, TOOL_HEAD_AXE_DOUBLE),
                    id));
        }
        if (eligible(material, ToolKind.SENSE)
                && forms.contains(TOOL_HEAD_SENSE)
                && plateIngot) {
            recipes.add(head(
                    path(id, "tool_head_sense"),
                    pad3x3("PPI", "f h"),
                    map(
                            "P", item(material, MaterialPrefixes.PLATE),
                            "I", item(material, MaterialPrefixes.INGOT)),
                    map("f", FILE_ITEM, "h", HAMMER_ITEM),
                    item(material, TOOL_HEAD_SENSE),
                    id));
        } else if (eligible(material, ToolKind.SENSE)
                && forms.contains(TOOL_HEAD_SENSE)
                && plateGem
                && gem) {
            recipes.add(head(
                    path(id, "tool_head_sense"),
                    pad3x3("CCG", "f  "),
                    map(
                            "C", item(material, PLATE_GEM),
                            "G", item(material, MaterialPrefixes.GEM)),
                    map("f", FILE_ITEM),
                    item(material, TOOL_HEAD_SENSE),
                    id));
        }
        if (eligible(material, ToolKind.PLOW)
                && forms.contains(TOOL_HEAD_PLOW)
                && forms.contains(MaterialPrefixes.PLATE)) {
            recipes.add(head(
                    path(id, "tool_head_plow"),
                    List.of("PPP", "PPP", "f h"),
                    map("P", item(material, MaterialPrefixes.PLATE)),
                    map("f", FILE_ITEM, "h", HAMMER_ITEM),
                    item(material, TOOL_HEAD_PLOW),
                    id));
        } else if (eligible(material, ToolKind.PLOW)
                && forms.contains(TOOL_HEAD_PLOW)
                && plateGem) {
            recipes.add(head(
                    path(id, "tool_head_plow"),
                    List.of("CCC", "CCC", "f  "),
                    map("C", item(material, PLATE_GEM)),
                    map("f", FILE_ITEM),
                    item(material, TOOL_HEAD_PLOW),
                    id));
        }
        if (eligible(material, ToolKind.CONSTRUCTION_PICK)
                && forms.contains(TOOL_HEAD_CONSTRUCTION)
                && plateIngot) {
            recipes.add(head(
                    path(id, "tool_head_construction_pickaxe"),
                    pad3x3("PIP", "f h"),
                    map(
                            "P", item(material, MaterialPrefixes.PLATE),
                            "I", item(material, MaterialPrefixes.INGOT)),
                    map("f", FILE_ITEM, "h", HAMMER_ITEM),
                    item(material, TOOL_HEAD_CONSTRUCTION),
                    id));
        } else if (eligible(material, ToolKind.CONSTRUCTION_PICK)
                && forms.contains(TOOL_HEAD_CONSTRUCTION)
                && plateGem
                && gem) {
            recipes.add(head(
                    path(id, "tool_head_construction_pickaxe"),
                    pad3x3("CGC", "f  "),
                    map(
                            "C", item(material, PLATE_GEM),
                            "G", item(material, MaterialPrefixes.GEM)),
                    map("f", FILE_ITEM),
                    item(material, TOOL_HEAD_CONSTRUCTION),
                    id));
        }
        if (eligible(material, ToolKind.BUILDER_WAND)
                && forms.contains(TOOL_HEAD_BUILDERWAND)
                && forms.contains(MaterialPrefixes.PLATE)) {
            recipes.add(head(
                    path(id, "tool_head_builderwand"),
                    List.of(" P ", "f h", " s "),
                    map("P", item(material, MaterialPrefixes.PLATE)),
                    map("f", FILE_ITEM, "h", HAMMER_ITEM, "s", SAW_ITEM),
                    item(material, TOOL_HEAD_BUILDERWAND),
                    id));
        } else if (eligible(material, ToolKind.BUILDER_WAND)
                && forms.contains(TOOL_HEAD_BUILDERWAND)
                && plateGem) {
            recipes.add(head(
                    path(id, "tool_head_builderwand"),
                    List.of(" C ", "f h", " s "),
                    map("C", item(material, PLATE_GEM)),
                    map("f", FILE_ITEM, "h", HAMMER_ITEM, "s", SAW_ITEM),
                    item(material, TOOL_HEAD_BUILDERWAND),
                    id));
        }
    }

    private static void addFinishedExtras(
            List<Recipe> recipes,
            MaterialDefinition material,
            Set<MaterialPrefix> forms) {
        String id = material.id();
        if (eligible(material, ToolKind.KNIFE)) {
            if (forms.contains(MaterialPrefixes.PLATE)) {
                recipes.add(finished(
                        path(id, "knife"),
                        pad3x3("fP", "hH"),
                        map(
                                "P", item(material, MaterialPrefixes.PLATE),
                                "H", STICK),
                        map("f", FILE_ITEM, "h", HAMMER_ITEM),
                        "cruciblecraft:material_knife",
                        id));
            } else if (forms.contains(PLATE_GEM)) {
                recipes.add(finished(
                        path(id, "knife"),
                        pad3x3("fC", "hH"),
                        map(
                                "C", item(material, PLATE_GEM),
                                "H", STICK),
                        map("f", FILE_ITEM, "h", HAMMER_ITEM),
                        "cruciblecraft:material_knife",
                        id));
            }
            if ("obsidian".equals(id) && forms.contains(ROCK)) {
                recipes.add(finished(
                        path(id, "knife_from_rock"),
                        pad3x3("SX"),
                        map("X", item(material, ROCK), "S", STICK),
                        Map.of(),
                        "cruciblecraft:material_knife",
                        id,
                        true));
            }
        }
        if (eligible(material, ToolKind.CLUB)) {
            if (forms.contains(MaterialPrefixes.INGOT)) {
                recipes.add(finished(
                        path(id, "club"),
                        List.of(" II", "III", "HI "),
                        map(
                                "I", item(material, MaterialPrefixes.INGOT),
                                "H", STICK),
                        Map.of(),
                        "cruciblecraft:material_club",
                        id));
            } else if (forms.contains(MaterialPrefixes.GEM)) {
                recipes.add(finished(
                        path(id, "club"),
                        List.of(" GG", "GGG", "HG "),
                        map(
                                "G", item(material, MaterialPrefixes.GEM),
                                "H", STICK),
                        Map.of(),
                        "cruciblecraft:material_club",
                        id));
            }
        }
        if (eligible(material, ToolKind.CROWBAR)
                && forms.contains(MaterialPrefixes.ROD)) {
            recipes.add(finished(
                    path(id, "crowbar"),
                    List.of("hVS", "VSV", "SVf"),
                    map("S", item(material, MaterialPrefixes.ROD), "V", BLUE_DYE),
                    map("h", HAMMER_ITEM, "f", FILE_ITEM),
                    "cruciblecraft:material_crowbar",
                    id));
        }
        if (eligible(material, ToolKind.PLUNGER)
                && forms.contains(MaterialPrefixes.ROD)) {
            recipes.add(finished(
                    path(id, "plunger"),
                    List.of("xVV", " SV", "S f"),
                    map("S", item(material, MaterialPrefixes.ROD), "V", RUBBER_PLATE),
                    map("x", WIRE_CUTTER_ITEM, "f", FILE_ITEM),
                    "cruciblecraft:material_plunger",
                    id));
        }
        if (eligible(material, ToolKind.SCOOP)
                && forms.contains(MaterialPrefixes.ROD)) {
            recipes.add(finished(
                    path(id, "scoop"),
                    List.of("SVS", "SSS", "xSh"),
                    map("S", item(material, MaterialPrefixes.ROD), "V", WHITE_WOOL),
                    map("x", WIRE_CUTTER_ITEM, "h", HAMMER_ITEM),
                    "cruciblecraft:material_scoop",
                    id));
        }
        if (eligible(material, ToolKind.BUTCHERY_KNIFE)
                && forms.contains(MaterialPrefixes.PLATE)) {
            recipes.add(finished(
                    path(id, "butchery_knife"),
                    List.of("fPP", "hPP", "  H"),
                    map("P", item(material, MaterialPrefixes.PLATE), "H", STICK),
                    map("f", FILE_ITEM, "h", HAMMER_ITEM),
                    "cruciblecraft:material_butchery_knife",
                    id));
        } else if (eligible(material, ToolKind.BUTCHERY_KNIFE)
                && forms.contains(PLATE_GEM)) {
            recipes.add(finished(
                    path(id, "butchery_knife"),
                    List.of("fCC", " CC", "  H"),
                    map("C", item(material, PLATE_GEM), "H", STICK),
                    map("f", FILE_ITEM),
                    "cruciblecraft:material_butchery_knife",
                    id));
        }
        if (eligible(material, ToolKind.BRANCH_CUTTER)
                && forms.contains(MaterialPrefixes.PLATE)
                && forms.contains(MaterialPrefixes.ROD)
                && forms.contains(MaterialPrefixes.SCREW)) {
            recipes.add(finished(
                    path(id, "branch_cutter"),
                    List.of("PfP", "PdP", "STS"),
                    map(
                            "P", item(material, MaterialPrefixes.PLATE),
                            "S", item(material, MaterialPrefixes.ROD),
                            "T", item(material, MaterialPrefixes.SCREW)),
                    map("f", FILE_ITEM, "d", SCREWDRIVER_ITEM),
                    "cruciblecraft:material_branch_cutter",
                    id));
        } else if (eligible(material, ToolKind.BRANCH_CUTTER)
                && forms.contains(PLATE_GEM)
                && forms.contains(MaterialPrefixes.ROD)
                && forms.contains(MaterialPrefixes.SCREW)) {
            recipes.add(finished(
                    path(id, "branch_cutter"),
                    List.of("CfC", "CdC", "STS"),
                    map(
                            "C", item(material, PLATE_GEM),
                            "S", item(material, MaterialPrefixes.ROD),
                            "T", item(material, MaterialPrefixes.SCREW)),
                    map("f", FILE_ITEM, "d", SCREWDRIVER_ITEM),
                    "cruciblecraft:material_branch_cutter",
                    id));
        }
        if (eligible(material, ToolKind.SCISSORS)
                && forms.contains(MaterialPrefixes.PLATE)
                && forms.contains(MaterialPrefixes.SCREW)
                && forms.contains(MaterialPrefixes.RING)) {
            recipes.add(finished(
                    path(id, "scissors"),
                    List.of("PfP", " T ", "OdO"),
                    map(
                            "P", item(material, MaterialPrefixes.PLATE),
                            "T", item(material, MaterialPrefixes.SCREW),
                            "O", item(material, MaterialPrefixes.RING)),
                    map("f", FILE_ITEM, "d", SCREWDRIVER_ITEM),
                    "cruciblecraft:material_scissors",
                    id));
        } else if (eligible(material, ToolKind.SCISSORS)
                && forms.contains(PLATE_GEM)
                && forms.contains(MaterialPrefixes.SCREW)
                && forms.contains(MaterialPrefixes.RING)) {
            recipes.add(finished(
                    path(id, "scissors"),
                    List.of("CfC", " T ", "OdO"),
                    map(
                            "C", item(material, PLATE_GEM),
                            "T", item(material, MaterialPrefixes.SCREW),
                            "O", item(material, MaterialPrefixes.RING)),
                    map("f", FILE_ITEM, "d", SCREWDRIVER_ITEM),
                    "cruciblecraft:material_scissors",
                    id));
        }
        if (eligible(material, ToolKind.PINCERS)
                && forms.contains(MaterialPrefixes.CURVED_PLATE)
                && forms.contains(MaterialPrefixes.SCREW)
                && forms.contains(MaterialPrefixes.ROD)) {
            recipes.add(finished(
                    path(id, "pincers"),
                    List.of("XhX", " T ", "SdS"),
                    map(
                            "X", item(material, MaterialPrefixes.CURVED_PLATE),
                            "T", item(material, MaterialPrefixes.SCREW),
                            "S", item(material, MaterialPrefixes.ROD)),
                    map("h", HAMMER_ITEM, "d", SCREWDRIVER_ITEM),
                    "cruciblecraft:material_pincers",
                    id));
        }
        if (eligible(material, ToolKind.BENDING_CYLINDER)
                && forms.contains(MaterialPrefixes.INGOT)) {
            recipes.add(finished(
                    path(id, "bending_cylinder"),
                    List.of("sfh", "III", "III"),
                    map("I", item(material, MaterialPrefixes.INGOT)),
                    map("s", SAW_ITEM, "f", FILE_ITEM, "h", HAMMER_ITEM),
                    "cruciblecraft:material_bending_cylinder",
                    id));
        }
        if (eligible(material, ToolKind.BENDING_CYLINDER_SMALL)
                && forms.contains(MaterialPrefixes.INGOT)) {
            recipes.add(finished(
                    path(id, "bending_cylinder_small"),
                    pad3x3("sfh", "III"),
                    map("I", item(material, MaterialPrefixes.INGOT)),
                    map("s", SAW_ITEM, "f", FILE_ITEM, "h", HAMMER_ITEM),
                    "cruciblecraft:material_bending_cylinder_small",
                    id));
        }
        if (eligible(material, ToolKind.HAND_DRILL)
                && forms.contains(TOOL_HEAD_ARROW)
                && forms.contains(MaterialPrefixes.BOLT)) {
            recipes.add(finished(
                    path(id, "hand_drill"),
                    List.of("  X", "HYH", "YH "),
                    map(
                            "X", item(material, TOOL_HEAD_ARROW),
                            "Y", item(material, MaterialPrefixes.BOLT),
                            "H", STICK),
                    Map.of(),
                    "cruciblecraft:material_hand_drill",
                    id));
        }
        if (eligible(material, ToolKind.ROLLING_PIN)
                && ROLLING_PIN_MATERIALS.contains(id)
                && forms.contains(MaterialPrefixes.INGOT)
                && forms.contains(MaterialPrefixes.ROD)) {
            recipes.add(finished(
                    path(id, "rolling_pin"),
                    List.of("  S", " I ", "S f"),
                    map(
                            "I", item(material, MaterialPrefixes.INGOT),
                            "S", item(material, MaterialPrefixes.ROD)),
                    map("f", FILE_ITEM),
                    "cruciblecraft:material_rolling_pin",
                    id,
                    true));
        }
        if (eligible(material, ToolKind.FLINT_AND_TINDER)
                && FLINT_TINDER_NUGGET_MATERIALS.contains(id)
                && forms.contains(MaterialPrefixes.NUGGET)) {
            recipes.add(finished(
                    path(id, "flint_and_tinder"),
                    pad3x3("T ", " F"),
                    map("T", item(material, MaterialPrefixes.NUGGET), "F", FLINT),
                    Map.of(),
                    "cruciblecraft:material_flint_and_tinder",
                    id,
                    true));
        }
        if (eligible(material, ToolKind.POCKET_MULTITOOL)
                && forms.contains(TOOL_HEAD_SCREWDRIVER)
                && forms.contains(TOOL_HEAD_SAW)
                && forms.contains(TOOL_HEAD_CHISEL)
                && forms.contains(TOOL_HEAD_FILE)
                && forms.contains(TOOL_HEAD_SWORD)
                && forms.contains(MaterialPrefixes.RING)
                && forms.contains(MaterialPrefixes.PLATE)) {
            LinkedHashMap<String, String> pocket = new LinkedHashMap<>();
            pocket.put("A", item(material, TOOL_HEAD_SCREWDRIVER));
            pocket.put("X", item(material, TOOL_HEAD_SAW));
            pocket.put("Y", item(material, TOOL_HEAD_CHISEL));
            pocket.put("Z", item(material, TOOL_HEAD_FILE));
            pocket.put("V", item(material, TOOL_HEAD_SWORD));
            pocket.put("W", item(material, TOOL_HEAD_SWORD));
            pocket.put("O", item(material, MaterialPrefixes.RING));
            pocket.put("P", item(material, MaterialPrefixes.PLATE));
            recipes.add(finished(
                    path(id, "pocket_multitool"),
                    List.of("AXO", "ZPV", "OWY"),
                    pocket,
                    Map.of(),
                    "cruciblecraft:material_pocket_multitool",
                    id));
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
        if ("flint".equals(id)) {
            return;
        }
        String rock = item(material, ROCK);
        Map<String, String> headAndStick = map("X", rock, "S", STICK);
        boolean obsidian = "obsidian".equals(id);
        boolean petrifiedWood = "petrified_wood".equals(id);
        if (eligible(material, ToolKind.AXE)) {
            recipes.add(finished(
                    path(id, "axe"),
                    pad3x3("XX", "XS"),
                    headAndStick,
                    Map.of(),
                    "cruciblecraft:material_axe",
                    id,
                    true));
        }
        if (!obsidian && eligible(material, ToolKind.HOE)) {
            recipes.add(finished(
                    path(id, "hoe"),
                    pad3x3("XX", " S"),
                    headAndStick,
                    Map.of(),
                    "cruciblecraft:material_hoe",
                    id,
                    true));
        }
        if (eligible(material, ToolKind.SHOVEL)) {
            recipes.add(finished(
                    path(id, "shovel"),
                    pad3x3("X", "S"),
                    headAndStick,
                    Map.of(),
                    "cruciblecraft:material_shovel",
                    id));
        }
        if (eligible(material, ToolKind.PICKAXE)) {
            recipes.add(finished(
                    path(id, "pickaxe"),
                    pad3x3("XXX", " S "),
                    headAndStick,
                    Map.of(),
                    "cruciblecraft:material_pickaxe",
                    id));
        }
        if (!obsidian
                && eligible(material, ToolKind.CLUB)
                && recipes.stream().noneMatch(
                        recipe -> path(id, "club").equals(recipe.path()))) {
            recipes.add(finished(
                    path(id, "club"),
                    List.of(" XX", "XXX", "SX "),
                    headAndStick,
                    Map.of(),
                    "cruciblecraft:material_club",
                    id,
                    true));
        }
        if (!obsidian
                && (eligible(material, ToolKind.SMITHING_HAMMER) || petrifiedWood)
                && recipes.stream().noneMatch(
                        recipe -> path(id, "tool_head_hammer")
                                .equals(recipe.path()))) {
            recipes.add(finished(
                    path(id, "smithing_hammer"),
                    List.of("XX ", "XXS", "XX "),
                    headAndStick,
                    Map.of(),
                    HAMMER_ITEM,
                    id,
                    true));
        }
    }

    private static void addCrystalPlates(
            List<Recipe> recipes,
            MaterialDefinition material,
            Set<MaterialPrefix> forms) {
        String id = material.id();
        if (forms.contains(MaterialPrefixes.PLATE_GEM)
                && forms.contains(MaterialPrefixes.BOULE)) {
            recipes.add(sawCut(
                    prefixPath(id, "boule2plate_gem"),
                    item(material, MaterialPrefixes.BOULE),
                    item(material, MaterialPrefixes.PLATE_GEM),
                    3,
                    id));
        }
        if (forms.contains(MaterialPrefixes.PLATE_GEM)
                && forms.contains(MaterialPrefixes.GEM_FLAWLESS)) {
            recipes.add(sawCut(
                    prefixPath(id, "flawless2plate_gem"),
                    item(material, MaterialPrefixes.GEM_FLAWLESS),
                    item(material, MaterialPrefixes.PLATE_GEM),
                    1,
                    id));
        }
        if (forms.contains(MaterialPrefixes.PLATE_GEM)
                && forms.contains(MaterialPrefixes.GEM_EXQUISITE)) {
            recipes.add(sawCut(
                    prefixPath(id, "exquisite2plate_gem"),
                    item(material, MaterialPrefixes.GEM_EXQUISITE),
                    item(material, MaterialPrefixes.PLATE_GEM),
                    3,
                    id));
        }
        if (forms.contains(MaterialPrefixes.PLATE_GEM)
                && forms.contains(MaterialPrefixes.GEM_LEGENDARY)) {
            recipes.add(sawCut(
                    prefixPath(id, "legendary2plate_gem"),
                    item(material, MaterialPrefixes.GEM_LEGENDARY),
                    item(material, MaterialPrefixes.PLATE_GEM),
                    7,
                    id));
        }
        if (forms.contains(MaterialPrefixes.TINY_PLATE_GEM)
                && forms.contains(MaterialPrefixes.PLATE_GEM)) {
            recipes.add(sawCut(
                    prefixPath(id, "plate_gem2tiny"),
                    item(material, MaterialPrefixes.PLATE_GEM),
                    item(material, MaterialPrefixes.TINY_PLATE_GEM),
                    8,
                    id));
        }
        if (forms.contains(MaterialPrefixes.TINY_PLATE_GEM)
                && forms.contains(MaterialPrefixes.GEM_CHIPPED)) {
            recipes.add(sawCut(
                    prefixPath(id, "chipped2tiny_plate_gem"),
                    item(material, MaterialPrefixes.GEM_CHIPPED),
                    item(material, MaterialPrefixes.TINY_PLATE_GEM),
                    2,
                    id));
        }
        if (forms.contains(MaterialPrefixes.TINY_PLATE_GEM)
                && forms.contains(MaterialPrefixes.GEM_FLAWED)) {
            recipes.add(sawCut(
                    prefixPath(id, "flawed2tiny_plate_gem"),
                    item(material, MaterialPrefixes.GEM_FLAWED),
                    item(material, MaterialPrefixes.TINY_PLATE_GEM),
                    4,
                    id));
        }
        if (forms.contains(MaterialPrefixes.TINY_PLATE_GEM)
                && forms.contains(MaterialPrefixes.GEM)) {
            recipes.add(sawCut(
                    prefixPath(id, "gem2tiny_plate_gem"),
                    item(material, MaterialPrefixes.GEM),
                    item(material, MaterialPrefixes.TINY_PLATE_GEM),
                    8,
                    id));
        }
    }

    private static Recipe sawCut(
            String path,
            String inputId,
            String resultId,
            int count,
            String material) {
        return recipe(
                path,
                pad3x3("s ", " X"),
                map("X", inputId),
                map("s", SAW_ITEM),
                resultId,
                material,
                false,
                count);
    }

    private static List<Recipe> vanillaFlintHarvest() {
        Map<String, String> flintAndStick = map("X", FLINT, "S", STICK);
        return List.of(
                finished(
                        "tools/flint_pickaxe",
                        pad3x3("XXX", " S "),
                        flintAndStick,
                        Map.of(),
                        "cruciblecraft:material_pickaxe",
                        "flint"),
                finished(
                        "tools/flint_axe",
                        pad3x3("XX", "XS"),
                        flintAndStick,
                        Map.of(),
                        "cruciblecraft:material_axe",
                        "flint",
                        true),
                finished(
                        "tools/flint_shovel",
                        pad3x3("X", "S"),
                        flintAndStick,
                        Map.of(),
                        "cruciblecraft:material_shovel",
                        "flint"),
                finished(
                        "tools/flint_knife",
                        pad3x3("SX"),
                        flintAndStick,
                        Map.of(),
                        "cruciblecraft:material_knife",
                        "flint",
                        true));
    }

    private static List<Recipe> vanillaBoneClub() {
        return List.of(
                finished(
                        "tools/bone/club",
                        List.of("  X", " X ", "S  "),
                        map("X", BONE, "S", STICK),
                        Map.of(),
                        "cruciblecraft:material_club",
                        "bone",
                        true));
    }

    static boolean eligible(MaterialDefinition material, ToolKind kind) {
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

    static String item(MaterialDefinition material, MaterialPrefix form) {
        return MaterialLookup.logicalItemId(material, form, Map.of()).toString();
    }

    static String path(String material, String tool) {
        return "tools/" + material + "/" + tool;
    }

    private static String prefixPath(String material, String kind) {
        return "prefix/" + kind + "/" + material;
    }

    private static Assembly assembly(
            String tool, String headPrefix, String resultId, ToolKind kind) {
        return new Assembly(
                "tools/assemble/" + tool, headPrefix, resultId, kind);
    }

    static Recipe finished(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material) {
        return finished(
                path, pattern, ingredients, catalysts, resultId, material, false);
    }

    static Recipe finished(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material,
            boolean mirrored) {
        return recipe(
                path,
                pattern,
                ingredients,
                catalysts,
                resultId,
                material,
                true,
                1,
                mirrored);
    }

    private static Recipe head(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material) {
        return recipe(
                path, pattern, ingredients, catalysts, resultId, material, false);
    }

    private static Recipe recipe(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material,
            boolean persistToolMaterial) {
        return recipe(
                path,
                pattern,
                ingredients,
                catalysts,
                resultId,
                material,
                persistToolMaterial,
                1,
                false);
    }

    private static Recipe recipe(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material,
            boolean persistToolMaterial,
            int count) {
        return recipe(
                path,
                pattern,
                ingredients,
                catalysts,
                resultId,
                material,
                persistToolMaterial,
                count,
                false);
    }

    private static Recipe recipe(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material,
            boolean persistToolMaterial,
            int count,
            boolean mirrored) {
        return new Recipe(
                path,
                List.copyOf(pattern),
                Map.copyOf(new LinkedHashMap<>(ingredients)),
                Map.copyOf(new LinkedHashMap<>(catalysts)),
                resultId,
                material,
                persistToolMaterial,
                count,
                mirrored,
                0L,
                0L);
    }

    static Recipe electricFinished(
            String path,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material,
            long capacity,
            long voltage) {
        return new Recipe(
                path,
                List.copyOf(pattern),
                Map.copyOf(new LinkedHashMap<>(ingredients)),
                Map.copyOf(new LinkedHashMap<>(catalysts)),
                resultId,
                material,
                true,
                1,
                false,
                capacity,
                voltage);
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

    private static JsonObject toolKeys(Map<String, String> keys) {
        JsonObject object = new JsonObject();
        keys.forEach((symbol, itemId) -> {
            JsonObject slot = new JsonObject();
            CraftingTools.tagId(itemId).ifPresentOrElse(
                    tag -> slot.addProperty("tag", tag.location().toString()),
                    () -> slot.addProperty("item", itemId));
            object.add(symbol, slot);
        });
        return object;
    }

    static Map<String, String> map(String... pairs) {
        if ((pairs.length & 1) != 0) {
            throw new IllegalArgumentException("map requires even pairs");
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put(pairs[index], pairs[index + 1]);
        }
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
