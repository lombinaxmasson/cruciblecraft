package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A named recipe collection with immutable lookup snapshots and code-level
 * fallback handlers.
 */
public final class RecipeMap {
    private final ResourceLocation id;
    private final List<RecipeHandler> handlers;
    private volatile Index index = Index.empty();

    public RecipeMap(ResourceLocation id) {
        this(id, List.of());
    }

    public RecipeMap(ResourceLocation id, Collection<RecipeHandler> handlers) {
        this.id = Objects.requireNonNull(id, "id");
        this.handlers = List.copyOf(handlers);
    }

    public ResourceLocation id() {
        return id;
    }

    public List<GTRecipe> recipes() {
        return index.recipes();
    }

    public List<Entry> entries() {
        return index.entries();
    }

    public ResourceLocation recipeId(int index) {
        return this.index.entries().get(index).id();
    }

    public int unindexedRecipeCount() {
        return index.unindexed().size();
    }

    public long revision() {
        return index.revision();
    }

    /**
     * Atomically replaces datapack recipes and rebuilds all input indexes.
     * Callers must supply a stable priority order, normally sorted by recipe id.
     */
    public void replaceRecipes(List<Entry> orderedRecipes) {
        GTRecipeRuntimeEpoch.replaceSingle(this, orderedRecipes);
    }

    /** Builds all indexes without changing the live map. */
    public synchronized Prepared prepareRecipes(List<Entry> orderedRecipes) {
        long baseRevision = index.revision();
        return new Prepared(
                this,
                baseRevision,
                Index.build(List.copyOf(orderedRecipes), Math.incrementExact(baseRevision)));
    }

    synchronized boolean canPublish(Prepared prepared) {
        return prepared.owner() == this && prepared.baseRevision() == index.revision();
    }

    synchronized void publishPrepared(Prepared prepared) {
        if (!canPublish(prepared)) {
            throw new IllegalStateException("Stale prepared recipe map " + id);
        }
        index = prepared.index();
    }

    /**
     * Finds the first concrete recipe in declaration order, then asks handlers
     * in registration order to synthesize a result.
     */
    public Optional<GTRecipe> find(GTRecipeQuery query) {
        return findMatch(query).map(Match::recipe);
    }

    public Optional<Match> findMatch(GTRecipeQuery query) {
        Objects.requireNonNull(query, "query");
        Index snapshot = index;
        TreeSet<Integer> candidates = new TreeSet<>();
        for (ItemStack stack : query.itemInputsView()) {
            if (!stack.isEmpty()) {
                candidates.addAll(snapshot.byItem().getOrDefault(stack.getItem(), List.of()));
            }
        }
        for (FluidStack stack : query.fluidInputsView()) {
            if (!stack.isEmpty()) {
                candidates.addAll(snapshot.byFluid().getOrDefault(stack.getFluid(), List.of()));
            }
        }
        candidates.addAll(snapshot.unindexed());

        Match best = null;
        for (int recipeIndex : candidates) {
            Entry entry = snapshot.entries().get(recipeIndex);
            if (entry.recipe().matches(query)) {
                Match candidate = new Match(entry.id(), entry.recipe());
                if (best == null || isStrictRequirementSuperset(
                        candidate.recipe(), best.recipe())) {
                    best = candidate;
                }
            }
        }
        if (best != null) {
            return Optional.of(best);
        }
        for (RecipeHandler handler : handlers) {
            Optional<Entry> entry = Objects.requireNonNull(
                    handler.find(query),
                    "Recipe handlers must not return null");
            if (entry.isPresent()) {
                return Optional.of(new Match(entry.get().id(), entry.get().recipe()));
            }
        }
        return Optional.empty();
    }

    static boolean isStrictRequirementSuperset(GTRecipe candidate, GTRecipe other) {
        Map<IngredientKey, Integer> candidateItems = itemRequirements(candidate);
        Map<IngredientKey, Integer> otherItems = itemRequirements(other);
        Map<Fluid, Integer> candidateFluids = fluidRequirements(candidate);
        Map<Fluid, Integer> otherFluids = fluidRequirements(other);
        if (!candidateItems.keySet().containsAll(otherItems.keySet())
                || !candidateFluids.keySet().containsAll(otherFluids.keySet())) {
            return false;
        }
        boolean stricter = candidateItems.size() > otherItems.size()
                || candidateFluids.size() > otherFluids.size();
        for (var entry : otherItems.entrySet()) {
            int amount = candidateItems.get(entry.getKey());
            if (amount < entry.getValue()) return false;
            stricter |= amount > entry.getValue();
        }
        for (var entry : otherFluids.entrySet()) {
            int amount = candidateFluids.get(entry.getKey());
            if (amount < entry.getValue()) return false;
            stricter |= amount > entry.getValue();
        }
        return stricter;
    }

