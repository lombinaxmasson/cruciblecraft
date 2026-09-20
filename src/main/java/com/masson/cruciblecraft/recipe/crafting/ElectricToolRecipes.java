package com.masson.cruciblecraft.recipe.crafting;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.TechnologicalPartCatalog;
import com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog;
import com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog.RecipeGrid;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryCatalog;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryProfile;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/**
 * GT6 {@code Loader_Tools} 356–378 electric {@code OreProcessing_Tool} grids.
 * Skip a row when any live form or part is missing. Do not stand in.
 */
final class ElectricToolRecipes {
    private static final MaterialPrefix TOOL_HEAD_DRILL =
            new MaterialPrefix("cruciblecraft:tool_head_drill");
    private static final MaterialPrefix TOOL_HEAD_CHAINSAW =
            new MaterialPrefix("cruciblecraft:tool_head_chainsaw");
    private static final MaterialPrefix TOOL_HEAD_WRENCH =
            new MaterialPrefix("cruciblecraft:tool_head_wrench");
    private static final MaterialPrefix TOOL_HEAD_BUZZSAW =
            new MaterialPrefix("cruciblecraft:tool_head_buzzsaw");
    private static final MaterialPrefix TOOL_HEAD_SCREWDRIVER =
            new MaterialPrefix("cruciblecraft:tool_head_screwdriver");
    private static final MaterialPrefix TOOL_HEAD_SWORD =
            new MaterialPrefix("cruciblecraft:tool_head_sword");
    private static final MaterialPrefix CHAIN =
            new MaterialPrefix("cruciblecraft:chain");
    private static final String SCREWDRIVER =
            "cruciblecraft:material_screwdriver";
    private static final String FILE = "cruciblecraft:material_file";
    private static final String HAMMER = "cruciblecraft:smithing_hammer";
    private static final String STEEL = "steel";

    private ElectricToolRecipes() {}

    static void add(
            List<WorkbenchToolRecipePlan.Recipe> recipes,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            Map<String, List<MaterialPrefix>> registeredForms,
            Map<String, MaterialDefinition> byId) {
        for (ElectricToolCatalog spec : ElectricToolCatalog.values()) {
            if (spec.recipeGrid() == RecipeGrid.NONE) {
                continue;
            }
            if (!WorkbenchToolRecipePlan.eligible(material, spec.kind())) {
                continue;
            }
            if ((spec.kind() == ToolKind.MINING_DRILL_LV
                    || spec.kind() == ToolKind.CHAINSAW_LV)
                    && !WorkbenchToolRecipePlan.eligible(
                            material, ToolKind.MINING_DRILL_MV)) {
                continue;
            }
            MaterialPrefix head = headPrefix(spec);
            if (!forms.contains(head)) {
                continue;
            }
            MaterialDefinition hull = byId.get(spec.voltage().hullMaterial());
            if (hull == null) {
                continue;
            }
            Set<MaterialPrefix> hullForms = WorkbenchToolRecipePlan.formsOf(
                    hull, registeredForms);
            for (EnergyBatteryProfile battery : EnergyBatteryCatalog.profiles()) {
                if (battery.energyType() != EnergyType.ELECTRIC
                        || !battery.voltage().equals(spec.voltage().path())) {
                    continue;
                }
                WorkbenchToolRecipePlan.Recipe recipe = grid(
                        spec, material, forms, hull, hullForms, battery);
                if (recipe != null) {
                    recipes.add(recipe);
                }
            }
        }
        addMechanical(recipes, material, forms, registeredForms, byId);
    }

    private static WorkbenchToolRecipePlan.Recipe grid(
            ElectricToolCatalog spec,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition hull,
            Set<MaterialPrefix> hullForms,
            EnergyBatteryProfile battery) {
        String path = WorkbenchToolRecipePlan.path(
                material.id(),
                spec.kind().serializedName()
                        + "/"
                        + battery.id().getPath());
        String result = "cruciblecraft:" + spec.itemPath();
        return switch (spec.recipeGrid()) {
            case SHARED_HEAD -> sharedHead(
                    spec, material, forms, hull, hullForms, battery, path, result);
            case MIXER -> mixer(
                    spec, material, forms, hull, hullForms, battery, path, result);
            case HAND_DRILL -> handDrill(
                    spec, material, forms, hull, hullForms, battery, path, result);
            case SCREWDRIVER -> screwdriver(
                    spec, material, forms, hull, hullForms, battery, path, result);
            case BUZZSAW -> buzzsaw(
                    spec, material, forms, hull, hullForms, battery, path, result);
            case TRIMMER -> trimmer(
                    spec, material, forms, hull, hullForms, battery, path, result);
            case JACKHAMMER -> jackhammer(
                    spec, material, forms, hull, hullForms, battery, path, result);
            case NONE -> null;
        };
    }

