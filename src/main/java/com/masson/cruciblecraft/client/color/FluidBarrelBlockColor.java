package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.FluidBarrelBlock;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** tintindex 0 is the barrel body. Overlay faces stay white. */
public final class FluidBarrelBlockColor {
    private FluidBarrelBlockColor() {}

    public static int blockColor(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
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
        return ModBlocks.fluidBarrelBlocks().stream()
                .map(holder -> (Block) holder.get())
                .toArray(Block[]::new);
    }

    private static int colorFor(Block block) {
        if (!(block instanceof FluidBarrelBlock barrel)
                || barrel.profile().materialId().isBlank()) {
            return 0xFFFFFFFF;
        }
        return MaterialCatalog.find(barrel.profile().materialId())
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElse(0xFFFFFFFF);
    }
}
