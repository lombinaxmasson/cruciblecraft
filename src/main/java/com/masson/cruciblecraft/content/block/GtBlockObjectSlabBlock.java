package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.world.level.block.SlabBlock;

/** GT block-object slab identity. */
public final class GtBlockObjectSlabBlock extends SlabBlock {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectSlabBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }
}
