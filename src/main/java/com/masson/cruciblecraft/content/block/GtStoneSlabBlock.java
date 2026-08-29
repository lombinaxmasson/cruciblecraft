package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtStoneCatalog;

import net.minecraft.world.level.block.SlabBlock;

/** GT stone slab identity. Each (source item, meta) is a distinct SlabBlock. */
public final class GtStoneSlabBlock extends SlabBlock {
    private final GtStoneCatalog.Variant variant;

    public GtStoneSlabBlock(GtStoneCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtStoneCatalog.Variant variant() {
        return variant;
    }
}
