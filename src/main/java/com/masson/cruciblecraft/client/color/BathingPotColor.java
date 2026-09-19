package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.mte.BathingPotRuntime;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** tintindex 0 only; overlay faces stay white. */
public final class BathingPotColor {
    private BathingPotColor() {}

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
        return ModBlocks.mteInPlaceBlocksById().values().stream()
                .map(holder -> holder.get())
                .filter(block -> BathingPotRuntime.hosts(block.spec()))
                .toArray(Block[]::new);
    }

    static String materialId(MteInPlaceSpec spec) {
        return BathingPotRuntime.materialId(spec);
    }

    private static int colorFor(Block block) {
        if (!(block instanceof MteInPlaceBlock inplace)
                || !BathingPotRuntime.hosts(inplace.spec())) {
            return 0xFFFFFFFF;
        }
        return MaterialCatalog.find(materialId(inplace.spec()))
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElse(0xFFCD7F32);
    }
}
