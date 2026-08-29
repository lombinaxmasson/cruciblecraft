package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.world.level.block.RotatedPillarBlock;

/** GT log / fireproof-log identity with axis placement. */
public final class GtBlockObjectLogBlock extends RotatedPillarBlock {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectLogBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }
}
