package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.api.material.MaterialPrefix;

import net.minecraft.world.level.block.Block;

/** Placeable GT6 PrefixBlock for {@code machine_casing} / {@code machine_casing_double}. */
public final class MaterialCasingBlock extends Block {
    private final String materialId;
    private final MaterialPrefix form;

    public MaterialCasingBlock(
            String materialId, MaterialPrefix form, Properties properties) {
        super(properties);
        this.materialId = materialId;
        this.form = form;
    }

    public String materialId() {
        return materialId;
    }

    public MaterialPrefix form() {
        return form;
    }
}
