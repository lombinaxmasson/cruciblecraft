package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

/**
 * Block membership the projection needs from the live registries.
 * Tests supply a stand-in so the grid can be asserted without GL.
 */
public interface BlockFormIndex {
    /** Registered blocks in the tag. Order is not significant. */
    List<ResourceLocation> blocksInTag(ResourceLocation tag);

    /** True when the block has an item form EMI can put in a material slot. */
    boolean hasItemForm(ResourceLocation block);
}
