package com.masson.cruciblecraft.content.block;

import net.minecraft.world.level.block.Block;

/** Placeable GT6-style storage cube for one material's {@code block} form. */
public final class MaterialStorageBlock extends Block {
    private final String materialId;

    public MaterialStorageBlock(String materialId, Properties properties) {
        super(properties);
        this.materialId = materialId;
    }

    public String materialId() {
        return materialId;
    }
}
