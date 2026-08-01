package com.masson.cruciblecraft.recipe.gt;

final class RecipeCacheRules {
    private RecipeCacheRules() {}

    static Decision decide(
            boolean hasQuery,
            boolean resourcesSame,
            long cachedRevision,
            long currentRevision,
            boolean searched) {
        boolean rebuildQuery = !hasQuery || !resourcesSame;
        boolean invalidateResult = rebuildQuery || cachedRevision != currentRevision;
        return new Decision(rebuildQuery, invalidateResult, invalidateResult || !searched);
    }

    record Decision(
            boolean rebuildQuery,
            boolean invalidateResult,
            boolean search) {}
}
