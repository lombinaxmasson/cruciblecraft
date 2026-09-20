package com.masson.cruciblecraft.datagen;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.TechnologicalPartCatalog;
import com.masson.cruciblecraft.content.item.SlicerOperandCatalog;
import com.masson.cruciblecraft.content.item.PressureWasherOperandCatalog;
import com.masson.cruciblecraft.content.item.ToolPatternCatalog;
import com.masson.cruciblecraft.content.mold.CeramicMoldCatalog;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CrucibleCraft.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        generatedImportedGt6("raw_ceramic_crucible");
        generatedImportedGt6("raw_ceramic_mold");
        generatedImportedGt6("coin");
        CeramicMoldCatalog.SHAPED.forEach(variant -> {
            generatedImportedGt6(CeramicMoldCatalog.rawItemId(variant));
            withExistingParent(
                    CeramicMoldCatalog.firedItemId(variant),
                    modLoc("block/ceramic_mold"));
        });
        generatedCc("match");
        generatedCc("programmed_circuit");
        generatedCc("creosote_bucket");
        generatedCc("steam_bucket");
        generatedCc("portable_fluid_tank");
        generatedCc("fluid_cell");
        generatedCc("gas_cell");
        generatedImportedGt6(
                "lead_acid_cell_empty", "battery_cell/lead_acid_empty");
        generatedImportedGt6(
                "lead_acid_cell_filled", "battery_cell/lead_acid_filled");
        generatedImportedGt6(
                "alkaline_cell_empty", "battery_cell/alkaline_empty");
        generatedImportedGt6(
                "alkaline_cell_filled", "battery_cell/alkaline_filled");
        generatedImportedGt6(
                "nickel_cadmium_cell_empty",
                "battery_cell/nickel_cadmium_empty");
        generatedImportedGt6(
                "nickel_cadmium_cell_filled",
                "battery_cell/nickel_cadmium_filled");
        generatedImportedGt6(
                "lithium_cobalt_cell_empty",
                "battery_cell/lithium_cobalt_empty");
        generatedImportedGt6(
                "lithium_cobalt_cell_filled",
                "battery_cell/lithium_cobalt_filled");
        generatedImportedGt6(
                "lithium_manganese_cell_empty",
                "battery_cell/lithium_manganese_empty");
        generatedImportedGt6(
                "lithium_manganese_cell_filled",
                "battery_cell/lithium_manganese_filled");
        generatedCc("pipe_filter_cover");
        generatedCc("pipe_valve_cover");
        generatedImportedGt6("pipe_pump_cover");
        generatedImportedGt6("conveyor_cover");
        generatedImportedGt6("retriever_item_cover");
        generatedImportedGt6("robot_arm_cover");
        generatedImportedGt6("pressure_valve_cover");
        generatedCc("selector_manual_cover");
        generatedImportedGt6("logistics_item_storage_cover");
        generatedImportedGt6("logistics_item_import_cover");
        generatedImportedGt6("logistics_item_export_cover");
        generatedImportedGt6("logistics_fluid_storage_cover");
        generatedImportedGt6("logistics_fluid_import_cover");
        generatedImportedGt6("logistics_fluid_export_cover");
        generatedImportedGt6("logistics_generic_storage_cover");
        generatedImportedGt6("logistics_generic_import_cover");
        generatedImportedGt6("logistics_generic_export_cover");
        generatedImportedGt6("logistics_generic_dump_cover");
        generatedImportedGt6("logistics_display_cpu_logic_cover");
        generatedImportedGt6("logistics_display_cpu_control_cover");
        generatedImportedGt6("logistics_display_cpu_storage_cover");
        generatedImportedGt6("logistics_display_cpu_conversion_cover");
        CoverComponentTiers.entries().forEach(entry ->
                generatedImportedGt6(
                        entry.itemPath(),
                        TechnologicalPartCatalog.findByPath(entry.itemPath())
                                .map(TechnologicalPartCatalog.Part::texture)
                                .orElse(switch (entry.family()) {
                                    case CONVEYOR -> "conveyor_cover";
                                    case ROBOT_ARM -> "robot_arm_cover";
                                    case PUMP -> "compact_electric_pump";
                                })));
        MachineCoverKinds.ITEMS.forEach(entry ->
                generatedImportedGt6(entry.itemPath()));
        generatedCc("unknown_material");
        ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                generatedCc(shape.registryPath()));

        ToolPatternCatalog.DEFINITIONS.forEach(pattern ->
                generatedCc(pattern.registryPath()));
        // layer0 = tinted metal head; later layers stay untinted except
        // wood-handle layer2 (stick / HANDLE_* iconsets).
        toolWithStick("smithing_hammer", "smithing_hammer", "smithing_hammer_overlay");
        toolWithStick("material_pickaxe", "pickaxe", "pickaxe_overlay");
        tool("material_file", "file", "file_overlay", "handle_file", "handle_file_overlay");
        toolWithStick("material_shovel", "shovel", "shovel_overlay");
        toolWithStick("material_axe", "axe", "axe_overlay");
        toolWithStick("material_hoe", "hoe", "hoe_overlay");
        tool("material_sword", "sword", "sword_overlay", "handle_sword", "handle_sword_overlay");
        tool("material_chisel", "chisel", "chisel_overlay", "handle_chisel", "handle_chisel_overlay");
        tool("material_saw", "saw", "saw_overlay", "handle_saw", "handle_saw_overlay");
        tool(
                "material_screwdriver",
                "screwdriver",
                "screwdriver_overlay",
                "handle_screwdriver",
                "handle_screwdriver_overlay");
        tool("material_wrench", "wrench", "wrench_overlay");
        tool(
                "material_monkey_wrench",
                "monkey_wrench",
                "monkey_wrench_overlay");
        tool("material_wire_cutter", "wire_cutter", "wire_cutter_overlay");
        tool("material_knife", "knife", "knife_overlay");
        tool("material_club", "club", "club_overlay");
        toolWithStick("material_spade", "spade", "spade_overlay");
        toolWithStick("material_double_axe", "double_axe", "double_axe_overlay");
        toolWithStick("material_sense", "sense", "sense_overlay");
        toolWithStick("material_plow", "plow", "plow_overlay");
        toolWithStick("material_construction_pick", "construction_pick",
                "construction_pick_overlay");
        toolWithStick("material_gem_pick", "gem_pick", "gem_pick_overlay");
        toolWithStick("material_builder_wand", "builder_wand", "builder_wand_overlay");
        toolWithStick("material_universal_spade", "universal_spade",
                "universal_spade_overlay");
        tool("material_crowbar", "crowbar", "crowbar_overlay");
        tool("material_plunger", "plunger", "plunger_overlay");
        tool("material_scoop", "scoop", "scoop_overlay");
        tool("material_butchery_knife", "butchery_knife",
                "butchery_knife_overlay");
        tool("material_branch_cutter", "branch_cutter",
                "branch_cutter_overlay");
        tool("material_scissors", "scissors", "scissors_overlay");
        tool("material_pincers", "pincers", "pincers_overlay");
        toolWithStick("material_soft_hammer", "soft_hammer", "soft_hammer_overlay");
        tool("material_bending_cylinder", "bending_cylinder",
                "bending_cylinder_overlay");
        tool("material_bending_cylinder_small", "bending_cylinder_small",
                "bending_cylinder_small_overlay");
        tool("material_hand_drill", "hand_drill", "hand_drill_overlay");
        tool("material_rolling_pin", "rolling_pin", "rolling_pin_overlay");
        tool("material_flint_and_tinder", "flint_and_tinder",
                "flint_and_tinder_overlay");
        tool("material_pocket_multitool", "pocket_multitool",
                "pocket_multitool_overlay");
        ModBlocks.electricalConductorBlocks().forEach(holder -> {
            var conductor = holder.get().conductor();
            String specification = conductor.sourceSpecification();
            withExistingParent(
                    SlashItemModels.path(conductor.registryName()),
                    modLoc(
                            "conductor/"
                                    + specification.toLowerCase(
                                            java.util.Locale.ROOT)
                                    + "_item"));
        });
        withExistingParent(
                "lu_fiber_cable",
                modLoc("conductor/lu_fiber_cable_item"));
        for (var rod : com.masson.cruciblecraft.nuclear.ReactorRodCatalog.entries()) {
            withExistingParent(
                            SlashItemModels.path(rod.id().getPath()),
                            mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/gt6_import/reactor_rod"))
                    .texture(
                            "layer1",
                            modLoc("item/gt6_import/reactor_rod_overlay"));
        }
        TechnologicalPartCatalog.parts().forEach(part -> {
            if (CoverComponentTiers.findByItemPath(part.registryPath()).isPresent()) {
                return;
            }
            generatedImportedGt6(part.registryPath(), part.texture());
        });
        SlicerOperandCatalog.operands().forEach(operand ->
                generatedGt6Multiitem(operand.registryPath()));
        PressureWasherOperandCatalog.operands().forEach(operand ->
                generatedGt6Multiitem(operand.registryPath()));
        for (var species : com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies.ALL) {
            withExistingParent("item/" + species.saplingPath(), mcLoc("item/generated"))
                    .texture("layer0", modLoc("block/tree/" + species.id() + "/sapling"));
        }
        withExistingParent("item/tree/rubber_resin", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/tree/rubber_resin"));
        ModBlocks.pipeBlocks().forEach(holder -> {
            var pipe = holder.get().pipe();
            String modelKey = pipe.kind().name().toLowerCase(
                    java.util.Locale.ROOT) + "_" + pipe.textureKey();
            withExistingParent(
                    SlashItemModels.path(pipe.registryName()),
                    modLoc("pipe/" + modelKey + "_item"));
        });
    }

    private void generatedCc(String name) {
        withExistingParent(SlashItemModels.path(name), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name));
    }

    private void generatedImportedGt6(String name) {
        generatedImportedGt6(name, name);
    }

    private void generatedImportedGt6(String name, String texture) {
        withExistingParent(
                        SlashItemModels.path(name), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/gt6_import/" + texture));
    }

    private void generatedGt6Multiitem(String name) {
        withExistingParent("item/" + name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name));
    }

    private void tool(String name, String... textureLayers) {
        var model = withExistingParent(name, mcLoc("item/handheld"));
        for (int i = 0; i < textureLayers.length; i++) {
            model.texture("layer" + i, modLoc("item/tool/" + textureLayers[i]));
        }
    }

    /** GT6 MultiItemTool: tool-head iconset plus {@code OP.stick} (rod + overlay). */
    private void toolWithStick(String name, String... headLayers) {
        var model = withExistingParent(name, mcLoc("item/handheld"));
        int index = 0;
        for (; index < headLayers.length; index++) {
            model.texture(
                    "layer" + index,
                    modLoc("item/tool/" + headLayers[index]));
        }
        model.texture("layer" + index, modLoc("item/material/rod"));
        model.texture("layer" + (index + 1), modLoc("item/material/rod_overlay"));
    }
}
