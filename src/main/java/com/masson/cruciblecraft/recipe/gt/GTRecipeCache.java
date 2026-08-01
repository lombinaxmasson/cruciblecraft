package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Per-machine recipe cache. Unchanged inventories reuse both the immutable
 * query snapshot and the previous lookup result.
 */
public final class GTRecipeCache {
    private final RecipeMap recipeMap;
    private GTRecipeQuery query;
    private RecipeMap.Match cachedMatch;
    private long revision = -1L;
    private boolean searched;

    public GTRecipeCache(RecipeMap recipeMap) {
        this.recipeMap = Objects.requireNonNull(recipeMap, "recipeMap");
    }

    public Optional<RecipeMap.Match> findItems(ItemStack... inputs) {
        return find(List.of(inputs), List.of());
    }

    public Optional<RecipeMap.Match> find(
            List<ItemStack> itemInputs,
            List<FluidStack> fluidInputs) {
        boolean resourcesSame = query != null
                && sameItems(query.itemInputsView(), itemInputs)
                && sameFluids(query.fluidInputsView(), fluidInputs);
        long currentRevision = recipeMap.revision();
        RecipeCacheRules.Decision decision = RecipeCacheRules.decide(
                query != null,
                resourcesSame,
                revision,
                currentRevision,
                searched);
        if (decision.rebuildQuery()) {
            query = new GTRecipeQuery(itemInputs, fluidInputs);
        }
        if (decision.invalidateResult()) {
            cachedMatch = null;
            searched = false;
            revision = currentRevision;
        }
        if (decision.search()) {
            RecipeMap.Match found = recipeMap.findMatch(query).orElse(null);
            if (found != null && !found.recipe().canBeBuffered()) {
                return Optional.of(found);
            }
            cachedMatch = found;
            searched = true;
        }
        return Optional.ofNullable(cachedMatch);
    }

    public void invalidate() {
        query = null;
        cachedMatch = null;
        revision = -1L;
        searched = false;
    }

    private static boolean sameItems(List<ItemStack> cached, List<ItemStack> offered) {
        return RecipeCacheSnapshots.same(
                cached,
                offered,
                (left, right) -> left.getCount() == right.getCount()
                        && ItemStack.isSameItemSameComponents(left, right));
    }

    private static boolean sameFluids(List<FluidStack> cached, List<FluidStack> offered) {
        return RecipeCacheSnapshots.same(
                cached,
                offered,
                (left, right) -> left.getAmount() == right.getAmount()
                        && FluidStack.isSameFluidSameComponents(left, right));
    }
}
