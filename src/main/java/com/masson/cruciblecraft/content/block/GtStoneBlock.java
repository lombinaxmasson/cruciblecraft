package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtStoneCatalog;

import net.minecraft.world.level.block.Block;

/** Full GT stone identity. Each (source item, meta) is a distinct block. */
public final class GtStoneBlock extends Block {
    private final GtStoneCatalog.Variant variant;

    public GtStoneBlock(GtStoneCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtStoneCatalog.Variant variant() {
        return variant;
    }
}
