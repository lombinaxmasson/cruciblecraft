package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.energy.converter.FurnaceFuelAdapter;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** One registered item per public-exchange (material, prefix) pair. */
public final class MaterialItem extends Item implements MaterialFormItem {
    private final String materialId;
    private final MaterialPrefix form;

    public MaterialItem(MaterialDefinition material, MaterialPrefix form, Properties properties) {
        super(properties);
        this.materialId = material.id();
        this.form = form;
    }

    @Override
    public String materialId() {
        return materialId;
    }

    @Override
    public MaterialPrefix form() {
        return form;
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
