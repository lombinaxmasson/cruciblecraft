package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.AnvilHosts;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** tintindex 0 only. GT6 anvil overlay icons are empty and omitted. */
public final class AnvilBlockColor {
    private AnvilBlockColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        if (level != null
                && pos != null
                && level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) {
            return AnvilHosts.colorRgb(anvil.materialId());
        }
        return AnvilHosts.bakedMaterial(state)
                .map(AnvilHosts::colorRgb)
                .orElse(AnvilHosts.STONE_COLOR);
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        Block block = Block.byItem(stack.getItem());
        if (block instanceof MteInPlaceBlock inplace && AnvilHosts.isAnvil(inplace.spec())) {
            return AnvilHosts.colorRgb(AnvilHosts.materialId(inplace.spec()));
        }
        return AnvilHosts.STONE_COLOR;
    }

    public static Block[] tintedBlocks() {
        java.util.ArrayList<Block> blocks = new java.util.ArrayList<>();
        ModBlocks.mteInPlaceBlocksById().values().forEach(holder -> {
            if (AnvilHosts.isAnvil(holder.get().spec())) {
                blocks.add(holder.get());
            }
        });
        return blocks.toArray(Block[]::new);
    }
}
