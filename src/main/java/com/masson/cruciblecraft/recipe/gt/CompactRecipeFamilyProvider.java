package com.masson.cruciblecraft.recipe.gt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * Host-neutral compact family snapshot: epoch, candidate indexes,
 * parameterized materialization, and a bounded lazy cache.
 *
 * <p>Core behavior is not gated on a specific RecipeMap id. Unknown target
 * maps fail closed in {@link #prepareByTarget(List, Map, long, RuntimeSide,
 * MaterializationPolicy)}.
 */
public final class CompactRecipeFamilyProvider {
    private CompactRecipeFamilyProvider() {}

    public static String familyId(ResourceLocation mapId) {
        Objects.requireNonNull(mapId, "mapId");
        return "compact:" + mapId;
    }

    public static Snapshot prepare(
            RecipeMap map,
            List<CompactRecipeFamilySource> sources,
            long epoch,
            RuntimeSide side,
            MaterializationPolicy policy) {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(policy, "policy");
        if (side == RuntimeSide.INTEGRATED_CLIENT) {
            throw new IllegalArgumentException(
                    "Integrated clients must reuse the server compact family snapshot");
        }
        if (epoch <= 0L) {
            throw new IllegalArgumentException(
                    "Compact family epoch must be positive");
        }
        List<IndexedRelation> relations = collectRelations(map.id(), sources);
        return new Snapshot(map.id(), epoch, side, policy, relations);
    }

