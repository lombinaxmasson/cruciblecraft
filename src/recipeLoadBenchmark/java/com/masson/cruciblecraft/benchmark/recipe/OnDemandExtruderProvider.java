package com.masson.cruciblecraft.benchmark.recipe;

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
 * Retains an indexed relation catalog and materializes recipes on lookup.
 * Lookup and enumeration are deliberately non-retaining: this candidate is
 * the zero-cache boundary rather than an unbounded memoization policy.
 */
public final class OnDemandExtruderProvider extends AbstractExtruderProvider {
    private volatile Map<String, List<ExtruderRelation>> byInput = Map.of();
    private volatile long cacheEpoch;
    private long cacheMisses;
    private long materializations;

    @Override
    public String candidateId() {
        return "on_demand";
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
        long expansionNanos = System.nanoTime() - expansionStarted;

        long indexStarted = System.nanoTime();
        Map<String, List<ExtruderRelation>> index = immutableInputIndex(
                relationSnapshot, ExtruderRelation::inputKey);
        long indexNanos = System.nanoTime() - indexStarted;
        long syncBytes = ExtruderBenchmarkModel.relationPayloadBytes(
                relationSnapshot);

        byInput = index;
        cacheEpoch = epoch;
        cacheMisses = 0L;
        materializations = 0L;
        commitCommon(relationSnapshot, epoch, side, syncBytes);
        long reloadNanos = System.nanoTime() - reloadStarted;
        return new Publication(
                epoch,
                side,
                expansionNanos,
                indexNanos,
                reloadNanos,
                relationSnapshot.size(),
                0);
    }

    @Override
    public Optional<ExtruderRecipe> lookup(LookupRequest request) {
        long snapshotEpoch = publishedEpoch;
        List<ExtruderRelation> candidates =
                byInput.getOrDefault(request.inputKey(), List.of());
        for (ExtruderRelation relation : candidates) {
            if (relation.relationId().equals(request.relationId())) {
                ExtruderRecipe recipe = materializeOnDemand(
                        relation, snapshotEpoch);
                if (snapshotEpoch != publishedEpoch || cacheEpoch != snapshotEpoch) {
                    throw new IllegalStateException(
                            "Recipe epoch changed during on-demand lookup");
                }
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private synchronized ExtruderRecipe materializeOnDemand(
            ExtruderRelation relation,
            long expectedEpoch) {
        if (expectedEpoch != publishedEpoch || cacheEpoch != expectedEpoch) {
            throw new IllegalStateException(
                    "Recipe epoch changed before on-demand materialization");
        }
        cacheMisses++;
        materializations++;
        return ExtruderBenchmarkModel.materialize(relation);
    }

    @Override
    public int indexedCandidateCount(LookupRequest request) {
        return byInput.getOrDefault(request.inputKey(), List.of()).size();
    }

    @Override
    public RecipeEnumeration enumerationView() {
        List<ExtruderRelation> snapshot = publishedRelations;
        return epochView(
                snapshot.size(),
                index -> ExtruderBenchmarkModel.materialize(snapshot.get(index)));
    }

    @Override
    public synchronized ProviderDiagnostics diagnostics() {
        return new ProviderDiagnostics(
                publishedEpoch,
                publishedRelations.size(),
                byInput.size(),
                0,
                0,
                0,
                CachePolicy.NO_CACHE,
                0L,
                cacheMisses,
                materializations,
                0L,
                cacheEpoch,
                publishedSide);
    }
}
