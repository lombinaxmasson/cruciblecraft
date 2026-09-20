package com.masson.cruciblecraft.content.block;

import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.PressureWasherOperandCatalog;
import com.masson.cruciblecraft.registry.ModBlockTags;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code OD.beamWood} identity: vanilla stripped logs/woods/stems plus
 * GT-tree beam blocks. Axe strip uses this mapping. Pressure-washer datagen
 * uses it for extra 1.16+ woods/stems and GT logs; 1.7.10 vanilla logs stay
 * on the GT6 mill compact family with the same water input.
 */
public final class WoodDebark {
    private WoodDebark() {}

    public static final int WASHER_DURATION = 64;
    public static final long WASHER_EUT = 16L;
    public static final int WASHER_WATER_MB = 200;
    public static final int COKE_DURATION = 3600;

    public static final List<VanillaPair> VANILLA_PAIRS = List.of(
            pair("oak_log", "stripped_oak_log"),
            pair("oak_wood", "stripped_oak_wood"),
            pair("spruce_log", "stripped_spruce_log"),
            pair("spruce_wood", "stripped_spruce_wood"),
            pair("birch_log", "stripped_birch_log"),
            pair("birch_wood", "stripped_birch_wood"),
            pair("jungle_log", "stripped_jungle_log"),
            pair("jungle_wood", "stripped_jungle_wood"),
            pair("acacia_log", "stripped_acacia_log"),
            pair("acacia_wood", "stripped_acacia_wood"),
            pair("dark_oak_log", "stripped_dark_oak_log"),
            pair("dark_oak_wood", "stripped_dark_oak_wood"),
            pair("mangrove_log", "stripped_mangrove_log"),
            pair("mangrove_wood", "stripped_mangrove_wood"),
            pair("cherry_log", "stripped_cherry_log"),
            pair("cherry_wood", "stripped_cherry_wood"),
            pair("bamboo_block", "stripped_bamboo_block"),
            pair("crimson_stem", "stripped_crimson_stem"),
            pair("crimson_hyphae", "stripped_crimson_hyphae"),
            pair("warped_stem", "stripped_warped_stem"),
            pair("warped_hyphae", "stripped_warped_hyphae"));

    /**
     * Vanilla 1.7.10 logs already selected in the GT6 pressure-washer compact
     * family with the same water input. Datagen must not emit a second row.
     */
    public static final Set<ResourceLocation> GT6_PRESSURE_WASHER_VANILLA_LOGS = Set.of(
            vanilla("oak_log"),
            vanilla("spruce_log"),
            vanilla("birch_log"),
            vanilla("jungle_log"),
            vanilla("acacia_log"),
            vanilla("dark_oak_log"));

    public static int extraPressureWasherWoodRows() {
        int vanilla = 0;
        for (VanillaPair pair : VANILLA_PAIRS) {
            if (!GT6_PRESSURE_WASHER_VANILLA_LOGS.contains(pair.log())) {
                vanilla++;
            }
        }
        return vanilla + GtTreeSpecies.ALL.size();
    }

    /** Pillar stripped forms that get their own coke-oven row. */
    public static final List<ResourceLocation> VANILLA_BEAM_COKE_INPUTS = List.of(
            vanilla("stripped_oak_log"),
            vanilla("stripped_spruce_log"),
            vanilla("stripped_birch_log"),
            vanilla("stripped_jungle_log"),
            vanilla("stripped_acacia_log"),
            vanilla("stripped_dark_oak_log"),
            vanilla("stripped_mangrove_log"),
            vanilla("stripped_cherry_log"),
            vanilla("stripped_bamboo_block"),
            vanilla("stripped_crimson_stem"),
            vanilla("stripped_warped_stem"));

    public static boolean isDebarkSource(BlockState state) {
        if (state.is(ModBlockTags.WOODEN_BEAMS)) {
            return false;
        }
        return state.getBlock() instanceof GtTreeLogBlock
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.BAMBOO_BLOCKS);
    }

    public static ItemStack axeBarkDrop(BlockState original) {
        if (!isDebarkSource(original)) {
            return ItemStack.EMPTY;
        }
        if (original.getBlock() instanceof GtTreeLogBlock log
                && log.species() == GtTreeSpecies.CINNAMON) {
            return cinnamonBark(2);
        }
        return barkDust(1);
    }

    public static ItemStack pressureWasherBark(GtTreeSpecies species) {
        if (species == GtTreeSpecies.CINNAMON) {
            return cinnamonBark(1);
        }
        return barkDust(1);
    }

    public static ItemStack barkDust(int count) {
        return MaterialLookup.stack("bark", MaterialPrefixes.DUST, count);
    }

    public static ItemStack cinnamonBark(int count) {
        PressureWasherOperandCatalog.Operand operand =
                PressureWasherOperandCatalog.operands().getFirst();
        return new ItemStack(
                ModItems.pressureWasherOperandItemsById()
                        .get(operand.id())
                        .get(),
                count);
    }

    public static Block beamBlock(GtTreeSpecies species) {
        return ModBlocks.treeBeam(species).get();
    }

    /** GT6 {@code BeamEntry} charcoal count. */
    public static int beamCharcoal(GtTreeSpecies species) {
        return switch (species) {
            case WILLOW -> 2;
            default -> 1;
        };
    }

    /** GT6 {@code BeamEntry} creosote mB. */
    public static int beamCreosoteMb(GtTreeSpecies species) {
        return switch (species) {
            case RUBBER -> 300;
            case MAPLE -> 350;
            case WILLOW, RAINBOWOOD -> 400;
            default -> 200;
        };
    }

    /** Default GT6 {@code new BeamEntry(beam, plank)} creosote. */
    public static int vanillaBeamCreosoteMb() {
        return 200;
    }

    public static int vanillaBeamCharcoal() {
        return 1;
    }

    public static Item requireItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("Missing wood item " + id);
        }
        return item;
    }

    private static VanillaPair pair(String log, String beam) {
        return new VanillaPair(vanilla(log), vanilla(beam));
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    public record VanillaPair(ResourceLocation log, ResourceLocation beam) {}
}
