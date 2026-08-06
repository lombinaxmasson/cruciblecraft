package com.masson.cruciblecraft.recipe.gt;

import java.util.List;

import com.masson.cruciblecraft.material.MaterialCatalog;

/**
 * Single synchronized commit point for recipe maps, effective material
 * metadata, and publication-adjacent state. Validation and index construction
 * happen before entering this critical section; the accepted epoch advances
 * only after every publication action succeeds.
 */
public final class GTRecipeRuntimeEpoch {
    private static long epoch;

    private GTRecipeRuntimeEpoch() {}

    public static synchronized long publish(
            MaterialCatalog.RuntimePreview materials,
            List<RecipeMap.Prepared> maps) {
        return publish(materials, maps, () -> {});
    }

    static synchronized long publish(
            MaterialCatalog.RuntimePreview materials,
            List<RecipeMap.Prepared> maps,
            Runnable afterPublication) {
        long nextEpoch = Math.incrementExact(epoch);
        if (MaterialCatalog.runtimeRevision() != materials.baseRevision()) {
            throw new IllegalStateException("Stale material snapshot before recipe publication");
        }
        if (maps.stream().anyMatch(
                prepared -> prepared.runtimeEpoch() != 0L
                        && prepared.runtimeEpoch() != nextEpoch)) {
            throw new IllegalStateException(
                    "Prepared recipe maps do not belong to epoch " + nextEpoch);
        }
        if (maps.stream().anyMatch(prepared -> !preparedOwnerCanPublish(prepared))) {
            throw new IllegalStateException("Stale recipe map snapshot before publication");
        }

        MaterialCatalog.publishPreview(materials, () -> {
            for (RecipeMap.Prepared prepared : maps) {
                publishOwner(prepared);
            }
        });
        afterPublication.run();
        epoch = nextEpoch;
        return epoch;
    }

    public static synchronized long epoch() {
        return epoch;
    }

    public static synchronized long nextEpoch() {
        return Math.incrementExact(epoch);
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
