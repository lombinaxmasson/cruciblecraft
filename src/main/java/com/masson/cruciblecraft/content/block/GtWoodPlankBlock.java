package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtWoodCatalog;

import net.minecraft.world.level.block.Block;

/** Placeable GT6 distinct-wood plank cube. */
public final class GtWoodPlankBlock extends Block {
    private final GtWoodCatalog.Definition definition;

    public GtWoodPlankBlock(GtWoodCatalog.Definition definition, Properties properties) {
        super(properties);
        this.definition = definition;
    }

    public GtWoodCatalog.Definition definition() {
        return definition;
    }
}
