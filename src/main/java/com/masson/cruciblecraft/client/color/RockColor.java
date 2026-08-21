package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Tintindex-0 color for placeable material rocks. */
public final class RockColor {
    private RockColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        if (!(state.getBlock() instanceof RockBlock rock)) {
            return 0xFFFFFFFF;
        }
        return MaterialCatalog.find(rock.materialId())
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElse(0xFFFFFFFF);
    }

    public static Block[] rockBlocks() {
        return ModBlocks.rockBlocks().stream()
                .map(holder -> (Block) holder.get())
                .toArray(Block[]::new);
    }
}
