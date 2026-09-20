package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 material tints for the fixed early-game ceramic and brick devices. */
public final class Gt6OpeningBlockColor {
    private static final int GT6_BRICK = 0xFFB75A40;
    /** GT6 {@code MT.Ceramic} 220, 130, 70. */
    private static final int GT6_CERAMIC = 0xFFDC8246;

    private Gt6OpeningBlockColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        return tintIndex == 0 ? colorFor(state.getBlock()) : 0xFFFFFFFF;
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        return tintIndex == 0 ? colorFor(Block.byItem(stack.getItem())) : 0xFFFFFFFF;
    }

    public static Block[] tintedBlocks() {
        return new Block[] {
            ModBlocks.FIREBRICK.get(),
            ModBlocks.CERAMIC_MOLD.get()
        };
    }

    /**
     * Fired ceramic molds share the grayscale ROUGH block texture and need
     * {@link #ceramicColor()}. GT6 randomtools clay icons (raw molds /
     * crucible) are already painted and must not be multiplied again.
     */
    public static Item[] tintedItems() {
        java.util.ArrayList<Item> items = new java.util.ArrayList<>();
        items.add(ModBlocks.FIREBRICK.get().asItem());
        java.util.Collections.addAll(items, ModItems.ceramicMoldItems());
        return items.toArray(Item[]::new);
    }

    static int ceramicColor() {
        return GT6_CERAMIC;
    }

    private static int colorFor(Block block) {
        return block == ModBlocks.FIREBRICK.get() ? GT6_BRICK : GT6_CERAMIC;
    }
}
