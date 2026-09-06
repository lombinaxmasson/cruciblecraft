package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.energy.converter.EnergyConverterKindCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterTierCatalog;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerTierCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Casing tint for dual-layer GT6-style machine cubes (tintindex 0 only). */
public final class MachineBlockColor {
    private MachineBlockColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        return colorFor(state.getBlock());
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        return colorFor(Block.byItem(stack.getItem()));
    }

    public static Block[] tintedBlocks() {
        java.util.ArrayList<Block> blocks = new java.util.ArrayList<>();
        java.util.Collections.addAll(blocks, ModBlocks.configuredProcessingBlocks());
        java.util.Collections.addAll(blocks, ModBlocks.converterBlockArray());
        blocks.add(ModBlocks.ROTATIONAL_GEARBOX.get());
        blocks.add(ModBlocks.COKE_OVEN.get());
        blocks.add(ModBlocks.LARGE_BOILER.get());
        blocks.add(ModBlocks.TANK_3X3X3.get());
        blocks.add(ModBlocks.LARGE_CRUCIBLE.get());
        blocks.add(ModBlocks.MORTAR.get());
        java.util.ArrayList<Block> tinted = new java.util.ArrayList<>(
                blocks.stream()
                        .filter(MachineBlockColor::usesDualLayerTextures)
                        .toList());
        java.util.Collections.addAll(
                tinted, ModBlocks.transformerBlockArray());
        return tinted.toArray(Block[]::new);
    }

    private static boolean usesDualLayerTextures(Block block) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null
                && CrucibleCraft.MODID.equals(id.getNamespace())
                && hasMachineTextures(machineTextureId(id.getPath()));
    }

    private static int colorFor(Block block) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        String materialId = "bronze";
        if (id != null && CrucibleCraft.MODID.equals(id.getNamespace())) {
            materialId = casingMaterialId(id.getPath());
        }
        return MaterialLookup.byId(materialId)
                .map(material -> 0xFF000000 | material.colorRgb())
                .orElse(0xFFCD7F32);
    }

    /** GT6 loader casing material identity for the machine texture family. */
    public static String casingMaterialId(String path) {
        var transformer = EnergyTransformerTierCatalog.findByPath(path);
        if (transformer != null) {
            return transformer.material();
        }
        var converter = EnergyConverterTierCatalog.findByPath(path);
        if (converter != null) {
            return converter.material();
        }
        if (path.startsWith("titanium_")) {
            return "titanium";
        }
        if (path.startsWith("aluminium_")) {
            return "aluminium";
        }
        if (path.startsWith("stainless_steel_")) {
            return "stainless_steel";
        }
        if (path.startsWith("invar_")) {
            return "invar";
        }
        if (path.startsWith("steel_")) {
            return "steel";
        }
        return "bronze";
    }

    static String machineTextureId(String id) {
        var converter = EnergyConverterTierCatalog.findByPath(id);
        if (converter != null) {
            return EnergyConverterKindCatalog.require(converter.kindId())
                    .textureProfile();
        }
        String profile = com.masson.cruciblecraft.machine.processing
                .MachineTierCatalog.textureProfile(id);
        if (!profile.equals(id)) {
            return profile;
        }
        return switch (id) {
            case "steel_centrifuge", "titanium_centrifuge" -> "centrifuge";
            case "steel_sifter", "titanium_sifter" -> "sifter";
            case "steel_lathe", "titanium_lathe" -> "lathe";
            case "steel_rollingmill", "titanium_rollingmill" -> "rollingmill";
            case "steel_wiremill", "titanium_wiremill" -> "wiremill";
            case "steel_shredder", "titanium_shredder" -> "shredder";
            case "steel_press", "titanium_press" -> "press";
            case "aluminium_electrolyzer", "stainless_steel_electrolyzer" ->
                    "electrolyzer";
            case "invar_distillery", "titanium_distillery" -> "distillery";
            case "distillation_tower" -> "distillery";
            case "large_boiler" -> "boiler";
            case "large_crucible" -> "coke_oven";
            case "drying", "invar_drying", "titanium_drying" -> "dryer";
            case "invar_smelter", "titanium_smelter" -> "smelter";
            default -> profile;
        };
    }

    static boolean hasMachineTextures(String textureId) {
        return switch (textureId) {
            case "large_centrifuge",
                    "sluice",
                    "bath",
                    "centrifuge",
                    "shredder",
                    "sifter",
                    "smelter",
                    "extruder",
                    "cutter",
                    "lathe",
                    "rollingmill",
                    "rollbender",
                    "bender",
                    "wiremill",
                    "assembler",
                    "welder",
                    "press",
                    "electrolyzer",
                    "mixer",
                    "distillery",
                    "autoclave",
                    "dryer",
                    "compressor",
                    "generifier",
                    "electric_motor",
                    "rotational_gearbox",
                    "fuel_engine",
                    "burning_gas_generator",
                    "burning_box_solid",
                    "burning_box_brick",
                    "burning_box_liquid",
                    "burning_box_fluid_bed",
                    "boiler",
                    "tank_3x3x3",
                    "mortar",
                    "coke_oven",
                    "bronze_boiler",
                    "bronze_crusher",
                    "bronze_dynamo",
                    "bronze_steam_engine" -> true;
            default -> false;
        };
    }
}
