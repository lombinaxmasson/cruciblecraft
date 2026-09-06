package com.masson.cruciblecraft.recipe.gt;

import java.util.AbstractList;
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
        Index snapshot = index;
        if (snapshot.families().isEmpty()) {
            return snapshot.recipes();
        }
        List<Entry> entries = logicalEntries(snapshot);
        return new AbstractList<>() {
            @Override
            public GTRecipe get(int position) {
                return entries.get(position).recipe();
            }

            @Override
            public int size() {
                return entries.size();
            }
        };
    }

    public List<Entry> entries() {
        Index snapshot = index;
        return snapshot.families().isEmpty()
                ? snapshot.baseEntries()
                : logicalEntries(snapshot);
    }

    public Optional<Entry> entry(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        Index snapshot = index;
        Entry concrete = snapshot.byId().get(id);
        if (concrete != null) {
            return Optional.of(concrete);
        }
        for (RecipeFamily family : snapshot.families()) {
            Optional<Entry> entry = family.entry(id);
            if (entry.isPresent()) {
                requireCurrent(snapshot);
                return entry;
            }
        }
        return Optional.empty();
    }

    public boolean hasFluidCandidate(Fluid fluid) {
        Objects.requireNonNull(fluid, "fluid");
        return index.byFluid().containsKey(fluid);
    }

    public ResourceLocation recipeId(int index) {
        return entries().get(index).id();
    }

    public int unindexedRecipeCount() {
        return index.unindexed().size();
    }

    public long revision() {
        return index.revision();
    }

    public long runtimeEpoch() {
        return index.runtimeEpoch();
    }

    public Optional<RecipeFamily> family(String familyId) {
        Objects.requireNonNull(familyId, "familyId");
        return index.families().stream()
                .filter(family -> family.familyId().equals(familyId))
                .findFirst();
    }

    public List<RecipeFamily> families() {
        return index.families();
    }

    /**
     * Atomically replaces datapack recipes and rebuilds all input indexes.
     * Callers must supply a stable priority order, normally sorted by recipe id.
     */
    public void replaceRecipes(List<Entry> orderedRecipes) {
        if (!index.families().isEmpty()) {
            throw new UnsupportedOperationException(
                    "RecipeMap " + id
                            + " contains logical recipe families and must be "
                            + "replaced through an epoch publication");
        }
        GTRecipeRuntimeEpoch.replaceSingle(this, orderedRecipes);
    }

    /**
     * True when this recipe would land in the unindexed fallback bucket.
     * Used to validate family rows without building a second full index.
     */
    static boolean wouldBeUnindexed(GTRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        boolean indexed = false;
        boolean requiresFallbackScan = false;
        boolean hasFallbackItems = false;
        boolean hasFallbackComponents = false;
        for (int inputIndex = 0; inputIndex < recipe.itemInputs().size(); inputIndex++) {
            Ingredient ingredient = recipe.itemInputs().get(inputIndex);
            boolean primary = recipe.itemInputActions().get(inputIndex).kind()
                    == ItemInputAction.Kind.CONSUME;
            if (ingredient.isSimple()) {
                for (ItemStack stack : ingredient.getItems()) {
                    if (!stack.isEmpty()) {
                        if (primary) {
                            indexed = true;
                        } else {
                            hasFallbackItems = true;
                        }
                    }
                }
                continue;
            }
            ComponentIngredientIndex.Extraction extraction =
                    ComponentIngredientIndex.extract(ingredient);
            if (extraction.supported()) {
                if (primary) {
                    indexed = true;
                } else {
                    hasFallbackComponents = true;
                }
            } else {
                requiresFallbackScan = true;
            }
        }
        for (FluidStack stack : recipe.fluidInputsView()) {
            if (!stack.isEmpty()) {
                indexed = true;
            }
        }
        if (!indexed) {
            indexed = hasFallbackItems || hasFallbackComponents;
        }
        return !indexed || requiresFallbackScan;
    }

    /** Builds all indexes without changing the live map. */
    public synchronized Prepared prepareRecipes(List<Entry> orderedRecipes) {
        return prepareRecipes(orderedRecipes, List.of(), 0L);
    }

    /**
     * Builds concrete indexes and exact logical family snapshots without
     * changing the live map.
     */
    public synchronized Prepared prepareRecipes(
            List<Entry> orderedRecipes,
            List<RecipeFamily> families,
            long runtimeEpoch) {
        long baseRevision = index.revision();
        return new Prepared(
                this,
                baseRevision,
                Index.build(
                        List.copyOf(orderedRecipes),
                        List.copyOf(families),
                        Math.incrementExact(baseRevision),
                        runtimeEpoch));
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
     * Finds the unique maximal concrete recipe under the requirement-superset
     * relation, then asks handlers in registration order to synthesize a result.
     */
    public Optional<GTRecipe> find(GTRecipeQuery query) {
        return findMatch(query).map(Match::recipe);
    }

    public Optional<Match> findMatch(GTRecipeQuery query) {
        Objects.requireNonNull(query, "query");
        Index snapshot = index;
        TreeSet<Integer> candidates = candidates(snapshot, query);
        List<Match> maximal = new ArrayList<>();
        for (int recipeIndex : candidates) {
            Entry entry = snapshot.entries().get(recipeIndex);
            if (entry.recipe().matches(query)) {
                Match candidate = new Match(entry.id(), entry.recipe());
                boolean dominated = maximal.stream().anyMatch(existing ->
                        isStrictRequirementSuperset(
                                existing.recipe(), candidate.recipe()));
                if (!dominated) {
                    maximal.removeIf(existing -> isStrictRequirementSuperset(
                            candidate.recipe(), existing.recipe()));
                    maximal.add(candidate);
                }
            }
        }
        if (!maximal.isEmpty()) {
            // Candidate indexes are declaration ordered. Dominance removes
            // less-specific matches; incomparable maximal recipes describe
            // multiple valid jobs, so use the first declared one just as GT6
            // does instead of throwing from the per-tick player input path.
            return Optional.of(maximal.getFirst());
        }
        for (RecipeFamily family : snapshot.families()) {
            Optional<Entry> entry = family.findLazy(query);
            requireCurrent(snapshot);
            if (entry.isPresent()) {
                return Optional.of(new Match(
                        entry.orElseThrow().id(),
                        entry.orElseThrow().recipe()));
            }
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

    public int indexedCandidateCount(GTRecipeQuery query) {
        Objects.requireNonNull(query, "query");
        Index snapshot = index;
        int count = candidates(snapshot, query).size();
        for (RecipeFamily family : snapshot.families()) {
            count = Math.addExact(count, family.indexedLazyCandidateCount(query));
        }
        requireCurrent(snapshot);
        return count;
    }

    private static TreeSet<Integer> candidates(
            Index snapshot,
            GTRecipeQuery query) {
        TreeSet<Integer> candidates = new TreeSet<>();
        for (ItemStack stack : query.itemInputsView()) {
            if (!stack.isEmpty()) {
                candidates.addAll(snapshot.byItem().getOrDefault(stack.getItem(), List.of()));
                for (ComponentIngredientIndex.Key key
                        : ComponentIngredientIndex.keys(stack)) {
                    candidates.addAll(snapshot.byComponent()
                            .getOrDefault(key, List.of()));
                }
            }
        }
        for (FluidStack stack : query.fluidInputsView()) {
            if (!stack.isEmpty()) {
                candidates.addAll(snapshot.byFluid().getOrDefault(stack.getFluid(), List.of()));
            }
        }
        candidates.addAll(snapshot.unindexed());
        return candidates;
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
                    ingredient.isSimple() ? null : ingredient,
                    recipe.itemInputActions().get(i));
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

    private record IngredientKey(
            String type,
            Set<Item> items,
            Object customIdentity,
            ItemInputAction action) {}

    private List<Entry> logicalEntries(Index snapshot) {
        return new AbstractList<>() {
            @Override
            public Entry get(int position) {
                requireCurrent(snapshot);
                if (position < 0 || position >= size()) {
                    throw new IndexOutOfBoundsException(position);
                }
                if (position < snapshot.baseEntries().size()) {
                    return snapshot.baseEntries().get(position);
                }
                int familyIndex = position - snapshot.baseEntries().size();
                for (RecipeFamily family : snapshot.families()) {
                    if (familyIndex < family.logicalRecipeCount()) {
                        Entry entry = family.enumerationEntry(familyIndex);
                        requireCurrent(snapshot);
                        return entry;
                    }
                    familyIndex -= family.logicalRecipeCount();
                }
                throw new IndexOutOfBoundsException(position);
            }

            @Override
            public int size() {
                requireCurrent(snapshot);
                return snapshot.logicalRecipeCount();
            }
        };
    }

    private void requireCurrent(Index snapshot) {
        if (index != snapshot && !snapshot.families().isEmpty()) {
            throw new IllegalStateException(
                    "RecipeMap " + id + " view belongs to an old epoch");
        }
    }

    public boolean hasCandidate(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Index snapshot = index;
        boolean concrete = snapshot.byItem().containsKey(stack.getItem())
                || ComponentIngredientIndex.keys(stack).stream()
                        .anyMatch(snapshot.byComponent()::containsKey)
                || !snapshot.unindexed().isEmpty();
        if (concrete) {
            return true;
        }
        boolean family = snapshot.families().stream()
                .anyMatch(candidate -> candidate.hasLazyCandidate(stack));
        requireCurrent(snapshot);
        return family;
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

    /**
     * Immutable per-epoch logical family contract. Implementations may retain
     * bounded lookup caches, but enumeration must expose every stable id.
     */
    public interface RecipeFamily {
        String familyId();
        long epoch();
        int logicalRecipeCount();
        int eagerRecipeCount();
        int lazyRecipeCount();
        int cacheSize();
        int cacheCeiling();
        String stableFingerprint();
        List<ResourceLocation> recipeIds();
        List<Entry> eagerEntries();
        Entry enumerationEntry(int index);
        Optional<Entry> entry(ResourceLocation id);
        Optional<Entry> findLazy(GTRecipeQuery query);
        int indexedLazyCandidateCount(GTRecipeQuery query);
        boolean hasLazyCandidate(ItemStack stack);
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
        public List<Entry> entries() { return index.baseEntries(); }
        public int logicalRecipeCount() { return index.logicalRecipeCount(); }
        public List<RecipeFamily> families() { return index.families(); }
        public long runtimeEpoch() { return index.runtimeEpoch(); }
        public int unindexedRecipeCount() { return index.unindexed().size(); }
        boolean belongsTo(RecipeMap map) { return owner == map; }
        boolean canPublish() { return owner.canPublish(this); }
        void publish() { owner.publishPrepared(this); }
    }

    private record Index(
            long revision,
            List<Entry> entries,
            List<GTRecipe> recipes,
            Map<ResourceLocation, Entry> byId,
            Map<Item, List<Integer>> byItem,
            Map<ComponentIngredientIndex.Key, List<Integer>> byComponent,
            Map<Fluid, List<Integer>> byFluid,
            List<Integer> unindexed,
            List<Entry> baseEntries,
            List<RecipeFamily> families,
            int logicalRecipeCount,
            long runtimeEpoch) {

        private static Index empty() {
            return new Index(
                    0L,
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    0,
                    0L);
        }

        private static Index build(
                List<Entry> baseEntries,
                List<RecipeFamily> families,
                long revision,
                long runtimeEpoch) {
            List<Entry> entries = new ArrayList<>(baseEntries);
            Set<ResourceLocation> logicalIds = new HashSet<>();
            for (Entry entry : baseEntries) {
                if (!logicalIds.add(entry.id())) {
                    throw new IllegalArgumentException(
                            "Duplicate recipe id " + entry.id());
                }
            }
            int logicalRecipeCount = baseEntries.size();
            Set<String> familyIds = new HashSet<>();
            for (RecipeFamily family : families) {
                if (!familyIds.add(family.familyId())) {
                    throw new IllegalArgumentException(
                            "Duplicate recipe family " + family.familyId());
                }
                if (runtimeEpoch <= 0L || family.epoch() != runtimeEpoch) {
                    throw new IllegalArgumentException(
                            "Recipe family epoch does not match prepared map epoch");
                }
                if (family.recipeIds().size() != family.logicalRecipeCount()) {
                    throw new IllegalArgumentException(
                            "Recipe family stable-id view is incomplete");
                }
                for (ResourceLocation id : family.recipeIds()) {
                    if (!logicalIds.add(id)) {
                        throw new IllegalArgumentException(
                                "Duplicate recipe id " + id);
                    }
                }
                if (family.eagerEntries().size() != family.eagerRecipeCount()) {
                    throw new IllegalArgumentException(
                            "Recipe family eager view is incomplete");
                }
                entries.addAll(family.eagerEntries());
                logicalRecipeCount = Math.addExact(
                        logicalRecipeCount, family.logicalRecipeCount());
            }
            entries = List.copyOf(entries);
            List<GTRecipe> recipes = entries.stream().map(Entry::recipe).toList();
            Map<ResourceLocation, Entry> byId = new HashMap<>();
            Map<Item, List<Integer>> byItem = new HashMap<>();
            Map<ComponentIngredientIndex.Key, List<Integer>> byComponent =
                    new HashMap<>();
            Map<Fluid, List<Integer>> byFluid = new HashMap<>();
            List<Integer> unindexed = new ArrayList<>();

            for (int recipeIndex = 0; recipeIndex < recipes.size(); recipeIndex++) {
                Entry duplicate = byId.putIfAbsent(
                        entries.get(recipeIndex).id(),
                        entries.get(recipeIndex));
                if (duplicate != null) {
                    throw new IllegalArgumentException(
                            "Duplicate recipe id "
                                    + entries.get(recipeIndex).id());
                }
                GTRecipe recipe = recipes.get(recipeIndex);
                boolean indexed = false;
                boolean requiresFallbackScan = false;
                Set<Item> indexedItems = new HashSet<>();
                Set<ComponentIngredientIndex.Key> indexedComponents =
                        new HashSet<>();
                Set<Item> fallbackItems = new HashSet<>();
                Set<ComponentIngredientIndex.Key> fallbackComponents =
                        new HashSet<>();
                for (int inputIndex = 0;
                        inputIndex < recipe.itemInputs().size();
                        inputIndex++) {
                    Ingredient ingredient = recipe.itemInputs().get(inputIndex);
                    boolean primary = recipe.itemInputActions().get(inputIndex).kind()
                            == ItemInputAction.Kind.CONSUME;
                    if (ingredient.isSimple()) {
                        for (ItemStack stack : ingredient.getItems()) {
                            if (!stack.isEmpty()) {
                                (primary ? indexedItems : fallbackItems)
                                        .add(stack.getItem());
                                indexed |= primary;
                            }
                        }
                        continue;
                    }
                    ComponentIngredientIndex.Extraction extraction =
                            ComponentIngredientIndex.extract(ingredient);
                    if (extraction.supported()) {
                        (primary ? indexedComponents : fallbackComponents)
                                .addAll(extraction.keys());
                        indexed |= primary;
                    } else {
                        requiresFallbackScan = true;
                    }
                }
                for (Item item : indexedItems) {
                    byItem.computeIfAbsent(item, ignored -> new ArrayList<>())
                            .add(recipeIndex);
                }
                for (ComponentIngredientIndex.Key key : indexedComponents) {
                    byComponent.computeIfAbsent(key, ignored -> new ArrayList<>())
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
                if (!indexed) {
                    indexedItems.addAll(fallbackItems);
                    indexedComponents.addAll(fallbackComponents);
                    for (Item item : fallbackItems) {
                        byItem.computeIfAbsent(item, ignored -> new ArrayList<>())
                                .add(recipeIndex);
                    }
                    for (ComponentIngredientIndex.Key key : fallbackComponents) {
                        byComponent.computeIfAbsent(key, ignored -> new ArrayList<>())
                                .add(recipeIndex);
                    }
                    indexed = !fallbackItems.isEmpty()
                            || !fallbackComponents.isEmpty();
                }
                if (!indexed || requiresFallbackScan) {
                    unindexed.add(recipeIndex);
                }
            }
            return new Index(
                    revision,
                    entries,
                    recipes,
                    Map.copyOf(byId),
                    immutableIndex(byItem),
                    immutableIndex(byComponent),
                    immutableIndex(byFluid),
                    List.copyOf(unindexed),
                    baseEntries,
                    families,
                    logicalRecipeCount,
                    runtimeEpoch);
        }

        private static <K> Map<K, List<Integer>> immutableIndex(
                Map<K, List<Integer>> mutable) {
            Map<K, List<Integer>> immutable = new HashMap<>();
            mutable.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
            return Map.copyOf(immutable);
        }
    }
}
