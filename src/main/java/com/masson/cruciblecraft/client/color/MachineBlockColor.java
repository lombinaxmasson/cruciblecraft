package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
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
        blocks.add(ModBlocks.ELECTRIC_MOTOR.get());
        blocks.add(ModBlocks.ROTATIONAL_GEARBOX.get());
        blocks.add(ModBlocks.FUEL_ENGINE.get());
        blocks.add(ModBlocks.BURNING_GAS_GENERATOR.get());
        return blocks.stream()
                .filter(MachineBlockColor::usesDualLayerTextures)
                .toArray(Block[]::new);
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
            materialId = casingMaterial(id.getPath());
        }
        return MaterialLookup.byId(materialId)
                .map(material -> 0xFF000000 | material.colorRgb())
                .orElse(0xFFCD7F32);
    }

    private static String casingMaterial(String path) {
        if (path.startsWith("titanium_")) {
            return "titanium";
        }
        if (path.startsWith("aluminium_")) {
            return "aluminium";
        }
        if (path.startsWith("stainless_steel_")) {
            return "stainless_steel";
        }
        if (path.startsWith("steel_")) {
            return "steel";
        }
        return "bronze";
    }

    static String machineTextureId(String id) {
        return switch (id) {
            case "steel_centrifuge", "titanium_centrifuge" -> "centrifuge";
            case "steel_sifter", "titanium_sifter" -> "sifter";
            case "aluminium_electrolyzer", "stainless_steel_electrolyzer" ->
                    "electrolyzer";
            case "drying" -> "dryer";
            default -> id;
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
                    "burning_gas_generator" -> true;
            default -> false;
        };
    }
}
