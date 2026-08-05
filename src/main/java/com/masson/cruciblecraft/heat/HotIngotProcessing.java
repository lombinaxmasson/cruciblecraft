package com.masson.cruciblecraft.heat;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
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
        if (source.getItem() instanceof MaterialFormItem form
                && form.form().equals(MaterialPrefixes.INGOT_HOT)) {
            return prepare(source, form, gameTime);
        }
        return source;
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
        if (stack.isEmpty()
                || stack.has(ModComponents.HEAT.get())
                || !(stack.getItem() instanceof MaterialFormItem form)
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
