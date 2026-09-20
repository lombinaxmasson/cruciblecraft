package com.masson.cruciblecraft.content.block;

import net.minecraft.world.level.block.Block;

/** Placeable GT6 {@code OP.blockPlate} PrefixBlock: 9 plates, pickaxe, metal. */
public final class MaterialPlateStorageBlock extends Block {
    private final String materialId;

    public MaterialPlateStorageBlock(String materialId, Properties properties) {
        super(properties);
        this.materialId = materialId;
    }

    public String materialId() {
        return materialId;
    }
}
