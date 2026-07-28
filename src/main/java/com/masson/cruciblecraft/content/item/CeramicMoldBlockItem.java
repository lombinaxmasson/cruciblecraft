package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.mold.MoldShape;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;

public final class CeramicMoldBlockItem extends BlockItem {
    private final MoldShape shape;

    public CeramicMoldBlockItem(Block block, MoldShape shape, Properties properties) {
        super(block, properties);
        this.shape = shape;
    }

    public MoldShape shape() {
        return shape;
    }

    @Override
    public String getDescriptionId() {
        return "item.cruciblecraft." + shape.serializedName() + "_mold";
    }
}
