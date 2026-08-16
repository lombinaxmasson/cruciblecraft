package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.MaterialStorageBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Storage-cube block item that keeps the generated material-form identity. */
public final class MaterialStorageBlockItem extends BlockItem
        implements MaterialFormItem {
    private final String materialId;

    public MaterialStorageBlockItem(
            MaterialStorageBlock block, Properties properties) {
        super(block, properties);
        this.materialId = block.materialId();
    }

    @Override
    public String materialId() {
        return materialId;
    }

    @Override
    public MaterialPrefix form() {
        return MaterialPrefixes.BLOCK;
    }

    @Override
    public Component getName(ItemStack stack) {
        return materialFormName();
    }
}
