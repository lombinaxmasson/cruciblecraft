package com.masson.cruciblecraft.compat.emi;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * EMI round-trips non-empty component patches through SNBT. Empty patches
 * still encode as {@code {}}, which {@code StringNbtReader} then rejects as
 * "Error parsing NBT in deserialized stack".
 */
final class EmiStacks {
    /**
     * Multi-item ingredients pay EMI's full tag scan. The same ingredient
     * instance is shared by compact dictionary rows, so the scan runs once.
     */
    private static final IdentityHashMap<Ingredient, Map<Long, EmiIngredient>>
            MULTI_INGREDIENTS = new IdentityHashMap<>();

    private EmiStacks() {}

    static void clearIngredientCache() {
        synchronized (MULTI_INGREDIENTS) {
            MULTI_INGREDIENTS.clear();
        }
    }

    /**
     * Single-item inputs skip {@code EmiIngredient.of(Ingredient)}, which
     * builds a stream, a hash map, and then scans every item tag whenever
     * the ingredient has two or more items.
     */
    static EmiIngredient ofIngredient(Ingredient ingredient, long amount) {
        if (ingredient == null || ingredient.isEmpty()) {
            return EmiStack.EMPTY;
        }
        ItemStack[] items;
        synchronized (ingredient) {
            items = ingredient.getItems();
        }
        if (items.length == 1) {
            ItemStack stack = items[0];
            if (stack.isEmpty()) {
                return EmiStack.EMPTY;
            }
            DataComponentPatch patch = stack.getComponentsPatch();
            if (patch.isEmpty()) {
                return EmiStack.of(stack.getItem(), amount);
            }
            return EmiStack.of(stack.getItem(), patch, amount);
        }
        return multiIngredient(ingredient, amount);
    }

    private static EmiIngredient multiIngredient(Ingredient ingredient, long amount) {
        synchronized (MULTI_INGREDIENTS) {
            Map<Long, EmiIngredient> byAmount = MULTI_INGREDIENTS.get(ingredient);
            if (byAmount != null) {
                EmiIngredient cached = byAmount.get(amount);
                if (cached != null) {
                    return cached;
                }
            } else {
                byAmount = new HashMap<>();
                MULTI_INGREDIENTS.put(ingredient, byAmount);
            }
            EmiIngredient created = EmiIngredient.of(ingredient, amount);
            byAmount.put(amount, created);
            return created;
        }
    }

    static EmiStack ofItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return EmiStack.EMPTY;
        }
        if (stack.getComponentsPatch().isEmpty()) {
            return EmiStack.of(stack.getItem(), stack.getCount());
        }
        return EmiStack.of(stack);
    }

    static EmiStack ofFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            return EmiStack.EMPTY;
        }
        if (stack.getComponentsPatch().isEmpty()) {
            return EmiStack.of(stack.getFluid(), stack.getAmount());
        }
        return EmiStack.of(
                stack.getFluid(),
                stack.getComponentsPatch(),
                stack.getAmount());
    }
}
