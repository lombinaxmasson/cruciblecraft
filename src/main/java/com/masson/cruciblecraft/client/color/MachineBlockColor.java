package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.energy.converter.EnergyConverterTierCatalog;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerTierCatalog;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
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
        blocks.add(ModBlocks.MORTAR.get());
        blocks.add(ModBlocks.LASER_ENGRAVER.get());
        blocks.add(ModBlocks.AUTOMATIC_HAMMER.get());
        blocks.add(ModBlocks.STEEL_AUTOMATIC_HAMMER.get());
        blocks.add(ModBlocks.TITANIUM_AUTOMATIC_HAMMER.get());
        blocks.add(ModBlocks.TUNGSTENSTEEL_AUTOMATIC_HAMMER.get());
        blocks.add(ModBlocks.FUSION_REACTOR.get());
        blocks.add(ModBlocks.LARGE_HEAT_EXCHANGER.get());
        blocks.add(ModBlocks.BEDROCK_DRILL.get());
        blocks.add(ModBlocks.BEDROCK_DRILL_HEAD.get());
        ModBlocks.quantumEnergizerBlocksById().values()
                .forEach(block -> blocks.add(block.get()));
        ModBlocks.longDistanceTransformerBlocksById().values()
                .forEach(block -> blocks.add(block.get()));
        blocks.add(ModBlocks.REACTOR_CORE_1X1.get());
        blocks.add(ModBlocks.REACTOR_CORE_2X2.get());
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
        var catalog = MachineTierCatalog.findByPath(path);
        if (catalog != null) {
            return catalogMaterialId(catalog.tierBand().materialId());
        }
        if (path.startsWith("tungstensteel_")) {
            return "tungstensteel";
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
        return switch (path) {
            case "fusion_reactor" -> "steel_galvanized";
            case "large_heat_exchanger" -> "tungsten";
            case "bedrock_drill", "bedrock_drill_head" -> "titanium";
            case "reactor_core_1x1", "reactor_core_2x2" -> "lead";
            case "laser_engraver" -> "steel_galvanized";
            case "bath" -> "stainless_steel";
            default -> {
                for (var profile : com.masson.cruciblecraft.energy.longdistance
                        .LongDistanceTransformerCatalog.endpoints()) {
                    if (path.equals(profile.id().getPath())) {
                        yield profile.material();
                    }
                }
                yield path.startsWith("quantum_energizer_omega")
                        ? "neutronium"
                        : path.startsWith("quantum_energizer")
                                ? "osmiridium"
                                : "bronze";
            }
        };
    }

    private static String catalogMaterialId(String materialId) {
        var id = net.minecraft.resources.ResourceLocation.tryParse(materialId);
        if (id != null && CrucibleCraft.MODID.equals(id.getNamespace())) {
            return id.getPath();
        }
        return materialId;
    }

    static String machineTextureId(String id) {
        return com.masson.cruciblecraft.machine.processing
                .MachineTextureProfiles.textureId(id);
    }

    static boolean hasMachineTextures(String textureId) {
        return com.masson.cruciblecraft.machine.processing
                .MachineTextureProfiles.hasMachineTextures(textureId);
    }
}
