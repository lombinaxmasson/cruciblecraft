package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.world.level.block.RailBlock;

/** GT rail identity. */
public final class GtBlockObjectRailBlock extends RailBlock {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectRailBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }
}
