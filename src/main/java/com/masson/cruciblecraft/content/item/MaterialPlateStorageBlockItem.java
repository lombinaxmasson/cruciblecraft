package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.MaterialPlateStorageBlock;
import com.masson.cruciblecraft.energy.converter.FurnaceFuelAdapter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** Plate-storage block item that keeps the generated material-form identity. */
public final class MaterialPlateStorageBlockItem extends BlockItem
        implements MaterialFormItem {
    private final String materialId;

    public MaterialPlateStorageBlockItem(
            MaterialPlateStorageBlock block, Properties properties) {
        super(block, properties);
        this.materialId = block.materialId();
    }

    @Override
    public String materialId() {
        return materialId;
    }

    @Override
    public MaterialPrefix form() {
        return MaterialPrefixes.STORAGE_PLATE;
    }

    @Override
    public Component getName(ItemStack stack) {
        return materialFormName();
    }

    @Override
    public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) {
        return FurnaceFuelAdapter.itemBurnTime(
                stack, super.getBurnTime(stack, recipeType));
    }
}
