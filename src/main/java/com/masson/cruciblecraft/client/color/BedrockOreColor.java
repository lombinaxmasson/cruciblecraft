package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.blockentity.BedrockOreBlockEntity;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Tintindex 0 flecks for PrefixBlock-style bedrock / small ores. */
public final class BedrockOreColor {
    private BedrockOreColor() {}

    public static int blockColor(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        if (level != null
                && pos != null
                && level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore
                && ore.hasMaterial()) {
            return colorFor(ore.materialId());
        }
        return 0xFFFFFFFF;
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        String materialId = stack.getOrDefault(ModComponents.ORE_MATERIAL.get(), "");
        if (!materialId.isEmpty()) {
            return colorFor(materialId);
        }
        return 0xFFFFFFFF;
    }

    public static Block[] blocks() {
        return new Block[] {
            ModBlocks.GT_BEDROCK_ORE.get(),
            ModBlocks.GT_SMALL_BEDROCK_ORE.get(),
            ModBlocks.GT_SMALL_ORE.get(),
            ModBlocks.GT_HOSTED_ORE.get(),
            ModBlocks.GT_BROKEN_ORE.get()
        };
    }

    public static Item[] items() {
        return new Item[] {
            ModBlocks.GT_BEDROCK_ORE.get().asItem(),
            ModBlocks.GT_SMALL_BEDROCK_ORE.get().asItem(),
            ModBlocks.GT_SMALL_ORE.get().asItem(),
            ModBlocks.GT_HOSTED_ORE.get().asItem(),
            ModBlocks.GT_BROKEN_ORE.get().asItem()
        };
    }

    static int colorFor(String materialId) {
        return MaterialCatalog.find(materialId)
                .map(MaterialOreColor::colorFor)
                .orElse(0xFFFFFFFF);
    }
}