    private static WorkbenchToolRecipePlan.Recipe sharedHead(
            ElectricToolCatalog spec,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition hull,
            Set<MaterialPrefix> hullForms,
            EnergyBatteryProfile battery,
            String path,
            String result) {
        if (!forms.contains(MaterialPrefixes.SCREW)
                || !hullForms.contains(MaterialPrefixes.CURVED_PLATE)
                || missingPart(spec.voltage().motorId())) {
            return null;
        }
        return WorkbenchToolRecipePlan.electricFinished(
                path,
                List.of("dAT", "XWX", "XVX"),
                WorkbenchToolRecipePlan.map(
                        "A", WorkbenchToolRecipePlan.item(material, headPrefix(spec)),
                        "T", WorkbenchToolRecipePlan.item(
                                material, MaterialPrefixes.SCREW),
                        "X", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.CURVED_PLATE),
                        "W", spec.voltage().motorId(),
                        "V", battery.id().toString()),
                WorkbenchToolRecipePlan.map("d", SCREWDRIVER),
                result,
                material.id(),
                battery.capacity(),
                spec.voltage().packet());
    }

    private static WorkbenchToolRecipePlan.Recipe mixer(
            ElectricToolCatalog spec,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition hull,
            Set<MaterialPrefix> hullForms,
            EnergyBatteryProfile battery,
            String path,
            String result) {
        if (!forms.contains(MaterialPrefixes.ROD)
                || !hullForms.contains(MaterialPrefixes.CURVED_PLATE)
                || !hullForms.contains(MaterialPrefixes.RING)
                || !hullForms.contains(MaterialPrefixes.PLATE)
                || missingPart(spec.voltage().motorId())) {
            return null;
        }
        return WorkbenchToolRecipePlan.electricFinished(
                path,
                List.of("SSY", "SXW", "hVZ"),
                WorkbenchToolRecipePlan.map(
                        "S", WorkbenchToolRecipePlan.item(
                                material, MaterialPrefixes.ROD),
                        "Y", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.RING),
                        "X", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.CURVED_PLATE),
                        "W", spec.voltage().motorId(),
                        "V", battery.id().toString(),
                        "Z", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.PLATE)),
                WorkbenchToolRecipePlan.map("h", HAMMER),
                result,
                material.id(),
                battery.capacity(),
                spec.voltage().packet());
    }

    private static WorkbenchToolRecipePlan.Recipe handDrill(
            ElectricToolCatalog spec,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition hull,
            Set<MaterialPrefix> hullForms,
            EnergyBatteryProfile battery,
            String path,
            String result) {
        if (!forms.contains(MaterialPrefixes.SCREW)
                || !forms.contains(MaterialPrefixes.ROD)
                || !hullForms.contains(MaterialPrefixes.CURVED_PLATE)
                || !hullForms.contains(MaterialPrefixes.RING)
                || !hullForms.contains(MaterialPrefixes.PLATE)
                || missingPart(spec.voltage().motorId())) {
            return null;
        }
        return WorkbenchToolRecipePlan.electricFinished(
                path,
                List.of("fSY", "TXW", "dVZ"),
                WorkbenchToolRecipePlan.map(
                        "S", WorkbenchToolRecipePlan.item(
                                material, MaterialPrefixes.ROD),
                        "Y", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.RING),
                        "T", WorkbenchToolRecipePlan.item(
                                material, MaterialPrefixes.SCREW),
                        "X", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.CURVED_PLATE),
                        "W", spec.voltage().motorId(),
                        "V", battery.id().toString(),
                        "Z", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.PLATE)),
                WorkbenchToolRecipePlan.map("f", FILE, "d", SCREWDRIVER),
                result,
                material.id(),
                battery.capacity(),
                spec.voltage().packet());
    }

    private static WorkbenchToolRecipePlan.Recipe screwdriver(
            ElectricToolCatalog spec,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition hull,
            Set<MaterialPrefix> hullForms,
            EnergyBatteryProfile battery,
            String path,
            String result) {
        if (!forms.contains(MaterialPrefixes.SCREW)
                || !hullForms.contains(MaterialPrefixes.CURVED_PLATE)
                || !hullForms.contains(MaterialPrefixes.RING)
                || missingPart(spec.voltage().motorId())) {
            return null;
        }
        return WorkbenchToolRecipePlan.electricFinished(
                path,
                List.of("XdA", "TWY", "VYX"),
                WorkbenchToolRecipePlan.map(
                        "A", WorkbenchToolRecipePlan.item(material, headPrefix(spec)),
                        "X", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.CURVED_PLATE),
                        "T", WorkbenchToolRecipePlan.item(
                                material, MaterialPrefixes.SCREW),
                        "W", spec.voltage().motorId(),
                        "Y", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.RING),
                        "V", battery.id().toString()),
                WorkbenchToolRecipePlan.map("d", SCREWDRIVER),
                result,
                material.id(),
                battery.capacity(),
                spec.voltage().packet());
    }

    private static WorkbenchToolRecipePlan.Recipe buzzsaw(
            ElectricToolCatalog spec,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition hull,
            Set<MaterialPrefix> hullForms,
            EnergyBatteryProfile battery,
            String path,
            String result) {
        if (!forms.contains(MaterialPrefixes.SCREW)
                || !hullForms.contains(MaterialPrefixes.CURVED_PLATE)
                || !hullForms.contains(MaterialPrefixes.PLATE)
                || missingPart(spec.voltage().motorId())) {
            return null;
        }
        return WorkbenchToolRecipePlan.electricFinished(
                path,
                List.of("YXV", "TWX", "AdY"),
                WorkbenchToolRecipePlan.map(
                        "A", WorkbenchToolRecipePlan.item(material, headPrefix(spec)),
                        "Y", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.PLATE),
                        "X", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.CURVED_PLATE),
                        "T", WorkbenchToolRecipePlan.item(
                                material, MaterialPrefixes.SCREW),
                        "W", spec.voltage().motorId(),
                        "V", battery.id().toString()),
                WorkbenchToolRecipePlan.map("d", SCREWDRIVER),
                result,
                material.id(),
                battery.capacity(),
                spec.voltage().packet());
    }

    private static WorkbenchToolRecipePlan.Recipe trimmer(
            ElectricToolCatalog spec,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition hull,
            Set<MaterialPrefix> hullForms,
            EnergyBatteryProfile battery,
            String path,
            String result) {
        if (!forms.contains(MaterialPrefixes.SCREW)
                || !hullForms.contains(MaterialPrefixes.CURVED_PLATE)
                || !hullForms.contains(MaterialPrefixes.RING)
                || !hullForms.contains(MaterialPrefixes.LONG_ROD)
                || missingPart(spec.voltage().pistonId())) {
            return null;
        }
        return WorkbenchToolRecipePlan.electricFinished(
                path,
                List.of("XAT", "ZYA", "VWd"),
                WorkbenchToolRecipePlan.map(
                        "A", WorkbenchToolRecipePlan.item(material, headPrefix(spec)),
                        "X", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.CURVED_PLATE),
                        "T", WorkbenchToolRecipePlan.item(
                                material, MaterialPrefixes.SCREW),
                        "Z", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.RING),
                        "Y", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.LONG_ROD),
                        "V", battery.id().toString(),
                        "W", spec.voltage().pistonId()),
                WorkbenchToolRecipePlan.map("d", SCREWDRIVER),
                result,
                material.id(),
                battery.capacity(),
                spec.voltage().packet());
    }

    private static WorkbenchToolRecipePlan.Recipe jackhammer(
            ElectricToolCatalog spec,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition hull,
            Set<MaterialPrefix> hullForms,
            EnergyBatteryProfile battery,
            String path,
            String result) {
        if (!forms.contains(MaterialPrefixes.ROD)
                || !hullForms.contains(MaterialPrefixes.CURVED_PLATE)
                || !hullForms.contains(MaterialPrefixes.SPRING)
                || missingPart(spec.voltage().pistonId())) {
            return null;
        }
        return WorkbenchToolRecipePlan.electricFinished(
                path,
                List.of("SVS", "XWX", "YSY"),
                WorkbenchToolRecipePlan.map(
                        "S", WorkbenchToolRecipePlan.item(
                                material, MaterialPrefixes.ROD),
                        "V", battery.id().toString(),
                        "X", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.CURVED_PLATE),
                        "W", spec.voltage().pistonId(),
                        "Y", WorkbenchToolRecipePlan.item(
                                hull, MaterialPrefixes.SPRING)),
                Map.of(),
                result,
                material.id(),
                battery.capacity(),
                spec.voltage().packet());
    }

    private static MaterialPrefix headPrefix(ElectricToolCatalog spec) {
        return switch (spec.headPrefix()) {
            case "tool_head_drill" -> TOOL_HEAD_DRILL;
            case "tool_head_chainsaw" -> TOOL_HEAD_CHAINSAW;
            case "tool_head_wrench" -> TOOL_HEAD_WRENCH;
            case "tool_head_buzzsaw" -> TOOL_HEAD_BUZZSAW;
            case "tool_head_screwdriver" -> TOOL_HEAD_SCREWDRIVER;
            case "tool_head_sword" -> TOOL_HEAD_SWORD;
            default -> throw new IllegalStateException(spec.headPrefix());
        };
    }

    private static boolean missingPart(String itemId) {
        String path = itemId.substring(itemId.indexOf(':') + 1);
        return TechnologicalPartCatalog.findByPath(path).isEmpty();
    }

    private static void addMechanical(
            List<WorkbenchToolRecipePlan.Recipe> recipes,
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            Map<String, List<MaterialPrefix>> registeredForms,
            Map<String, MaterialDefinition> byId) {
        MaterialDefinition steel = byId.get(STEEL);
        if (steel == null) {
            return;
        }
        Set<MaterialPrefix> steelForms = WorkbenchToolRecipePlan.formsOf(
                steel, registeredForms);
        WorkbenchToolRecipePlan.Recipe drill = mechanicalDrill(
                material, forms, steel, steelForms);
        if (drill != null) {
            recipes.add(drill);
        }
        WorkbenchToolRecipePlan.Recipe chainsaw = mechanicalChainsaw(
                material, forms, steel, steelForms);
        if (chainsaw != null) {
            recipes.add(chainsaw);
        }
    }

    private static WorkbenchToolRecipePlan.Recipe mechanicalDrill(
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition steel,
            Set<MaterialPrefix> steelForms) {
        if (!WorkbenchToolRecipePlan.eligible(
                material, ToolKind.MINING_DRILL_LV)
                || !forms.contains(TOOL_HEAD_DRILL)
                || !forms.contains(MaterialPrefixes.PLATE)
                || !steelForms.contains(MaterialPrefixes.CURVED_PLATE)) {
            return null;
        }
        return WorkbenchToolRecipePlan.finished(
                WorkbenchToolRecipePlan.path(
                        material.id(), "mining_drill_lv/mechanical"),
                List.of("PVP", "PVP", "VhV"),
                WorkbenchToolRecipePlan.map(
                        "P", WorkbenchToolRecipePlan.item(
                                material, MaterialPrefixes.PLATE),
                        "V", WorkbenchToolRecipePlan.item(
                                steel, MaterialPrefixes.CURVED_PLATE)),
                WorkbenchToolRecipePlan.map("h", HAMMER),
                "cruciblecraft:material_mining_drill_lv",
                material.id());
    }

    private static WorkbenchToolRecipePlan.Recipe mechanicalChainsaw(
            MaterialDefinition material,
            Set<MaterialPrefix> forms,
            MaterialDefinition steel,
            Set<MaterialPrefix> steelForms) {
        if (!WorkbenchToolRecipePlan.eligible(material, ToolKind.CHAINSAW_LV)
                || !forms.contains(TOOL_HEAD_CHAINSAW)
                || !forms.contains(CHAIN)
                || !steelForms.contains(MaterialPrefixes.RING)
                || !steelForms.contains(MaterialPrefixes.PLATE)) {
            return null;
        }
        return WorkbenchToolRecipePlan.finished(
                WorkbenchToolRecipePlan.path(
                        material.id(), "chainsaw_lv/mechanical"),
                List.of("WVW", "XhX", "WVW"),
                WorkbenchToolRecipePlan.map(
                        "W", WorkbenchToolRecipePlan.item(
                                steel, MaterialPrefixes.PLATE),
                        "V", WorkbenchToolRecipePlan.item(
                                steel, MaterialPrefixes.RING),
                        "X", WorkbenchToolRecipePlan.item(material, CHAIN)),
                WorkbenchToolRecipePlan.map("h", HAMMER),
                "cruciblecraft:material_chainsaw_lv",
                material.id());
    }
}
