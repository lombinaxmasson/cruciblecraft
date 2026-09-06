package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.MaterialCasingBlock;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Tintindex-0 color for placeable machine casings. */
public final class MaterialCasingColor {
    private MaterialCasingColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        if (!(state.getBlock() instanceof MaterialCasingBlock casing)) {
            return 0xFFFFFFFF;
        }
        return MaterialCatalog.find(casing.materialId())
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElse(0xFFFFFFFF);
    }

    public static Block[] casingBlocks() {
        return ModBlocks.casingBlocks().stream()
                .map(holder -> (Block) holder.get())
                .toArray(Block[]::new);
    }
}