    /**
     * Groups authored compact sources by {@code target_map}, fails closed on
     * unknown maps, and builds one snapshot per populated map.
     */
    public static Map<ResourceLocation, Snapshot> prepareByTarget(
            List<CompactRecipeFamilySource> sources,
            Map<ResourceLocation, RecipeMap> knownMaps,
            long epoch,
            RuntimeSide side,
            MaterializationPolicy policy) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(knownMaps, "knownMaps");
        LinkedHashMap<ResourceLocation, List<CompactRecipeFamilySource>> grouped =
                new LinkedHashMap<>();
        for (CompactRecipeFamilySource source : sources) {
            ResourceLocation target = source.definition().targetMap();
            RecipeMap map = knownMaps.get(target);
            if (map == null) {
                throw new IllegalArgumentException(
                        "Unknown compact family target map " + target
                                + " in source " + source.id());
            }
            grouped.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
        }
        LinkedHashMap<ResourceLocation, Snapshot> snapshots = new LinkedHashMap<>();
        for (var entry : grouped.entrySet()) {
            snapshots.put(
                    entry.getKey(),
                    prepare(
                            knownMaps.get(entry.getKey()),
                            entry.getValue(),
                            epoch,
                            side,
                            policy));
        }
        return Map.copyOf(snapshots);
    }

    /**
     * Frozen T37 production selector: duration ≤ 16 ticks or the first 10
     * family ids by sort, cache ceiling 8. Matches the hybrid boundary in
     * {@code tools/t37_materialization_policy.json}.
     */
    public static MaterializationPolicy t37ProductionPolicy(
            List<CompactRecipeFamilySource> sources) {
        Objects.requireNonNull(sources, "sources");
        List<String> familyIds = sources.stream()
                .map(source -> source.definition().familyId())
                .sorted()
                .toList();
        Set<String> firstTen = new HashSet<>(
                familyIds.subList(0, Math.min(10, familyIds.size())));
        Set<ResourceLocation> eagerStableIds = new HashSet<>();
        for (CompactRecipeFamilySource source : sources) {
            List<CompactGTRecipeFamilyDefinition.Relation> relations =
                    source.definition().relations();
            if (relations.isEmpty()) {
                continue;
            }
            CompactGTRecipeFamilyDefinition.Relation relation = relations.get(0);
            if (relation.duration() <= 16
                    || firstTen.contains(source.definition().familyId())) {
                eagerStableIds.add(relation.stableId());
            }
        }
        return MaterializationPolicy.hybrid(
                8,
                (index, relation) -> eagerStableIds.contains(relation.stableId()));
    }

    private static List<IndexedRelation> collectRelations(
            ResourceLocation mapId,
            List<CompactRecipeFamilySource> sources) {
        Set<ResourceLocation> sourceIds = new HashSet<>();
        Set<String> authoredFamilyIds = new HashSet<>();
        List<IndexedRelation> relations = new ArrayList<>();
        for (CompactRecipeFamilySource source : sources) {
            CompactGTRecipeFamilyDefinition definition = source.definition();
            if (!sourceIds.add(source.id())) {
                throw new IllegalArgumentException(
                        "Duplicate compact authored id " + source.id());
            }
            if (!authoredFamilyIds.add(definition.familyId())) {
                throw new IllegalArgumentException(
                        "Duplicate compact family_id " + definition.familyId()
                                + " in source " + source.id());
            }
            if (!definition.targetMap().equals(mapId)) {
                throw new IllegalArgumentException(
                        "Compact source " + source.id()
                                + " targets " + definition.targetMap()
                                + " instead of " + mapId);
            }
            if (definition.parameterized().isPresent()) {
                throw new IllegalArgumentException(
                        "Parameterized compact families are not implemented: source "
                                + source.id()
                                + ", template "
                                + definition.parameterized().orElseThrow().template());
            }
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : definition.relations()) {
                relations.add(new IndexedRelation(
                        source,
                        relation,
                        indexKeys(relation)));
            }
        }
        relations.sort(Comparator
                .comparingInt((IndexedRelation value) -> value.relation().shadowOrder())
                .thenComparing(value -> value.relation().stableId().toString()));
        Set<ResourceLocation> stableIds = new HashSet<>();
        for (IndexedRelation indexed : relations) {
            if (!stableIds.add(indexed.relation().stableId())) {
                throw new IllegalArgumentException(
                        "Duplicate compact stable id "
                                + indexed.relation().stableId()
                                + " in source " + indexed.source().id());
            }
        }
        return List.copyOf(relations);
    }

    private static IndexKeys indexKeys(
            CompactGTRecipeFamilyDefinition.Relation relation) {
        Set<Item> items = new HashSet<>();
        Set<ComponentIngredientIndex.Key> components = new HashSet<>();
        Set<Fluid> fluids = new HashSet<>();
        boolean unindexed = false;
        for (Ingredient ingredient : relation.itemInputs()) {
            if (ingredient.isSimple()) {
                ItemStack[] stacks = ingredient.getItems();
                if (stacks.length == 0) {
                    unindexed = true;
                    continue;
                }
                for (ItemStack stack : stacks) {
                    if (!stack.isEmpty()) {
                        items.add(stack.getItem());
                    }
                }
            } else {
                ComponentIngredientIndex.Extraction extraction =
                        ComponentIngredientIndex.extract(ingredient);
                if (extraction.supported()) {
                    components.addAll(extraction.keys());
                } else {
                    unindexed = true;
                }
            }
        }
        for (FluidStack stack : relation.fluidInputs()) {
            if (!stack.isEmpty()) {
                fluids.add(stack.getFluid());
            }
        }
        if (items.isEmpty() && components.isEmpty() && fluids.isEmpty()) {
            unindexed = true;
        }
        return new IndexKeys(
                Set.copyOf(items),
                Set.copyOf(components),
                Set.copyOf(fluids),
                unindexed);
    }

    public enum RuntimeSide {
        SERVER,
        DEDICATED_CLIENT,
        INTEGRATED_CLIENT
    }

    public enum MaterializationStrategy {
        IMMEDIATE,
        ON_DEMAND,
        HYBRID
    }

    @FunctionalInterface
    public interface EagerSelector {
        boolean isEager(int index, CompactGTRecipeFamilyDefinition.Relation relation);

        EagerSelector ALL = (index, relation) -> true;
        EagerSelector NONE = (index, relation) -> false;
    }

    public record MaterializationPolicy(
            MaterializationStrategy strategy,
            int cacheCeiling,
            EagerSelector eagerSelector) {
        public MaterializationPolicy {
            Objects.requireNonNull(strategy, "strategy");
            Objects.requireNonNull(eagerSelector, "eagerSelector");
            if (cacheCeiling < 0) {
                throw new IllegalArgumentException(
                        "Compact family cache ceiling must not be negative");
            }
            if (strategy == MaterializationStrategy.IMMEDIATE && cacheCeiling != 0) {
                throw new IllegalArgumentException(
                        "Immediate compact materialization uses cache ceiling 0");
            }
        }

        public static MaterializationPolicy immediate() {
            return new MaterializationPolicy(
                    MaterializationStrategy.IMMEDIATE, 0, EagerSelector.ALL);
        }

        public static MaterializationPolicy onDemand(int cacheCeiling) {
            return new MaterializationPolicy(
                    MaterializationStrategy.ON_DEMAND,
                    cacheCeiling,
                    EagerSelector.NONE);
        }

        public static MaterializationPolicy hybrid(
                int cacheCeiling,
                EagerSelector eagerSelector) {
            Objects.requireNonNull(eagerSelector, "eagerSelector");
            return new MaterializationPolicy(
                    MaterializationStrategy.HYBRID, cacheCeiling, eagerSelector);
        }

        boolean isEager(int index, CompactGTRecipeFamilyDefinition.Relation relation) {
            return switch (strategy) {
                case IMMEDIATE -> true;
                case ON_DEMAND -> false;
                case HYBRID -> eagerSelector.isEager(index, relation);
            };
        }
    }

    public static final class Snapshot implements RecipeMap.RecipeFamily {
        private final ResourceLocation mapId;
        private final long epoch;
        private final RuntimeSide side;
        private final MaterializationPolicy policy;
        private final List<IndexedRelation> relations;
        private final List<ResourceLocation> recipeIds;
        private final Map<ResourceLocation, IndexedRelation> byId;
        private final Map<ResourceLocation, RecipeMap.Entry> eagerById;
        private final List<RecipeMap.Entry> eagerEntries;
        private final Map<Item, List<IndexedRelation>> lazyByItem;
        private final Map<ComponentIngredientIndex.Key, List<IndexedRelation>>
                lazyByComponent;
        private final Map<Fluid, List<IndexedRelation>> lazyByFluid;
        private final List<IndexedRelation> unindexedLazy;
        private final int unindexedRelationCount;
        private final String stableFingerprint;
        private final long syncPayloadBytes;
        private final LinkedHashMap<ResourceLocation, RecipeMap.Entry> cache;

        private Snapshot(
                ResourceLocation mapId,
                long epoch,
                RuntimeSide side,
                MaterializationPolicy policy,
                List<IndexedRelation> relations) {
            this.mapId = mapId;
            this.epoch = epoch;
            this.side = side;
            this.policy = policy;
            this.relations = relations;
            this.recipeIds = relations.stream()
                    .map(value -> value.relation().stableId())
                    .toList();
            Map<ResourceLocation, IndexedRelation> mutableById = new HashMap<>();
            Map<ResourceLocation, RecipeMap.Entry> mutableEager = new HashMap<>();
            List<RecipeMap.Entry> orderedEager = new ArrayList<>();
            Map<Item, List<IndexedRelation>> mutableByItem = new HashMap<>();
            Map<ComponentIngredientIndex.Key, List<IndexedRelation>> mutableByComponent =
                    new HashMap<>();
            Map<Fluid, List<IndexedRelation>> mutableByFluid = new HashMap<>();
            List<IndexedRelation> mutableUnindexed = new ArrayList<>();
            int unindexedCount = 0;
            for (int index = 0; index < relations.size(); index++) {
                IndexedRelation indexed = relations.get(index);
                CompactGTRecipeFamilyDefinition.Relation relation = indexed.relation();
                ResourceLocation id = relation.stableId();
                mutableById.put(id, indexed);
                if (policy.isEager(index, relation)) {
                    RecipeMap.Entry entry = materializeUncached(indexed);
                    mutableEager.put(id, entry);
                    orderedEager.add(entry);
                    continue;
                }
                if (indexed.keys().unindexed()) {
                    mutableUnindexed.add(indexed);
                    unindexedCount++;
                }
                for (Item item : indexed.keys().items()) {
                    mutableByItem.computeIfAbsent(item, ignored -> new ArrayList<>())
                            .add(indexed);
                }
                for (ComponentIngredientIndex.Key key : indexed.keys().components()) {
                    mutableByComponent.computeIfAbsent(key, ignored -> new ArrayList<>())
                            .add(indexed);
                }
                for (Fluid fluid : indexed.keys().fluids()) {
                    mutableByFluid.computeIfAbsent(fluid, ignored -> new ArrayList<>())
                            .add(indexed);
                }
            }
            this.byId = Map.copyOf(mutableById);
            this.eagerById = Map.copyOf(mutableEager);
            this.eagerEntries = List.copyOf(orderedEager);
            this.lazyByItem = copyIndex(mutableByItem);
            this.lazyByComponent = copyIndex(mutableByComponent);
            this.lazyByFluid = copyIndex(mutableByFluid);
            this.unindexedLazy = List.copyOf(mutableUnindexed);
            this.unindexedRelationCount = unindexedCount;
            this.stableFingerprint = fingerprint(mapId, relations);
            this.syncPayloadBytes = payloadBytes(relations);
            this.cache = new LinkedHashMap<>(16, 0.75F, true);
        }

        public RuntimeSide side() {
            return side;
        }

        public ResourceLocation mapId() {
            return mapId;
        }

        public MaterializationPolicy policy() {
            return policy;
        }

        public int unindexedRelationCount() {
            return unindexedRelationCount;
        }

        public long syncPayloadBytes() {
            return syncPayloadBytes;
        }

        @Override
        public String familyId() {
            return CompactRecipeFamilyProvider.familyId(mapId);
        }

        @Override
        public long epoch() {
            return epoch;
        }

        @Override
        public int logicalRecipeCount() {
            return relations.size();
        }

        @Override
        public int eagerRecipeCount() {
            return eagerEntries.size();
        }

        @Override
        public int lazyRecipeCount() {
            return logicalRecipeCount() - eagerRecipeCount();
        }

        @Override
        public synchronized int cacheSize() {
            return cache.size();
        }

        @Override
        public int cacheCeiling() {
            return policy.cacheCeiling();
        }

        @Override
        public String stableFingerprint() {
            return stableFingerprint;
        }

        @Override
        public List<ResourceLocation> recipeIds() {
            return recipeIds;
        }

        @Override
        public List<RecipeMap.Entry> eagerEntries() {
            return eagerEntries;
        }

        @Override
        public RecipeMap.Entry enumerationEntry(int index) {
            IndexedRelation indexed = relations.get(index);
            RecipeMap.Entry eager = eagerById.get(indexed.relation().stableId());
            return eager != null ? eager : materializeUncached(indexed);
        }

        @Override
        public Optional<RecipeMap.Entry> entry(ResourceLocation id) {
            IndexedRelation indexed = byId.get(id);
            if (indexed == null) {
                return Optional.empty();
            }
            RecipeMap.Entry eager = eagerById.get(id);
            return Optional.of(eager != null ? eager : materializeUncached(indexed));
        }

        @Override
        public Optional<RecipeMap.Entry> findLazy(GTRecipeQuery query) {
            for (IndexedRelation indexed : lazyCandidates(query)) {
                RecipeMap.Entry entry = materializeCached(indexed);
                if (entry.recipe().matches(query)) {
                    return Optional.of(entry);
                }
            }
            return Optional.empty();
        }

        @Override
        public int indexedLazyCandidateCount(GTRecipeQuery query) {
            return lazyCandidates(query).size();
        }

        @Override
        public boolean hasLazyCandidate(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (lazyByItem.containsKey(stack.getItem())) {
                return true;
            }
            for (ComponentIngredientIndex.Key key : ComponentIngredientIndex.keys(stack)) {
                if (lazyByComponent.containsKey(key)) {
                    return true;
                }
            }
            return !unindexedLazy.isEmpty();
        }

        private TreeSet<IndexedRelation> lazyCandidates(GTRecipeQuery query) {
            TreeSet<IndexedRelation> candidates = new TreeSet<>(Comparator
                    .comparingInt((IndexedRelation value) -> value.relation().shadowOrder())
                    .thenComparing(value -> value.relation().stableId().toString()));
            for (ItemStack stack : query.itemInputsView()) {
                if (stack.isEmpty()) {
                    continue;
                }
                candidates.addAll(lazyByItem.getOrDefault(stack.getItem(), List.of()));
                for (ComponentIngredientIndex.Key key
                        : ComponentIngredientIndex.keys(stack)) {
                    candidates.addAll(lazyByComponent.getOrDefault(key, List.of()));
                }
            }
            for (FluidStack stack : query.fluidInputsView()) {
                if (!stack.isEmpty()) {
                    candidates.addAll(
                            lazyByFluid.getOrDefault(stack.getFluid(), List.of()));
                }
            }
            candidates.addAll(unindexedLazy);
            return candidates;
        }

        private synchronized RecipeMap.Entry materializeCached(IndexedRelation indexed) {
            ResourceLocation id = indexed.relation().stableId();
            RecipeMap.Entry existing = cache.get(id);
            if (existing != null) {
                return existing;
            }
            RecipeMap.Entry created = materializeUncached(indexed);
            if (policy.cacheCeiling() <= 0) {
                return created;
            }
            cache.put(id, created);
            while (cache.size() > policy.cacheCeiling()) {
                var iterator = cache.entrySet().iterator();
                iterator.next();
                iterator.remove();
            }
            return created;
        }

        private static RecipeMap.Entry materializeUncached(IndexedRelation indexed) {
            CompactGTRecipeFamilyDefinition.Relation relation = indexed.relation();
            GTRecipe recipe = relation.materialize();
            return new RecipeMap.Entry(relation.stableId(), recipe);
        }
    }

    private static <K> Map<K, List<IndexedRelation>> copyIndex(
            Map<K, List<IndexedRelation>> mutable) {
        Map<K, List<IndexedRelation>> copy = new HashMap<>();
        mutable.forEach((key, rows) -> copy.put(key, List.copyOf(rows)));
        return Map.copyOf(copy);
    }

    private static String fingerprint(
            ResourceLocation mapId,
            List<IndexedRelation> relations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(mapId.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            for (IndexedRelation indexed : relations) {
                CompactGTRecipeFamilyDefinition.Relation relation = indexed.relation();
                CompactRecipeFamilySource source = indexed.source();
                String row = source.definition().familyId()
                        + "|" + source.definition().sourceRevision()
                        + "|" + relation.shadowOrder()
                        + "|" + relation.stableId()
                        + "|" + relation.duration()
                        + "|" + relation.eut()
                        + "|" + relation.specialValue()
                        + "|" + relation.canBeBuffered()
                        + "|" + relation.itemInputCounts()
                        + "|" + relation.itemInputActions()
                        + "|" + relation.outputChances()
                        + "|" + relation.provenance().sourceKind()
                        + "|" + relation.provenance().selectedSourceRecipe().orElse("")
                        + "\n";
                digest.update(row.getBytes(StandardCharsets.UTF_8));
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static long payloadBytes(List<IndexedRelation> relations) {
        long bytes = 0L;
        for (IndexedRelation indexed : relations) {
            CompactGTRecipeFamilyDefinition.Relation relation = indexed.relation();
            bytes += encodedLength(relation.stableId().toString())
                    + encodedLength(indexed.source().definition().familyId())
                    + Integer.BYTES * 2L
                    + Long.BYTES * 2L
                    + 1L
                    + (long) relation.itemInputs().size() * Integer.BYTES
                    + (long) relation.fluidInputs().size() * Integer.BYTES
                    + (long) relation.itemOutputs().size() * Integer.BYTES
                    + (long) relation.fluidOutputs().size() * Integer.BYTES;
        }
        return bytes;
    }

    private static int encodedLength(String value) {
        return Integer.BYTES + value.getBytes(StandardCharsets.UTF_8).length;
    }

    private record IndexKeys(
            Set<Item> items,
            Set<ComponentIngredientIndex.Key> components,
            Set<Fluid> fluids,
            boolean unindexed) {}

    private record IndexedRelation(
            CompactRecipeFamilySource source,
            CompactGTRecipeFamilyDefinition.Relation relation,
            IndexKeys keys) {}
}
