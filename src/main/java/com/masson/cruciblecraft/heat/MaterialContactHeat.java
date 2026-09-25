package com.masson.cruciblecraft.heat;

import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.content.item.PrefixMaterialItem;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixDefinition;

import net.minecraft.world.item.ItemStack;

/** Resolves GT6 contact heat strictly from material and prefix source facts. */
public final class MaterialContactHeat {
    private MaterialContactHeat() {}

    /**
     * Non-material stacks and stale material identities are inert. This keeps
     * inventory maintenance fail-closed while saved stacks are being migrated.
     */
    public static double damage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        if (stack.getItem() instanceof MaterialFormItem form) {
            return damage(form);
        }
        if (!(stack.getItem() instanceof PrefixMaterialItem prefix)) {
            return 0.0;
        }
        String materialId = stack.get(ModComponents.PREFIX_MATERIAL);
        if (materialId == null || !prefix.isPersistedMaterialAllowed(materialId)) {
            return 0.0;
        }
        try {
            return damage(
                    MaterialPrefixCatalog.definition(prefix.form()),
                    MaterialCatalog.require(materialId));
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return 0.0;
        }
    }

    static double damage(MaterialFormItem form) {
        try {
            return damage(
                    MaterialPrefixCatalog.definition(form.form()),
                    form.material());
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return 0.0;
        }
    }

    static double damage(
            MaterialPrefixDefinition prefix,
            MaterialDefinition material) {
        double materialDamage = material.gt6Metadata()
                .map(GT6MaterialMetadata::heatDamage)
                .orElse(0.0);
        double total = prefix.heatDamage() + materialDamage;
        return Double.isFinite(total) ? total : 0.0;
    }
}