    private static Map<IngredientKey, Integer> itemRequirements(GTRecipe recipe) {
        Map<IngredientKey, Integer> result = new HashMap<>();
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            Ingredient ingredient = recipe.itemInputs().get(i);
            IngredientKey key = new IngredientKey(
                    ingredient.getClass().getName(),
                    java.util.Arrays.stream(ingredient.getItems())
                            .filter(stack -> !stack.isEmpty())
                            .map(ItemStack::getItem)
                            .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                    ingredient.isSimple() ? null : ingredient);
            result.merge(key, recipe.itemInputCounts().get(i), Integer::sum);
        }
        return result;
    }

    private static Map<Fluid, Integer> fluidRequirements(GTRecipe recipe) {
        Map<Fluid, Integer> result = new HashMap<>();
        for (FluidStack stack : recipe.fluidInputsView()) {
            result.merge(stack.getFluid(), stack.getAmount(), Integer::sum);
        }
        return result;
    }

    private record IngredientKey(String type, Set<Item> items, Object customIdentity) {}

    public boolean hasCandidate(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Index snapshot = index;
        return snapshot.byItem().containsKey(stack.getItem())
                || !snapshot.unindexed().isEmpty();
    }

    public record Entry(ResourceLocation id, GTRecipe recipe) {
        public Entry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(recipe, "recipe");
        }
    }

    public record Match(ResourceLocation id, GTRecipe recipe) {
        public Match {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(recipe, "recipe");
        }
    }

    public static final class Prepared {
        private final RecipeMap owner;
        private final long baseRevision;
        private final Index index;

        private Prepared(RecipeMap owner, long baseRevision, Index index) {
            this.owner = owner;
            this.baseRevision = baseRevision;
            this.index = index;
        }

        private RecipeMap owner() { return owner; }
        public long baseRevision() { return baseRevision; }
        private Index index() { return index; }
        public List<Entry> entries() { return index.entries(); }
        public int unindexedRecipeCount() { return index.unindexed().size(); }
        boolean canPublish() { return owner.canPublish(this); }
        void publish() { owner.publishPrepared(this); }
    }

    private record Index(
            long revision,
            List<Entry> entries,
            List<GTRecipe> recipes,
            Map<Item, List<Integer>> byItem,
            Map<Fluid, List<Integer>> byFluid,
            List<Integer> unindexed) {

        private static Index empty() {
            return new Index(0L, List.of(), List.of(), Map.of(), Map.of(), List.of());
        }

        private static Index build(List<Entry> entries, long revision) {
            List<GTRecipe> recipes = entries.stream().map(Entry::recipe).toList();
            Map<Item, List<Integer>> byItem = new HashMap<>();
            Map<Fluid, List<Integer>> byFluid = new HashMap<>();
            List<Integer> unindexed = new ArrayList<>();

            for (int recipeIndex = 0; recipeIndex < recipes.size(); recipeIndex++) {
                GTRecipe recipe = recipes.get(recipeIndex);
                boolean indexed = false;
                boolean requiresFallbackScan = false;
                Set<Item> indexedItems = new HashSet<>();
                for (Ingredient ingredient : recipe.itemInputs()) {
                    requiresFallbackScan |= !ingredient.isSimple();
                    for (ItemStack stack : ingredient.getItems()) {
                        if (!stack.isEmpty()) {
                            indexedItems.add(stack.getItem());
                            indexed = true;
                        }
                    }
                }
                for (Item item : indexedItems) {
                    byItem.computeIfAbsent(item, ignored -> new ArrayList<>())
                            .add(recipeIndex);
                }
                Set<Fluid> indexedFluids = new HashSet<>();
                for (FluidStack stack : recipe.fluidInputsView()) {
                    if (!stack.isEmpty()) {
                        indexedFluids.add(stack.getFluid());
                        indexed = true;
                    }
                }
                for (Fluid fluid : indexedFluids) {
                    byFluid.computeIfAbsent(fluid, ignored -> new ArrayList<>())
                            .add(recipeIndex);
                }
                if (!indexed || requiresFallbackScan) {
                    unindexed.add(recipeIndex);
                }
            }
            return new Index(
                    revision,
                    entries,
                    recipes,
                    immutableIndex(byItem),
                    immutableIndex(byFluid),
                    List.copyOf(unindexed));
        }

        private static <K> Map<K, List<Integer>> immutableIndex(
                Map<K, List<Integer>> mutable) {
            Map<K, List<Integer>> immutable = new HashMap<>();
            mutable.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
            return Map.copyOf(immutable);
        }
    }
}
