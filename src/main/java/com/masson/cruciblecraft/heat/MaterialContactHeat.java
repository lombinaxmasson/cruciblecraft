package com.masson.cruciblecraft.heat;

import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
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
        if (stack.isEmpty() || !(stack.getItem() instanceof MaterialFormItem form)) {
            return 0.0;
        }
        return damage(form);
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
