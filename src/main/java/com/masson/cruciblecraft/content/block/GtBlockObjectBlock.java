package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.world.level.block.Block;

/** Solid GT block-object identity (cfoam and other non-special solids). */
public final class GtBlockObjectBlock extends Block {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }
}
