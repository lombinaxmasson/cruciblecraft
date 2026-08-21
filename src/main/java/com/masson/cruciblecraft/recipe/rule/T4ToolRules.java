package com.masson.cruciblecraft.recipe.rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

/** Data-driven T4 tool recipes projected once per eligible material route. */
public final class T4ToolRules {
    private static final String TOOL_DOMAIN =
            "material.tag(\"PROPERTIES.HAS_TOOL_STATS\")"
                    + " && material.tool.types >= ";
    private static final String NOT_ANTIMATTER =
            "!material.tag(\"ATOMIC.ANTIMATTER\")";
    private static final String NOT_EXACT_WOOD =
            "!material.is(\"wood\")";
    private static final String NOT_COATED =
            "!material.tag(\"COMPOUNDS.COATED\")";
    private static final String NOT_WOOD_TAG =
            "!material.tag(\"PROPERTIES.WOOD\")";
    private static final String NOT_BOUNCY =
            "!material.tag(\"PROPERTIES.BOUNCY\")";
    private static final String NOT_STRETCHY =
            "!material.tag(\"PROPERTIES.STRETCHY\")";
    private static final String CORE_ELIGIBLE = and(
            TOOL_DOMAIN + "1",
            NOT_ANTIMATTER,
            NOT_EXACT_WOOD,
            NOT_COATED);
    private static final String HAMMER_ELIGIBLE = and(
            TOOL_DOMAIN + "1",
            NOT_ANTIMATTER,
            NOT_EXACT_WOOD,
            NOT_COATED,
            NOT_WOOD_TAG,
            NOT_BOUNCY,
            NOT_STRETCHY,
            "material.tool.quality >= 1");
    private static final String FILE_ELIGIBLE = and(
            TOOL_DOMAIN + "2",
            NOT_ANTIMATTER,
            NOT_EXACT_WOOD,
            NOT_COATED,
            NOT_BOUNCY,
            NOT_STRETCHY,
            "material.tool.quality <= 2");
    private static final String ADVANCED_HEAD_ELIGIBLE = and(
            TOOL_DOMAIN + "2",
            NOT_ANTIMATTER,
            NOT_EXACT_WOOD,
            NOT_COATED,
            NOT_BOUNCY,
            NOT_STRETCHY);
    private static final String SCREWDRIVER_ELIGIBLE = and(
            TOOL_DOMAIN + "2",
            NOT_ANTIMATTER,
            NOT_EXACT_WOOD,
            NOT_COATED);
    private static final String WRENCH_ELIGIBLE = and(
            TOOL_DOMAIN + "2",
            NOT_ANTIMATTER,
            NOT_EXACT_WOOD,
            NOT_BOUNCY,
            NOT_STRETCHY,
            "material.tool.quality >= 1");
    // GT6 Loader_Tools.java:324: And(ANTIMATTER.NOT, Wood.NOT, BOUNCY.NOT,
    // STRETCHY.NOT, typemin(2)) — no qualmin, COATED accepted like the wrench.
    private static final String WIRE_CUTTER_ELIGIBLE = and(
            TOOL_DOMAIN + "2",
            NOT_ANTIMATTER,
            NOT_EXACT_WOOD,
            NOT_BOUNCY,
            NOT_STRETCHY);

    // GT6 Loader_Tools.java at 3703e403 uses PII/CGG for generic heads;
    // only the exact MT.Stone identity retains the direct vanilla stone route.
    private static final String STONE_SIMPLE =
            "material.is(\"stone\")";
    private static final String PLATE_ROD =
            "has_registered(plate) && has_registered(rod)";
    private static final String PLATE_INGOT_ROD =
            "has_registered(plate) && has_registered(ingot)"
                    + " && has_registered(rod)";
    private static final String INGOT_ROD =
            "has_registered(ingot) && has_registered(rod)";
    private static final String GEM_ROD =
            "has_registered(gem) && has_registered(rod)";
    private static final String ROD =
            "has_registered(rod)";
    private static final String PLATE =
            "has_registered(plate)";
    private static final String GEM =
            "has_registered(gem)";
    private static final String SCREW =
            "has_registered(screw)";

