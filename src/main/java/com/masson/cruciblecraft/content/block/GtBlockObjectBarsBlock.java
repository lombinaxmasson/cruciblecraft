package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.world.level.block.IronBarsBlock;

/** GT bars identity. */
public final class GtBlockObjectBarsBlock extends IronBarsBlock {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectBarsBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }
}
