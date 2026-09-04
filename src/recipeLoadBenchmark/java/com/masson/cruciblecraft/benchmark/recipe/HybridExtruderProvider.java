package com.masson.cruciblecraft.benchmark.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.CachePolicy;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRecipe;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRelation;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.LookupRequest;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ProviderDiagnostics;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.Publication;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.RecipeEnumeration;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.RuntimeSide;

/**
 * Eagerly materializes the declared hot partition and leaves the long tail
 * indexed by relation metadata.
 */
public final class HybridExtruderProvider extends AbstractExtruderProvider {
    public static final int CACHE_CEILING = 512;

    private volatile Map<String, List<ExtruderRelation>> eagerByInput = Map.of();
    private volatile Map<String, List<ExtruderRelation>> lazyByInput = Map.of();
    private volatile Map<String, ExtruderRecipe> eagerByRelationId = Map.of();
    private volatile List<ExtruderRecipe> eagerByOrdinal = List.of();
    private volatile LinkedHashMap<String, ExtruderRecipe> longTailCache =
            new LinkedHashMap<>(64, 0.75F, true);
    private volatile long cacheEpoch;
    private long cacheHits;
    private long cacheMisses;
    private long cacheMaterializations;
    private long cacheEvictions;

    @Override
    public String candidateId() {
        return "hybrid";
    }

    @Override
    public synchronized Publication publish(
            List<ExtruderRelation> relations,
            long epoch,
            RuntimeSide side) {
        validatePublication(relations, epoch, side);
        long reloadStarted = System.nanoTime();
        long expansionStarted = System.nanoTime();
        List<ExtruderRelation> relationSnapshot = List.copyOf(relations);
        Map<String, ExtruderRecipe> eager = new HashMap<>();
        List<ExtruderRecipe> ordinalRecipes =
                new ArrayList<>(java.util.Collections.nCopies(
                        relationSnapshot.size(), null));
        for (ExtruderRelation relation : relationSnapshot) {
            if (relation.eagerEligible()) {
                ExtruderRecipe recipe = ExtruderBenchmarkModel.materialize(relation);
                eager.put(relation.relationId(), recipe);
                ordinalRecipes.set(relation.ordinal(), recipe);
            }
        }
        Map<String, ExtruderRecipe> eagerSnapshot = Map.copyOf(eager);
        List<ExtruderRecipe> eagerOrdinalSnapshot =
                java.util.Collections.unmodifiableList(
                        new ArrayList<>(ordinalRecipes));
        LinkedHashMap<String, ExtruderRecipe> nextCache =
                new LinkedHashMap<>(64, 0.75F, true);
        long expansionNanos = System.nanoTime() - expansionStarted;

        long indexStarted = System.nanoTime();
        Map<String, List<ExtruderRelation>> eagerIndex = immutableInputIndex(
                relationSnapshot.stream()
                        .filter(ExtruderRelation::eagerEligible)
                        .toList(),
                ExtruderRelation::inputKey);
        Map<String, List<ExtruderRelation>> lazyIndex = immutableInputIndex(
                relationSnapshot.stream()
                        .filter(relation -> !relation.eagerEligible())
                        .toList(),
                ExtruderRelation::inputKey);
        long indexNanos = System.nanoTime() - indexStarted;
        long syncBytes = ExtruderBenchmarkModel.relationPayloadBytes(
                relationSnapshot);

        eagerByInput = eagerIndex;
        lazyByInput = lazyIndex;
        eagerByRelationId = eagerSnapshot;
        eagerByOrdinal = eagerOrdinalSnapshot;
        longTailCache = nextCache;
        cacheEpoch = epoch;
        cacheHits = 0L;
        cacheMisses = 0L;
        cacheMaterializations = 0L;
        cacheEvictions = 0L;
        commitCommon(relationSnapshot, epoch, side, syncBytes);
        long reloadNanos = System.nanoTime() - reloadStarted;
        return new Publication(
                epoch,
                side,
                expansionNanos,
                indexNanos,
                reloadNanos,
                relationSnapshot.size(),
                eagerSnapshot.size());
    }

    @Override
    public Optional<ExtruderRecipe> lookup(LookupRequest request) {
        long snapshotEpoch = publishedEpoch;
        boolean eagerRequest = eagerByRelationId.containsKey(
                request.relationId());
        List<ExtruderRelation> candidates = (eagerRequest
                ? eagerByInput : lazyByInput)
                .getOrDefault(request.inputKey(), List.of());
        for (ExtruderRelation relation : candidates) {
            if (!relation.relationId().equals(request.relationId())) {
                continue;
            }
            ExtruderRecipe eager = eagerByRelationId.get(relation.relationId());
            ExtruderRecipe recipe = eager != null
                    ? eager
                    : materializeLongTail(relation, snapshotEpoch);
            if (snapshotEpoch != publishedEpoch || cacheEpoch != snapshotEpoch) {
                throw new IllegalStateException(
                        "Recipe epoch changed during hybrid lookup");
            }
            return Optional.of(recipe);
        }
        return Optional.empty();
    }

    private synchronized ExtruderRecipe materializeLongTail(
            ExtruderRelation relation,
            long expectedEpoch) {
        if (expectedEpoch != publishedEpoch || cacheEpoch != expectedEpoch) {
            throw new IllegalStateException(
                    "Recipe epoch changed before hybrid cache access");
        }
        ExtruderRecipe existing = longTailCache.get(relation.relationId());
        if (existing != null) {
            cacheHits++;
            return existing;
        }
        cacheMisses++;
        cacheMaterializations++;
        ExtruderRecipe created = ExtruderBenchmarkModel.materialize(relation);
        longTailCache.put(relation.relationId(), created);
        while (longTailCache.size() > CACHE_CEILING) {
            var iterator = longTailCache.entrySet().iterator();
            iterator.next();
            iterator.remove();
            cacheEvictions++;
        }
        return created;
    }

    @Override
    public int indexedCandidateCount(LookupRequest request) {
        return (eagerByRelationId.containsKey(request.relationId())
                ? eagerByInput : lazyByInput)
                .getOrDefault(request.inputKey(), List.of())
                .size();
    }

    @Override
    public RecipeEnumeration enumerationView() {
        List<ExtruderRelation> relations = publishedRelations;
        List<ExtruderRecipe> eager = eagerByOrdinal;
        return epochView(relations.size(), index -> {
            ExtruderRecipe recipe = eager.get(index);
            return recipe != null
                    ? recipe
                    : ExtruderBenchmarkModel.materialize(relations.get(index));
        });
    }

    @Override
    public synchronized ProviderDiagnostics diagnostics() {
        return new ProviderDiagnostics(
                publishedEpoch,
                publishedRelations.size(),
                eagerByInput.size() + lazyByInput.size(),
                eagerByRelationId.size(),
                longTailCache.size(),
                CACHE_CEILING,
                CachePolicy.BOUNDED_ACCESS_ORDER_LRU,
                cacheHits,
                cacheMisses,
                cacheMaterializations,
                cacheEvictions,
                cacheEpoch,
                publishedSide);
    }
}
