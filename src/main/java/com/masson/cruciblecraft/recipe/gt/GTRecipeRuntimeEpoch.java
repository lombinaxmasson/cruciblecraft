package com.masson.cruciblecraft.recipe.gt;

import java.util.List;

import com.masson.cruciblecraft.material.MaterialCatalog;

/**
 * Single commit point for recipe maps and their effective material metadata.
 * Odd epochs are never exposed as accepted snapshots; validation and index
 * construction happen before entering this synchronized publication path.
 */
public final class GTRecipeRuntimeEpoch {
    private static long epoch;

    private GTRecipeRuntimeEpoch() {}

    public static synchronized long publish(
            MaterialCatalog.RuntimePreview materials,
            List<RecipeMap.Prepared> maps) {
        if (MaterialCatalog.runtimeRevision() != materials.baseRevision()) {
            throw new IllegalStateException("Stale material snapshot before recipe publication");
        }
        if (maps.stream().anyMatch(prepared -> !preparedOwnerCanPublish(prepared))) {
            throw new IllegalStateException("Stale recipe map snapshot before publication");
        }

        MaterialCatalog.publishPreview(materials, () -> {
            for (RecipeMap.Prepared prepared : maps) {
                publishOwner(prepared);
            }
        });
        epoch = Math.incrementExact(epoch);
        return epoch;
    }

    public static synchronized long epoch() {
        return epoch;
    }

    static synchronized void replaceSingle(
            RecipeMap map, List<RecipeMap.Entry> entries) {
        map.publishPrepared(map.prepareRecipes(entries));
    }

    private static boolean preparedOwnerCanPublish(RecipeMap.Prepared prepared) {
        return PreparedAccess.canPublish(prepared);
    }

    private static void publishOwner(RecipeMap.Prepared prepared) {
        PreparedAccess.publish(prepared);
    }

    /**
     * Keeps RecipeMap ownership encapsulated while the transaction owns all
     * publication calls.
     */
    static final class PreparedAccess {
        private PreparedAccess() {}

        static boolean canPublish(RecipeMap.Prepared prepared) {
            return prepared.canPublish();
        }

        static void publish(RecipeMap.Prepared prepared) {
            prepared.publish();
        }
    }
}
