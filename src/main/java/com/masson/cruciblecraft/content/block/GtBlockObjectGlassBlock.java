package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.world.level.block.TransparentBlock;

/** GT6 colored / glow glass cube. Slabs stay {@link GtBlockObjectSlabBlock}. */
public final class GtBlockObjectGlassBlock extends TransparentBlock {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectGlassBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }
}
