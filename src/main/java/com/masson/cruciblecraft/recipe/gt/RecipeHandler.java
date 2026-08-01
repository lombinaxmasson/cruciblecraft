package com.masson.cruciblecraft.recipe.gt;

import java.util.Optional;

/**
 * Code-level recipe extension point. Synthesized recipes must provide a stable
 * id so machines can identify and persist progress across ticks and reloads.
 */
@FunctionalInterface
public interface RecipeHandler {
    Optional<RecipeMap.Entry> find(GTRecipeQuery query);
}
