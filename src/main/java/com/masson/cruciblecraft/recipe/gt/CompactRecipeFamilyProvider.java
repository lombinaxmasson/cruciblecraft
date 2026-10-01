package com.masson.cruciblecraft.recipe.gt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Host-neutral compact family snapshot: epoch, publication group, shards,
 * parameterized materialization, and a bounded lazy cache.
 *
 * <p>Core behavior is not gated on a specific RecipeMap id. Unknown target
 * maps and targets without an explicit policy fail closed in
 * {@link #prepareByPublicationGroup(List, Map, long, RuntimeSide, Map)}.
 */
public final class CompactRecipeFamilyProvider {
    private CompactRecipeFamilyProvider() {}

    public static String familyId(ResourceLocation mapId) {
        Objects.requireNonNull(mapId, "mapId");
        return "compact:" + mapId;
    }

    public static String familyId(
            ResourceLocation mapId,
            ResourceLocation publicationGroup) {
        Objects.requireNonNull(mapId, "mapId");
        Objects.requireNonNull(publicationGroup, "publicationGroup");
        return "compact:" + mapId + "/" + publicationGroup;
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
        sources = CompactTransportFragments.reassemble(sources);
        ResourceLocation publicationGroup = resolvePublicationGroup(
                map.id(), sources);
        List<IndexedRelation> relations = collectRelations(map.id(), sources, null);
        return new Snapshot(
                map.id(),
                publicationGroup,
                epoch,
                side,
                policy,
                relations);
    }

    /**
     * Compatibility adapter for callers that still own one publication group
     * per target map.
     *
     * <p>If a target contains more than one resolved group, the caller must
     * move to {@link #prepareByPublicationGroup(List, Map, long, RuntimeSide,
     * Map)} so no policy can be inherited across workloads.
     */
    public static Map<ResourceLocation, Snapshot> prepareByTarget(
            List<CompactRecipeFamilySource> sources,
            Map<ResourceLocation, RecipeMap> knownMaps,
            long epoch,
            RuntimeSide side,
            Map<ResourceLocation, MaterializationPolicy> policies) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(knownMaps, "knownMaps");
        Objects.requireNonNull(policies, "policies");
        sources = CompactTransportFragments.reassemble(sources);
        for (var entry : policies.entrySet()) {
            ResourceLocation target = Objects.requireNonNull(
                    entry.getKey(), "compact policy target map");
            Objects.requireNonNull(
                    entry.getValue(), "compact materialization policy for " + target);
            if (!knownMaps.containsKey(target)) {
                throw new IllegalArgumentException(
                        "Unknown compact family policy target map " + target);
            }
        }
        TreeMap<ResourceLocation, List<CompactRecipeFamilySource>> grouped =
                new TreeMap<>(Comparator.comparing(ResourceLocation::toString));
        Map<ResourceLocation, Set<ResourceLocation>> groupsByTarget =
                new HashMap<>();
        for (CompactRecipeFamilySource source : sources) {
            Objects.requireNonNull(source, "source");
            ResourceLocation target = source.definition().targetMap();
            RecipeMap map = knownMaps.get(target);
            if (map == null) {
                throw new IllegalArgumentException(
                        "Unknown compact family target map " + target
                                + " in source " + source.id());
            }
            if (!policies.containsKey(target)) {
                throw new IllegalArgumentException(
                        "Missing compact materialization policy for target map "
                                + target + " in source " + source.id());
            }
            ResourceLocation group =
                    source.definition().resolvedPublicationGroup();
            groupsByTarget.computeIfAbsent(target, ignored -> new HashSet<>())
                    .add(group);
            grouped.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
        }
        for (var entry : groupsByTarget.entrySet()) {
            if (entry.getValue().size() > 1) {
                throw new IllegalArgumentException(
                        "Compact target map " + entry.getKey() + " contains "
                                + entry.getValue().size()
                                + " publication groups; use "
                                + "prepareByPublicationGroup");
            }
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
                            policies.get(entry.getKey())));
        }
        return Collections.unmodifiableMap(snapshots);
    }

    /**
     * Builds one deterministic snapshot per populated target/group policy
     * unit. Every source and policy is validated before any snapshot is
     * returned.
     */
    public static Map<PublicationGroupKey, Snapshot> prepareByPublicationGroup(
            List<CompactRecipeFamilySource> sources,
            Map<ResourceLocation, RecipeMap> knownMaps,
            long epoch,
            RuntimeSide side,
            Map<PublicationGroupKey, MaterializationPolicy> policies) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(knownMaps, "knownMaps");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(policies, "policies");
        if (side == RuntimeSide.INTEGRATED_CLIENT) {
            throw new IllegalArgumentException(
                    "Integrated clients must reuse the server compact family snapshot");
        }
        if (epoch <= 0L) {
            throw new IllegalArgumentException(
                    "Compact family epoch must be positive");
        }
        sources = CompactTransportFragments.reassemble(sources);
        Map<CompactRecipeFamilySource, List<CompactGTRecipeFamilyDefinition.Relation>>
                expanded = new IdentityHashMap<>();
        for (CompactRecipeFamilySource source : sources) {
            expanded.put(source, CompactAuthoredMatrix.expand(source.definition()));
        }
        for (var entry : policies.entrySet()) {
            PublicationGroupKey key = Objects.requireNonNull(
                    entry.getKey(), "compact publication group policy key");
            Objects.requireNonNull(
                    entry.getValue(),
                    "compact materialization policy for " + key);
            if (!knownMaps.containsKey(key.targetMap())) {
                throw new IllegalArgumentException(
                        "Unknown compact family policy target map "
                                + key.targetMap() + " for publication group "
                                + key.publicationGroup());
            }
        }

        TreeMap<PublicationGroupKey, List<CompactRecipeFamilySource>> grouped =
                new TreeMap<>();
        for (CompactRecipeFamilySource source : sources) {
            Objects.requireNonNull(source, "source");
            ResourceLocation target = source.definition().targetMap();
            if (!knownMaps.containsKey(target)) {
                throw new IllegalArgumentException(
                        "Unknown compact family target map " + target
                                + " in source " + source.id());
            }
            PublicationGroupKey key = new PublicationGroupKey(
                    target,
                    source.definition().resolvedPublicationGroup());
            MaterializationPolicy policy = policies.get(key);
            if (policy == null) {
                throw new IllegalArgumentException(
                        "Missing compact materialization policy for publication group "
                                + key.publicationGroup() + " on target map "
                                + key.targetMap() + " in source " + source.id());
            }
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(source);
        }
        validateCrossGroupCollisions(grouped, expanded);

        LinkedHashMap<PublicationGroupKey, Snapshot> snapshots =
                new LinkedHashMap<>();
        for (var entry : grouped.entrySet()) {
            PublicationGroupKey key = entry.getKey();
            Snapshot snapshot = prepareExpanded(
                    knownMaps.get(key.targetMap()),
                    entry.getValue(),
                    expanded,
                    epoch,
                    side,
                    policies.get(key));
            if (!snapshot.publicationGroup().equals(key.publicationGroup())) {
                throw new IllegalArgumentException(
                        "Compact snapshot publication group drifted from " + key);
            }
            snapshots.put(key, snapshot);
        }
        return Collections.unmodifiableMap(snapshots);
    }

    private static Snapshot prepareExpanded(
            RecipeMap map,
            List<CompactRecipeFamilySource> sources,
            Map<CompactRecipeFamilySource, List<CompactGTRecipeFamilyDefinition.Relation>>
                    expanded,
            long epoch,
            RuntimeSide side,
            MaterializationPolicy policy) {
        ResourceLocation publicationGroup = resolvePublicationGroup(
                map.id(), sources);
        return new Snapshot(
                map.id(),
                publicationGroup,
                epoch,
                side,
                policy,
                collectRelations(map.id(), sources, expanded));
    }

    private static List<IndexedRelation> collectRelations(
            ResourceLocation mapId,
            List<CompactRecipeFamilySource> sources,
            Map<CompactRecipeFamilySource, List<CompactGTRecipeFamilyDefinition.Relation>>
                    expanded) {
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
            try {
                List<CompactGTRecipeFamilyDefinition.Relation> authored =
                        expanded == null
                                ? CompactAuthoredMatrix.expand(definition)
                                : expanded.get(source);
                for (CompactGTRecipeFamilyDefinition.Relation relation : authored) {
                    relations.add(new IndexedRelation(source, relation));
                }
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException(
                        "Compact source " + source.id() + ": " + failure.getMessage(),
                        failure);
            }
        }
        relations.sort(Comparator
                .comparingInt((IndexedRelation value) -> value.relation().shadowOrder())
                .thenComparing(
                        value -> value.relation().stableId(),
                        CompactRecipeFamilyProvider::compareLocation));
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

    private static ResourceLocation resolvePublicationGroup(
            ResourceLocation mapId,
            List<CompactRecipeFamilySource> sources) {
        if (sources.isEmpty()) {
            return CompactGTRecipeFamilyDefinition.historicalPublicationGroup(
                    mapId);
        }
        ResourceLocation resolved = null;
        for (CompactRecipeFamilySource source : sources) {
            Objects.requireNonNull(source, "source");
            CompactGTRecipeFamilyDefinition definition = source.definition();
            if (!definition.targetMap().equals(mapId)) {
                throw new IllegalArgumentException(
                        "Compact source " + source.id()
                                + " targets " + definition.targetMap()
                                + " instead of " + mapId);
            }
            ResourceLocation candidate =
                    definition.resolvedPublicationGroup();
            if (resolved == null) {
                resolved = candidate;
            } else if (!resolved.equals(candidate)) {
                throw new IllegalArgumentException(
                        "Compact snapshot for target " + mapId
                                + " mixes publication groups " + resolved
                                + " and " + candidate);
            }
        }
        return Objects.requireNonNull(resolved, "resolved publication group");
    }

    private static int compareLocation(
            ResourceLocation left, ResourceLocation right) {
        int namespace = left.getNamespace().compareTo(right.getNamespace());
        if (namespace != 0) {
            return namespace;
        }
        return left.getPath().compareTo(right.getPath());
    }

    private static void validateCrossGroupCollisions(
            Map<PublicationGroupKey, List<CompactRecipeFamilySource>> grouped,
            Map<CompactRecipeFamilySource, List<CompactGTRecipeFamilyDefinition.Relation>>
                    expanded) {
        Map<ResourceLocation, Integer> groupsOnTarget = new HashMap<>();
        for (PublicationGroupKey key : grouped.keySet()) {
            groupsOnTarget.merge(key.targetMap(), 1, Integer::sum);
        }
        Map<ResourceLocation, Map<ResourceLocation, PublicationGroupKey>>
                authoredOwners = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, PublicationGroupKey>>
                stableOwners = new HashMap<>();
        Map<ResourceLocation, Map<String, PublicationGroupKey>>
                logicalOwners = new HashMap<>();
        Map<ResourceLocation, Map<String, String>>
                logicalOutputs = new HashMap<>();
        Map<Ingredient, String> ingredientTails =
                GTRecipeMapLoader.newIngredientTailCache();
        for (var entry : grouped.entrySet()) {
            PublicationGroupKey key = entry.getKey();
            if (groupsOnTarget.get(key.targetMap()) < 2) {
                continue;
            }
            Map<ResourceLocation, PublicationGroupKey> targetAuthored =
                    authoredOwners.computeIfAbsent(
                            key.targetMap(), ignored -> new HashMap<>());
            Map<ResourceLocation, PublicationGroupKey> targetStable =
                    stableOwners.computeIfAbsent(
                            key.targetMap(), ignored -> new HashMap<>());
            Map<String, PublicationGroupKey> targetLogical =
                    logicalOwners.computeIfAbsent(
                            key.targetMap(), ignored -> new HashMap<>());
            Map<String, String> targetOutputs =
                    logicalOutputs.computeIfAbsent(
                            key.targetMap(), ignored -> new HashMap<>());
            for (CompactRecipeFamilySource source : entry.getValue()) {
                PublicationGroupKey authoredPrevious =
                        targetAuthored.putIfAbsent(source.id(), key);
                if (authoredPrevious != null && !authoredPrevious.equals(key)) {
                    throw new IllegalArgumentException(
                            "Duplicate compact authored id " + source.id()
                                    + " across publication groups "
                                    + authoredPrevious.publicationGroup()
                                    + " and " + key.publicationGroup()
                                    + " on target " + key.targetMap());
                }
                List<CompactGTRecipeFamilyDefinition.Relation> relations =
                        expanded.get(source);
                if (relations == null) {
                    relations = source.authoredRelations();
                }
                for (CompactGTRecipeFamilyDefinition.Relation relation : relations) {
                    PublicationGroupKey stablePrevious =
                            targetStable.putIfAbsent(relation.stableId(), key);
                    if (stablePrevious != null && !stablePrevious.equals(key)) {
                        throw new IllegalArgumentException(
                                "Duplicate compact stable id "
                                        + relation.stableId()
                                        + " across publication groups "
                                        + stablePrevious.publicationGroup()
                                        + " and " + key.publicationGroup()
                                        + " on target " + key.targetMap());
                    }
                    String logicalIdentity = GTRecipeMapLoader.inputSignature(
                            relation.itemInputs(),
                            relation.itemInputCounts(),
                            relation.itemInputActions(),
                            relation.fluidInputs(),
                            ingredientTails);
                    String outputIdentity = GTRecipeMapLoader.outputSignature(
                            relation.itemOutputs(),
                            relation.fluidOutputs())
                            + "|" + relation.duration()
                            + "|" + relation.eut();
                    PublicationGroupKey logicalPrevious =
                            targetLogical.putIfAbsent(logicalIdentity, key);
                    if (logicalPrevious != null && !logicalPrevious.equals(key)) {
                        String previousOutput = targetOutputs.get(logicalIdentity);
                        if (previousOutput == null
                                || !previousOutput.equals(outputIdentity)) {
                            throw new IllegalArgumentException(
                                    "Duplicate compact logical identity across "
                                            + "publication groups "
                                            + logicalPrevious.publicationGroup()
                                            + " and " + key.publicationGroup()
                                            + " on target " + key.targetMap());
                        }
                    } else {
                        targetOutputs.put(logicalIdentity, outputIdentity);
                    }
                }
            }
        }
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
        private final ResourceLocation publicationGroup;
        private final long epoch;
        private final RuntimeSide side;
        private final MaterializationPolicy policy;
        private final List<IndexedRelation> relations;
        private final List<ResourceLocation> recipeIds;
        private final Map<ResourceLocation, IndexedRelation> byId;
        private final Map<ResourceLocation, RecipeMap.Entry> eagerById;
        private final List<RecipeMap.Entry> eagerEntries;
        private final Set<ResourceLocation> lazyIds;
        private final CompactRecipeShardRouter shardRouter;
        private final int unindexedRelationCount;
        private final String stableFingerprint;
        private final long syncPayloadBytes;
        private final LinkedHashMap<ResourceLocation, RecipeMap.Entry> cache;

        private Snapshot(
                ResourceLocation mapId,
                ResourceLocation publicationGroup,
                long epoch,
                RuntimeSide side,
                MaterializationPolicy policy,
                List<IndexedRelation> relations) {
            this.mapId = Objects.requireNonNull(mapId, "mapId");
            this.publicationGroup = Objects.requireNonNull(
                    publicationGroup, "publicationGroup");
            this.epoch = epoch;
            this.side = Objects.requireNonNull(side, "side");
            this.policy = Objects.requireNonNull(policy, "policy");
            this.relations = List.copyOf(relations);
            this.recipeIds = this.relations.stream()
                    .map(value -> value.relation().stableId())
                    .toList();
            this.shardRouter = new CompactRecipeShardRouter(
                    mapId,
                    publicationGroup,
                    this.relations.stream()
                            .map(IndexedRelation::relation)
                            .toList());
            Map<ResourceLocation, IndexedRelation> mutableById = new HashMap<>();
            Map<ResourceLocation, RecipeMap.Entry> mutableEager = new HashMap<>();
            List<RecipeMap.Entry> orderedEager = new ArrayList<>();
            Set<ResourceLocation> mutableLazyIds = new HashSet<>();
            for (int index = 0; index < this.relations.size(); index++) {
                IndexedRelation indexed = this.relations.get(index);
                CompactGTRecipeFamilyDefinition.Relation relation = indexed.relation();
                ResourceLocation id = relation.stableId();
                mutableById.put(id, indexed);
                if (policy.isEager(index, relation)) {
                    RecipeMap.Entry entry = materializeUncached(indexed);
                    mutableEager.put(id, entry);
                    orderedEager.add(entry);
                    continue;
                }
                mutableLazyIds.add(id);
            }
            this.byId = Map.copyOf(mutableById);
            this.eagerById = Map.copyOf(mutableEager);
            this.eagerEntries = List.copyOf(orderedEager);
            this.lazyIds = Set.copyOf(mutableLazyIds);
            this.unindexedRelationCount = shardRouter.overflowCount();
            this.stableFingerprint = fingerprint(
                    mapId, publicationGroup, this.relations);
            this.syncPayloadBytes = estimateStubPayloadBytes(this.relations);
            this.cache = new LinkedHashMap<>(16, 0.75F, true);
        }

        public RuntimeSide side() {
            return side;
        }

        public ResourceLocation mapId() {
            return mapId;
        }

        public ResourceLocation publicationGroup() {
            return publicationGroup;
        }

        public CompactRecipeShardRouter shardRouter() {
            return shardRouter;
        }

        public int shardCount() {
            return shardRouter.shardCount();
        }

        public int overflowRelationCount() {
            return shardRouter.overflowCount();
        }

        public Optional<String> shardId(ResourceLocation stableId) {
            return shardRouter.shardId(stableId);
        }

        public Set<String> routedShardIds(GTRecipeQuery query) {
            return shardRouter.routedShardIds(query);
        }

        public MaterializationPolicy policy() {
            return policy;
        }

        public int unindexedRelationCount() {
            return unindexedRelationCount;
        }

        /**
         * Stub size estimate used by publication telemetry. This is <strong>not</strong>
         * compact family wire size; per-entry proof is
         * {@link CompactGTRecipeFamilySerializer#streamCodec()}.
         */
        public long syncPayloadBytes() {
            return syncPayloadBytes;
        }

        @Override
        public String familyId() {
            return usesHistoricalFamilyIdentity(mapId, publicationGroup)
                    ? CompactRecipeFamilyProvider.familyId(mapId)
                    : CompactRecipeFamilyProvider.familyId(
                            mapId, publicationGroup);
        }

        @Override
        public long epoch() {
            return epoch;
        }

        @Override
        public int logicalRecipeCount() {
            return relations.size();
        }

        public CompactGTRecipeFamilyDefinition.Relation logicalRelation(int index) {
            return relations.get(index).relation();
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
        public Optional<RecipeMap.Entry> findLazyAtMostEut(
                GTRecipeQuery query,
                long maximumEut,
                java.util.function.Predicate<RecipeMap.Entry> accepted) {
            for (IndexedRelation indexed : lazyCandidates(query)) {
                if (indexed.relation().eut() > maximumEut) {
                    continue;
                }
                RecipeMap.Entry entry = materializeCached(indexed);
                if (entry.recipe().matches(query) && accepted.test(entry)) {
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
            for (String shardId : shardRouter.candidateShardIds(stack)) {
                for (ResourceLocation stableId
                        : shardRouter.shardMembers(shardId)) {
                    if (lazyIds.contains(stableId)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private List<IndexedRelation> lazyCandidates(GTRecipeQuery query) {
            return shardRouter.routedCandidates(query).stream()
                    .filter(relation -> lazyIds.contains(relation.stableId()))
                    .map(relation -> byId.get(relation.stableId()))
                    .toList();
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

    private static boolean usesHistoricalFamilyIdentity(
            ResourceLocation mapId,
            ResourceLocation publicationGroup) {
        try {
            return CompactGTRecipeFamilyDefinition
                    .historicalPublicationGroup(mapId)
                    .equals(publicationGroup);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void updateUtf8(MessageDigest digest, String text) {
        digest.update(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String fingerprint(
            ResourceLocation mapId,
            ResourceLocation publicationGroup,
            List<IndexedRelation> relations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(mapId.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            if (!usesHistoricalFamilyIdentity(mapId, publicationGroup)) {
                digest.update(publicationGroup.toString().getBytes(
                        StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            for (IndexedRelation indexed : relations) {
                CompactGTRecipeFamilyDefinition.Relation relation = indexed.relation();
                CompactRecipeFamilySource source = indexed.source();
                updateUtf8(digest, source.definition().familyId());
                digest.update((byte) '|');
                updateUtf8(digest, source.definition().sourceRevision());
                digest.update((byte) '|');
                updateUtf8(digest, Integer.toString(relation.shadowOrder()));
                digest.update((byte) '|');
                updateUtf8(digest, relation.stableId().toString());
                digest.update((byte) '|');
                updateUtf8(digest, Integer.toString(relation.duration()));
                digest.update((byte) '|');
                updateUtf8(digest, Long.toString(relation.eut()));
                digest.update((byte) '|');
                updateUtf8(digest, Long.toString(relation.specialValue()));
                digest.update((byte) '|');
                updateUtf8(digest, Boolean.toString(relation.canBeBuffered()));
                digest.update((byte) '|');
                updateUtf8(digest, String.valueOf(relation.itemInputCounts()));
                digest.update((byte) '|');
                updateUtf8(digest, String.valueOf(relation.itemInputActions()));
                digest.update((byte) '|');
                updateUtf8(digest, String.valueOf(relation.outputChances()));
                digest.update((byte) '|');
                updateUtf8(digest, String.valueOf(relation.provenance().sourceKind()));
                digest.update((byte) '|');
                updateUtf8(digest, relation.provenance()
                        .selectedSourceRecipe()
                        .orElse(""));
                digest.update((byte) '\n');
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * Counts ids, list lengths, and a few fixed numbers. It does not encode
     * Ingredient, ItemStack, FluidStack, or provenance and must not be used as
     * sync proof.
     */
    private static long estimateStubPayloadBytes(List<IndexedRelation> relations) {
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

    private record IndexedRelation(
            CompactRecipeFamilySource source,
            CompactGTRecipeFamilyDefinition.Relation relation) {}
}
