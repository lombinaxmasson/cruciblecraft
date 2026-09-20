package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.MaterialPlateStorageBlock;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Tintindex-0 color for placeable plate storage blocks. Overlay stays white. */
public final class MaterialPlateStorageColor {
    private MaterialPlateStorageColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        if (!(state.getBlock() instanceof MaterialPlateStorageBlock plate)) {
            return 0xFFFFFFFF;
        }
        return MaterialCatalog.find(plate.materialId())
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElse(0xFFFFFFFF);
    }

    public static Block[] plateStorageBlocks() {
        return ModBlocks.plateStorageBlocks().stream()
                .map(holder -> (Block) holder.get())
                .toArray(Block[]::new);
    }
}
