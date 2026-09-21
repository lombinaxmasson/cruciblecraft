package com.masson.cruciblecraft.content.multiblock;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** GT6 18040–18045 large coils plus the four coil-host controller identities. */
public final class CoilHosts {
    public static final ResourceLocation COPPER = id("multiblock/large_copper_coil");
    public static final ResourceLocation NIOBIUM_TITANIUM =
            id("multiblock/large_niobium_titanium_coil");
    public static final ResourceLocation NICHROME = id("multiblock/large_nichrome_coil");
    public static final ResourceLocation CARBORUNDUM =
            id("multiblock/large_carborundum_coil");
    public static final ResourceLocation OSMIUM = id("multiblock/large_osmium_coil");
    public static final ResourceLocation IRIDIUM = id("multiblock/large_iridium_coil");

    public static final ResourceLocation DENSE_STAINLESS =
            id("multiblock/dense_stainless_steel_wall");
    public static final ResourceLocation DENSE_TITANIUM =
            id("multiblock/dense_titanium_wall");
    public static final ResourceLocation DENSE_TUNGSTENSTEEL =
            id("multiblock/dense_tungstensteel_wall");
    public static final ResourceLocation DENSE_ADAMANTIUM =
            id("multiblock/dense_adamantium_wall");
    public static final ResourceLocation DENSE_LEAD = id("multiblock/dense_lead_wall");
    public static final ResourceLocation DENSE_GALVANIZED =
            id("multiblock/dense_galvanized_steel_wall");
    public static final ResourceLocation DENSE_STEEL = id("multiblock/dense_steel_wall");
    public static final ResourceLocation TUNGSTEN_WALL = id("tungsten/wall");
    public static final ResourceLocation LIGHTNING_ROD_PART =
            id("steel_galvanized/lightning_rod");

    public static final int COPPER_META = 18040;
    public static final int NIOBIUM_TITANIUM_META = 18041;
    public static final int NICHROME_META = 18042;
    public static final int CARBORUNDUM_META = 18043;
    public static final int OSMIUM_META = 18044;
    public static final int IRIDIUM_META = 18045;
    public static final int LIGHTNING_ROD_PART_META = 18104;
    public static final int DENSE_LEAD_META = 18031;
    public static final int DENSE_GALVANIZED_META = 18028;

    private CoilHosts() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }

    public static Block block(ResourceLocation id) {
        var holder = ModBlocks.mteInPlaceBlocksById().get(id);
        if (holder == null) {
            throw new IllegalStateException("Missing coil-host block " + id);
        }
        return holder.get();
    }

    public static boolean isCoil(Block block) {
        return block == block(COPPER)
                || block == block(NIOBIUM_TITANIUM)
                || block == block(NICHROME)
                || block == block(CARBORUNDUM)
                || block == block(OSMIUM)
                || block == block(IRIDIUM);
    }

    public static boolean isCoilMeta(int meta) {
        return meta >= COPPER_META && meta <= IRIDIUM_META;
    }

    public static String tintMaterial(String path) {
        if (path == null) {
            return null;
        }
        return switch (path) {
            case "multiblock/large_copper_coil" -> "copper";
            case "multiblock/large_niobium_titanium_coil" -> "niobium_titanium";
            case "multiblock/large_nichrome_coil" -> "nichrome";
            case "multiblock/large_carborundum_coil" -> "carborundum";
            case "multiblock/large_osmium_coil" -> "osmium";
            case "multiblock/large_iridium_coil" -> "iridium";
            case "stainless_steel/dynamo_main_housing" -> "stainless_steel";
            case "titanium/dynamo_main_housing" -> "titanium";
            case "tungstensteel/dynamo_main_housing" -> "tungstensteel";
            case "adamantium/dynamo_main_housing" -> "adamantium";
            case "tungsten/lightning_rod_electric_output" -> "tungsten";
            case "steel_galvanized/lightning_rod" -> "steel_galvanized";
            case "steel_galvanized/von_da_graagg_generator" -> "steel_galvanized";
            case "lead/large_matter_fabricator" -> "lead";
            default -> null;
        };
    }
}
