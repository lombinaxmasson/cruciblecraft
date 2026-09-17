package com.masson.cruciblecraft.heat;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.content.item.PrefixMaterialItem;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.world.item.ItemStack;

/** Applies runtime-only temperature state to every hot-ingot output. */
public final class HotIngotProcessing {
    private HotIngotProcessing() {}

    public static List<ItemStack> prepareOutputs(
            List<ItemStack> outputs,
            long gameTime) {
        return outputs.stream().map(stack -> prepare(stack, gameTime)).toList();
    }

    static ItemStack prepare(ItemStack source, long gameTime) {
        if (source.getItem() instanceof PrefixMaterialItem prefix
                && prefix.form().equals(MaterialPrefixes.INGOT_HOT)) {
            return preparePrefix(source, prefix, gameTime);
        }
        if (source.getItem() instanceof MaterialFormItem form
                && form.form().equals(MaterialPrefixes.INGOT_HOT)) {
            return prepare(source, form, gameTime);
        }
        return source;
    }

    private static ItemStack preparePrefix(
            ItemStack source,
            PrefixMaterialItem prefix,
            long gameTime) {
        String materialId = source.get(ModComponents.PREFIX_MATERIAL);
        if (materialId == null || !prefix.isPersistedMaterialAllowed(materialId)) {
            return source;
        }
        ItemStack output = source.copy();
        ItemHeat.set(
                output,
                (float) MaterialCatalog.require(materialId).thermal().meltingPoint(),
                gameTime);
        return output;
    }

    static ItemStack prepare(
            ItemStack source,
            MaterialFormItem form,
            long gameTime) {
        ItemStack output = source.copy();
        if (form.form().equals(MaterialPrefixes.INGOT_HOT)) {
            ItemHeat.set(
                    output,
                    (float) form.material().thermal().meltingPoint(),
                    gameTime);
        }
        return output;
    }

    /**
     * Repairs hot-ingot stacks created outside a processing recipe, such as
     * creative inventory, commands, loot, or compatibility integrations.
     */
    static boolean initializeIfMissing(ItemStack stack, long gameTime) {
        if (stack.isEmpty() || stack.has(ModComponents.HEAT.get())) {
            return false;
        }
        if (stack.getItem() instanceof PrefixMaterialItem prefix
                && prefix.form().equals(MaterialPrefixes.INGOT_HOT)) {
            String materialId = stack.get(ModComponents.PREFIX_MATERIAL);
            if (materialId == null || !prefix.isPersistedMaterialAllowed(materialId)) {
                return false;
            }
            ItemHeat.set(
                    stack,
                    (float) MaterialCatalog.require(materialId).thermal().meltingPoint(),
                    gameTime);
            return stack.has(ModComponents.HEAT.get());
        }
        if (!(stack.getItem() instanceof MaterialFormItem form)
                || !form.form().equals(MaterialPrefixes.INGOT_HOT)) {
            return false;
        }
        return initializeIfMissing(stack, form, gameTime);
    }

    static boolean initializeIfMissing(
            ItemStack stack,
            MaterialFormItem form,
            long gameTime) {
        if (stack.has(ModComponents.HEAT.get())
                || !form.form().equals(MaterialPrefixes.INGOT_HOT)) {
            return false;
        }
        ItemHeat.set(
                stack,
                (float) form.material().thermal().meltingPoint(),
                gameTime);
        return stack.has(ModComponents.HEAT.get());
    }
}
