package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialComponentPolicy;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One registered item per inventory prefix. The material lives on
 * {@link ModComponents#PREFIX_MATERIAL}; stacks without a gated material
 * are invalid and must not be treated as a wildcard.
 */
public final class PrefixMaterialItem extends Item implements MaterialComponentPolicy {
    private final MaterialPrefix form;

    public PrefixMaterialItem(MaterialPrefix form, Properties properties) {
        super(properties);
        this.form = form;
    }

    public MaterialPrefix form() {
        return form;
    }

    @Override
    public String materialComponentId() {
        return PREFIX_MATERIAL_COMPONENT_ID;
    }

    @Override
    public String missingMaterialForm() {
        return form.serializedName();
    }

    @Override
    public boolean isPersistedMaterialAllowed(String materialId) {
        return MaterialCatalog.find(materialId)
                .filter(material -> MaterialCatalog.isFormRegistered(material, form))
                .isPresent();
    }

    @Override
    public Component getName(ItemStack stack) {
        String materialId = stack.get(ModComponents.PREFIX_MATERIAL);
        if (materialId == null || !isPersistedMaterialAllowed(materialId)) {
            return super.getName(stack);
        }
        return MaterialFormItem.formName(materialId, form);
    }
}
