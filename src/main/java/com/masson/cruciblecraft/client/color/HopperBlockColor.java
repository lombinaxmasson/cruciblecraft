package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.DustFunnelBlock;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** tintindex 0 only; overlay faces stay white. */
public final class HopperBlockColor {
    private HopperBlockColor() {}

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
        ModBlocks.hopperBlocks().forEach(holder -> blocks.add(holder.get()));
        blocks.add(ModBlocks.STEEL_DUST_FUNNEL.get());
        return blocks.toArray(Block[]::new);
    }

    private static int colorFor(Block block) {
        String materialId = "steel";
        if (block instanceof HopperBlock hopper) {
            materialId = hopper.variant().materialPath();
        } else if (block instanceof DustFunnelBlock) {
            materialId = "steel";
        }
        return MaterialCatalog.find(materialId)
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElse(0xFFCD7F32);
    }
}
