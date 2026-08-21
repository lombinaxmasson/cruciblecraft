package com.masson.cruciblecraft.datagen;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.ToolPatternCatalog;
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
        generatedCc("raw_ceramic_crucible");
        generatedCc("raw_ceramic_mold");
        generatedCc("raw_ingot_mold");
        generatedCc("raw_plate_mold");
        generatedCc("raw_rod_mold");
        generatedCc("raw_bolt_mold");
        withExistingParent("ingot_mold", modLoc("block/ceramic_mold"));
        withExistingParent("plate_mold", modLoc("block/ceramic_mold"));
        withExistingParent("rod_mold", modLoc("block/ceramic_mold"));
        withExistingParent("bolt_mold", modLoc("block/ceramic_mold"));
        generatedCc("coal_coke");
        generatedCc("match");
        generatedCc("bronze_double_machine_casing");
        generatedCc("steel_double_machine_casing");
        generatedCc("titanium_double_machine_casing");
        generatedCc("steel_galvanized_machine_casing");
        generatedCc("aluminium_machine_casing");
        generatedCc("stainless_steel_machine_casing");
        generatedCc("creosote_bucket");
        generatedCc("steam_bucket");
        generatedCc("portable_fluid_tank");
        generatedCc("fluid_cell");
        generatedCc("gas_cell");
        generatedCc("pipe_filter_cover");
        generatedCc("pipe_valve_cover");
        generatedCc("pipe_pump_cover");
        generatedCc("conveyor_cover");
        generatedCc("retriever_item_cover");
        generatedCc("robot_arm_cover");
        generatedCc("pressure_valve_cover");
        generatedCc("selector_manual_cover");
        generatedCc("unknown_material");
        ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                generatedCc(shape.registryPath()));
        ToolPatternCatalog.DEFINITIONS.forEach(pattern ->
                generatedCc(pattern.registryPath()));
        // layer0 = tinted metal head; later layers stay untinted (handles/overlays).
        tool("flint_knife", "knife", "knife_overlay");
        tool("smithing_hammer", "smithing_hammer", "smithing_hammer_overlay");
        tool("material_pickaxe", "pickaxe", "pickaxe_overlay");
        tool("material_file", "file", "file_overlay", "handle_file", "handle_file_overlay");
        tool("material_shovel", "shovel", "shovel_overlay");
        tool("material_axe", "axe", "axe_overlay");
        tool("material_hoe", "hoe", "hoe_overlay");
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
        tool("material_wire_cutter", "wire_cutter", "wire_cutter_overlay");
        ModBlocks.electricalConductorBlocks().forEach(holder -> {
            var conductor = holder.get().conductor();
            String specification = conductor.sourceSpecification();
            withExistingParent(
                    conductor.registryName(),
                    modLoc(
                            "conductor/"
                                    + specification.toLowerCase(
                                            java.util.Locale.ROOT)
                                    + "_item"));
        });
        ModBlocks.pipeBlocks().forEach(holder -> {
            var pipe = holder.get().pipe();
            String modelKey = pipe.kind().name().toLowerCase(
                    java.util.Locale.ROOT) + "_" + pipe.width();
            withExistingParent(
                    pipe.registryName(),
                    modLoc("pipe/" + modelKey + "_item"));
        });
    }

    private void generatedCc(String name) {
        withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name));
    }

    private void tool(String name, String... textureLayers) {
        var model = withExistingParent(name, mcLoc("item/handheld"));
        for (int i = 0; i < textureLayers.length; i++) {
            model.texture("layer" + i, modLoc("item/tool/" + textureLayers[i]));
        }
    }
}
