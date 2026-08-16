package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.MaterialStorageBlock;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Tintindex-0 color for placeable material storage cubes. */
public final class MaterialStorageColor {
    private MaterialStorageColor() {}

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

    public static Block[] storageBlocks() {
        return ModBlocks.storageBlocks().stream()
                .map(holder -> (Block) holder.get())
                .toArray(Block[]::new);
    }

    private static int colorFor(Block block) {
        if (!(block instanceof MaterialStorageBlock storage)) {
            return 0xFFFFFFFF;
        }
        return MaterialCatalog.find(storage.materialId())
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElse(0xFFFFFFFF);
    }
}
