package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.world.level.block.RotatedPillarBlock;

/** GT grass/crop bale identity. */
public final class GtBlockObjectBaleBlock extends RotatedPillarBlock {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectBaleBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }
}
