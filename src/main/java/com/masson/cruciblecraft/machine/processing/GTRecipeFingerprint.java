package com.masson.cruciblecraft.machine.processing;

import java.util.Optional;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntry;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Full GT recipe-entry hash. This codec is also the source for the registered
 * recipe network codec, so custom ingredients and data components fail closed.
 */
public final class GTRecipeFingerprint {
    private GTRecipeFingerprint() {}

    public static Optional<String> recipe(
            ResourceLocation mapId,
            GTRecipe recipe,
            HolderLookup.Provider registries) {
        return RegistryCodecHash.hash(
                GTRecipeEntry.CODEC.codec(),
                new GTRecipeEntry(mapId, recipe.withoutProvenance()),
                registries);
    }

    public static Optional<String> input(
            ItemStack stack,
            HolderLookup.Provider registries) {
        return RegistryCodecHash.hash(ItemStack.STRICT_CODEC, stack, registries);
    }
}
