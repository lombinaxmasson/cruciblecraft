package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Compact family bodies stay off the login {@code update_recipes} packet.
 * Dedicated clients prefetch every map once they are in the world, and a
 * machine screen still requests its own map if that prefetch has not asked.
 */
public final class RecipeLoginSync {
    private RecipeLoginSync() {}

    public static boolean omittedFromLogin(Object recipe) {
        Object value = recipe instanceof RecipeHolder<?> holder ? holder.value() : recipe;
        return value instanceof CompactGTRecipeFamilyEntry
                || value instanceof CompactGTRecipeFamilyBundle;
    }

    public static Collection<?> loginPacketRecipes(Collection<?> recipes) {
        boolean omit = false;
        for (Object recipe : recipes) {
            if (omittedFromLogin(recipe)) {
                omit = true;
                break;
            }
        }
        if (!omit) {
            return recipes;
        }
        List<Object> kept = new ArrayList<>(recipes.size());
        for (Object recipe : recipes) {
            if (!omittedFromLogin(recipe)) {
                kept.add(recipe);
            }
        }
        return kept;
    }
}
