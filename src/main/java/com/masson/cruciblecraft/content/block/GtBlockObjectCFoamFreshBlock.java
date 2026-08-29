package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.world.level.block.Block;

/** Fresh C-Foam identity. Softer and slime-like versus hardened C-Foam. */
public final class GtBlockObjectCFoamFreshBlock extends Block {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectCFoamFreshBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }
}
