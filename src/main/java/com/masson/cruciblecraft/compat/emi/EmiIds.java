package com.masson.cruciblecraft.compat.emi;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/**
 * EMI treats a recipe ID as a RecipeManager holder unless the path starts with
 * {@code '/'}. RecipeMap logical IDs (especially compact-family expansions) are
 * not holders. Leaving them unprefixed makes EMI, in dev mode, scan the whole
 * manager with {@code Map.containsValue} for every recipe and spam
 * "not present in recipe manager".
 */
public final class EmiIds {
    private EmiIds() {}

    public static ResourceLocation synthetic(ResourceLocation logicalId) {
        Objects.requireNonNull(logicalId, "logicalId");
        String path = logicalId.getPath();
        if (path.startsWith("/")) {
            return logicalId;
        }
        return ResourceLocation.fromNamespaceAndPath(
                logicalId.getNamespace(), "/" + path);
    }

    /**
     * Mixer and electric mixer (and loom / electric loom) share a RecipeMap, so
     * EMI ids must include the machine category or the same expansion is
     * registered twice.
     */
    public static ResourceLocation synthetic(
            ResourceLocation categoryId, ResourceLocation recipeId) {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(recipeId, "recipeId");
        return synthetic(ResourceLocation.fromNamespaceAndPath(
                recipeId.getNamespace(),
                categoryId.getPath() + "/" + recipeId.getPath()));
    }
}
