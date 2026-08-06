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
        generated("raw_ceramic_crucible", "clay_ball");
        generated("raw_ceramic_mold", "clay_ball");
        generated("raw_ingot_mold", "clay_ball");
        generated("raw_plate_mold", "clay_ball");
        generated("raw_rod_mold", "clay_ball");
        generated("raw_bolt_mold", "clay_ball");
        withExistingParent("ingot_mold", modLoc("block/ceramic_mold"));
        withExistingParent("plate_mold", modLoc("block/ceramic_mold"));
        withExistingParent("rod_mold", modLoc("block/ceramic_mold"));
        withExistingParent("bolt_mold", modLoc("block/ceramic_mold"));
        generated("coal_coke", "coal");
        generated("match", "stick");
        generated("bronze_double_machine_casing", "copper_ingot");
        generated("steel_double_machine_casing", "iron_ingot");
        generated("titanium_double_machine_casing", "iron_ingot");
        generated("steel_galvanized_machine_casing", "iron_ingot");
        generated("aluminium_machine_casing", "iron_ingot");
        generated("stainless_steel_machine_casing", "iron_ingot");
        generated("creosote_bucket", "water_bucket");
        generated("portable_fluid_tank", "bucket");
        generated("fluid_cell", "glass_bottle");
        generated("gas_cell", "experience_bottle");
        generated("pipe_filter_cover", "hopper");
        generated("pipe_valve_cover", "repeater");
        generated("pipe_pump_cover", "redstone");
        generated("unknown_material", "barrier");
        ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                generated(shape.registryPath(), "iron_nugget"));
        ToolPatternCatalog.DEFINITIONS.forEach(pattern ->
                generated(pattern.registryPath(), "paper"));
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
        ModBlocks.electricalConductorBlocks().forEach(holder -> {
            var conductor = holder.get().conductor();
            withExistingParent(
                    conductor.registryName(),
                    modLoc(
                            "conductor/"
                                    + conductor.sourceSpecification()
                                            .toLowerCase(java.util.Locale.ROOT)
                                    + "_core"));
        });
        ModBlocks.pipeBlocks().forEach(holder -> {
            var pipe = holder.get().pipe();
            String modelKey = pipe.kind().name().toLowerCase(
                    java.util.Locale.ROOT) + "_" + pipe.width();
            withExistingParent(
                    pipe.registryName(),
                    modLoc("pipe/" + modelKey + "_core"));
        });
    }

    private void generated(String name, String vanillaTexture) {
        withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/" + vanillaTexture));
    }

    private void tool(String name, String... textureLayers) {
        var model = withExistingParent(name, mcLoc("item/handheld"));
        for (int i = 0; i < textureLayers.length; i++) {
            model.texture("layer" + i, modLoc("item/tool/" + textureLayers[i]));
        }
    }
}
