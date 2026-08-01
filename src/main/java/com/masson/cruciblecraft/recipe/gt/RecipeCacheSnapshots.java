package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.function.BiPredicate;

final class RecipeCacheSnapshots {
    private RecipeCacheSnapshots() {}

    static <T> boolean same(
            List<T> cached,
            List<T> offered,
            BiPredicate<T, T> sameEntry) {
        if (cached.size() != offered.size()) {
            return false;
        }
        for (int index = 0; index < cached.size(); index++) {
            if (!sameEntry.test(cached.get(index), offered.get(index))) {
                return false;
            }
        }
        return true;
    }
}