    public static final List<Definition> PICKAXES = List.of(
            definition(
                    "pickaxe",
                    "stone_rod_exception",
                    ModItems.MATERIAL_PICKAXE.getId(),
                    CORE_ELIGIBLE,
                    and(STONE_SIMPLE, ROD),
                    List.of(
                            prefix(MaterialPrefixes.ROD, 2),
                            file())),
            definition(
                    "pickaxe",
                    "metal",
                    ModItems.MATERIAL_PICKAXE.getId(),
                    CORE_ELIGIBLE,
                    and("!(" + STONE_SIMPLE + ")", PLATE_INGOT_ROD),
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 1),
                            prefix(MaterialPrefixes.INGOT, 2),
                            prefix(MaterialPrefixes.ROD, 1),
                            file(),
                            hammer())),
            definition(
                    "pickaxe",
                    "gem",
                    ModItems.MATERIAL_PICKAXE.getId(),
                    CORE_ELIGIBLE,
                    and(
                            "!(" + STONE_SIMPLE + ")",
                            "!(" + PLATE_INGOT_ROD + ")",
                            GEM_ROD),
                    List.of(
                            prefix(MaterialPrefixes.GEM, 3),
                            prefix(MaterialPrefixes.ROD, 1),
                            file())));

    public static final List<Definition> SHOVELS = List.of(
            core(
                    "shovel",
                    "metal",
                    ModItems.MATERIAL_SHOVEL.getId(),
                    PLATE_ROD,
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 1),
                            prefix(MaterialPrefixes.ROD, 1),
                            file(),
                            hammer())),
            core(
                    "shovel",
                    "gem",
                    ModItems.MATERIAL_SHOVEL.getId(),
                    GEM_ROD,
                    List.of(
                            prefix(MaterialPrefixes.GEM, 1),
                            prefix(MaterialPrefixes.ROD, 1),
                            file())));

    public static final List<Definition> AXES = List.of(
            core(
                    "axe",
                    "metal",
                    ModItems.MATERIAL_AXE.getId(),
                    PLATE_INGOT_ROD,
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 2),
                            prefix(MaterialPrefixes.INGOT, 1),
                            prefix(MaterialPrefixes.ROD, 1),
                            file(),
                            hammer())),
            core(
                    "axe",
                    "gem",
                    ModItems.MATERIAL_AXE.getId(),
                    GEM_ROD,
                    List.of(
                            prefix(MaterialPrefixes.GEM, 2),
                            prefix(MaterialPrefixes.ROD, 1),
                            file())));

    public static final List<Definition> HOES = List.of(
            core(
                    "hoe",
                    "metal",
                    ModItems.MATERIAL_HOE.getId(),
                    PLATE_INGOT_ROD,
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 1),
                            prefix(MaterialPrefixes.INGOT, 1),
                            prefix(MaterialPrefixes.ROD, 1),
                            file(),
                            hammer())),
            core(
                    "hoe",
                    "gem",
                    ModItems.MATERIAL_HOE.getId(),
                    GEM_ROD,
                    List.of(
                            prefix(MaterialPrefixes.GEM, 2),
                            prefix(MaterialPrefixes.ROD, 1),
                            file())));

    public static final List<Definition> SWORDS = List.of(
            core(
                    "sword",
                    "metal",
                    ModItems.MATERIAL_SWORD.getId(),
                    PLATE_ROD,
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 2),
                            prefix(MaterialPrefixes.ROD, 1),
                            file(),
                            hammer())),
            core(
                    "sword",
                    "gem",
                    ModItems.MATERIAL_SWORD.getId(),
                    GEM_ROD,
                    List.of(
                            prefix(MaterialPrefixes.GEM, 2),
                            prefix(MaterialPrefixes.ROD, 1),
                            file())));

    public static final List<Definition> SMITHING_HAMMERS = List.of(
            definition(
                    "smithing_hammer",
                    "metal",
                    ModItems.SMITHING_HAMMER.getId(),
                    HAMMER_ELIGIBLE,
                    firstRoute(INGOT_ROD),
                    List.of(
                            prefix(MaterialPrefixes.INGOT, 6),
                            prefix(MaterialPrefixes.ROD, 1),
                            hammer())),
            definition(
                    "smithing_hammer",
                    "gem",
                    ModItems.SMITHING_HAMMER.getId(),
                    HAMMER_ELIGIBLE,
                    laterRoute(GEM_ROD, INGOT_ROD),
                    List.of(
                            prefix(MaterialPrefixes.GEM, 6),
                            prefix(MaterialPrefixes.ROD, 1),
                            file())));

    public static final List<Definition> FILES = List.of(
            definition(
                    "file",
                    "metal",
                    ModItems.MATERIAL_FILE.getId(),
                    FILE_ELIGIBLE,
                    firstRoute(PLATE_ROD),
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 2),
                            prefix(MaterialPrefixes.ROD, 1),
                            knife())));

    public static final List<Definition> CHISELS = List.of(
            definition(
                    "chisel",
                    "metal",
                    ModItems.MATERIAL_CHISEL.getId(),
                    ADVANCED_HEAD_ELIGIBLE,
                    firstRoute(PLATE_ROD),
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 1),
                            prefix(MaterialPrefixes.ROD, 2),
                            file(),
                            hammer())),
            definition(
                    "chisel",
                    "gem",
                    ModItems.MATERIAL_CHISEL.getId(),
                    ADVANCED_HEAD_ELIGIBLE,
                    laterRoute(GEM_ROD, PLATE_ROD),
                    List.of(
                            prefix(MaterialPrefixes.GEM, 1),
                            prefix(MaterialPrefixes.ROD, 2),
                            file())));

    public static final List<Definition> SAWS = List.of(
            definition(
                    "saw",
                    "metal",
                    ModItems.MATERIAL_SAW.getId(),
                    ADVANCED_HEAD_ELIGIBLE,
                    firstRoute(PLATE_ROD),
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 2),
                            prefix(MaterialPrefixes.ROD, 1),
                            file(),
                            hammer())),
            definition(
                    "saw",
                    "gem",
                    ModItems.MATERIAL_SAW.getId(),
                    ADVANCED_HEAD_ELIGIBLE,
                    laterRoute(GEM_ROD, PLATE_ROD),
                    List.of(
                            prefix(MaterialPrefixes.GEM, 2),
                            prefix(MaterialPrefixes.ROD, 1),
                            file())));

    public static final List<Definition> SCREWDRIVERS = List.of(
            definition(
                    "screwdriver",
                    "rod",
                    ModItems.MATERIAL_SCREWDRIVER.getId(),
                    SCREWDRIVER_ELIGIBLE,
                    firstRoute(ROD),
                    List.of(
                            prefix(MaterialPrefixes.ROD, 3),
                            file(),
                            hammer())));

    public static final List<Definition> WRENCHES = List.of(
            definition(
                    "wrench",
                    "metal",
                    ModItems.MATERIAL_WRENCH.getId(),
                    WRENCH_ELIGIBLE,
                    firstRoute(PLATE),
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 4),
                            hammer())),
            definition(
                    "wrench",
                    "gem",
                    ModItems.MATERIAL_WRENCH.getId(),
                    WRENCH_ELIGIBLE,
                    laterRoute(GEM, PLATE),
                    List.of(
                            prefix(MaterialPrefixes.GEM, 4),
                            file())));

    // GT6 Loader_Tools.java:324 wirecutter shapes {"PfP","hPd","STS"} and
    // {"CfC","hCd","STS"}: P=plate, C=plateGem (-> gem), T=screw, S=stick
    // (-> rod), f/h/d are the file, hammer and screwdriver catalysts.
    // The d (screwdriver) catalyst is dropped: the assembler tool-slot cap
    // (3 slots, 6 input rows) cannot carry three catalysts plus the pattern.
    public static final List<Definition> WIRE_CUTTERS = List.of(
            definition(
                    "wire_cutter",
                    "metal",
                    ModItems.MATERIAL_WIRE_CUTTER.getId(),
                    WIRE_CUTTER_ELIGIBLE,
                    firstRoute(and(PLATE, SCREW, ROD)),
                    List.of(
                            prefix(MaterialPrefixes.PLATE, 3),
                            prefix(MaterialPrefixes.SCREW, 1),
                            prefix(MaterialPrefixes.ROD, 2),
                            hammer(),
                            file())),
            definition(
                    "wire_cutter",
                    "gem",
                    ModItems.MATERIAL_WIRE_CUTTER.getId(),
                    WIRE_CUTTER_ELIGIBLE,
                    laterRoute(and(GEM, SCREW, ROD), and(PLATE, SCREW, ROD)),
                    List.of(
                            prefix(MaterialPrefixes.GEM, 3),
                            prefix(MaterialPrefixes.SCREW, 1),
                            prefix(MaterialPrefixes.ROD, 2),
                            hammer(),
                            file())));

    public static final List<Definition> ALL = Stream.of(
                    PICKAXES,
                    SHOVELS,
                    AXES,
                    HOES,
                    SWORDS,
                    SMITHING_HAMMERS,
                    FILES,
                    CHISELS,
                    SAWS,
                    SCREWDRIVERS,
                    WRENCHES,
                    WIRE_CUTTERS)
            .flatMap(List::stream)
            .toList();

    private T4ToolRules() {}

    private static Definition core(
            String tool,
            String route,
            net.minecraft.resources.ResourceLocation output,
            String closure,
            List<MaterialRule.ItemResource> inputs) {
        String routeCondition = switch (route) {
            case "metal" -> firstRoute(closure);
            case "gem" -> laterRoute(closure, metalClosure(tool));
            default -> throw new IllegalArgumentException("Unknown core tool route " + route);
        };
        return definition(
                tool,
                route,
                output,
                CORE_ELIGIBLE,
                routeCondition,
                inputs);
    }

    private static String metalClosure(String tool) {
        return switch (tool) {
            case "pickaxe", "axe", "hoe" -> PLATE_INGOT_ROD;
            case "shovel", "sword" -> PLATE_ROD;
            default -> throw new IllegalArgumentException("Unknown core tool " + tool);
        };
    }

    private static Definition definition(
            String tool,
            String route,
            net.minecraft.resources.ResourceLocation output,
            String eligibility,
            String routeCondition,
            List<MaterialRule.ItemResource> materialAndToolInputs) {
        List<MaterialRule.ItemResource> inputs = Stream.concat(
                        materialAndToolInputs.stream(),
                        Stream.of(pattern(tool)))
                .toList();
        MaterialRule rule = new MaterialRule(
                Optional.of(ModRecipeMaps.ASSEMBLER.id()),
                inputs,
                List.of(fixed(
                        output,
                        "$material",
                        Optional.empty())),
                List.of(),
                List.of(),
                "200",
                "32",
                "0",
                true,
                Optional.empty(),
                Map.of(),
                List.of(eligibility, routeCondition),
                Optional.empty(),
                List.of());
        return new Definition("t4/assembler/" + tool + "/" + route, rule);
    }

    private static MaterialRule.ItemResource prefix(
            MaterialPrefix prefix,
            int count) {
        return new MaterialRule.ItemResource(
                Optional.of(prefix.serializedId()),
                Optional.empty(),
                Integer.toString(count),
                "10000");
    }

    private static MaterialRule.ItemResource file() {
        return fixed(
                ModItems.MATERIAL_FILE.getId(),
                "iron",
                Optional.of(ItemInputAction.wear(1)));
    }

    private static MaterialRule.ItemResource hammer() {
        return fixed(
                ModItems.SMITHING_HAMMER.getId(),
                null,
                Optional.of(ItemInputAction.wear(1)));
    }

    private static MaterialRule.ItemResource knife() {
        return fixed(
                ModItems.FLINT_KNIFE.getId(),
                null,
                Optional.of(ItemInputAction.wear(1)));
    }

    private static MaterialRule.ItemResource pattern(String tool) {
        return fixed(
                ModItems.toolPattern(tool).getId(),
                null,
                Optional.of(ItemInputAction.PRESERVE));
    }

    private static MaterialRule.ItemResource fixed(
            net.minecraft.resources.ResourceLocation item,
            String material,
            Optional<ItemInputAction> inputAction) {
        return new MaterialRule.ItemResource(
                Optional.empty(),
                Optional.of(item),
                inputAction.isPresent() ? "0" : "1",
                "10000",
                Optional.empty(),
                false,
                material == null
                        ? Map.of()
                        : Map.of(ModComponents.TOOL_MATERIAL.getId(), material),
                inputAction);
    }

    private static String firstRoute(String closure) {
        return "(" + closure + ")";
    }

    private static String laterRoute(String closure, String... priorClosures) {
        String prior = Stream.of(priorClosures)
                .map(value -> "!(" + value + ")")
                .reduce((left, right) -> left + " && " + right)
                .orElse("true");
        return prior + " && (" + closure + ")";
    }

    private static String and(String... conditions) {
        return Stream.of(conditions)
                .map(value -> "(" + value + ")")
                .reduce((left, right) -> left + " && " + right)
                .orElseThrow();
    }

    public record Definition(String path, MaterialRule rule) {}
}
