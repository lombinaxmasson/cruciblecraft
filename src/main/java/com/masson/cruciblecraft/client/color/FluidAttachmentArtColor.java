package com.masson.cruciblecraft.client.color;

import java.util.ArrayList;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.mte.MteFluidAttachmentProfile;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Material tint for the GT6 colored layer; overlay textures stay white. */
public final class FluidAttachmentArtColor {
    private FluidAttachmentArtColor() {}

    public static int blockColor(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            int tintIndex) {
        return tintIndex == 0 ? colorFor(state.getBlock()) : 0xFFFFFFFF;
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        return tintIndex == 0
                ? colorFor(Block.byItem(stack.getItem()))
                : 0xFFFFFFFF;
    }

    public static Block[] tintedBlocks() {
        ArrayList<Block> blocks = new ArrayList<>();
        ModBlocks.mteInPlaceBlocksById().values().forEach(holder -> {
            if (MteFluidAttachmentProfile.contains(holder.get().spec())) {
                blocks.add(holder.get());
            }
        });
        return blocks.toArray(Block[]::new);
    }

    private static int colorFor(Block block) {
        if (!(block instanceof MteInPlaceBlock inplace)
                || !MteFluidAttachmentProfile.contains(inplace.spec())) {
            return 0xFFFFFFFF;
        }
        String materialId =
                MteFluidAttachmentProfile.require(inplace.spec()).materialId();
        return MaterialCatalog.find(materialId)
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElse(0xFFCD7F32);
    }
}
