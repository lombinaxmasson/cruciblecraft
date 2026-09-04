package com.masson.cruciblecraft.benchmark.recipe;

import java.util.ArrayList;
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

/** Eagerly materializes and indexes every relation at publication. */
public final class ImmediateExtruderProvider extends AbstractExtruderProvider {
    private volatile List<ExtruderRecipe> recipes = List.of();
    private volatile Map<String, List<ExtruderRecipe>> byInput = Map.of();

    @Override
    public String candidateId() {
        return "immediate";
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
        List<ExtruderRecipe> expanded = new ArrayList<>(relationSnapshot.size());
        for (ExtruderRelation relation : relationSnapshot) {
            expanded.add(ExtruderBenchmarkModel.materialize(relation));
        }
        List<ExtruderRecipe> recipeSnapshot = List.copyOf(expanded);
        long expansionNanos = System.nanoTime() - expansionStarted;

        long indexStarted = System.nanoTime();
        Map<String, List<ExtruderRecipe>> index = immutableInputIndex(
                recipeSnapshot, ExtruderRecipe::inputKey);
        long indexNanos = System.nanoTime() - indexStarted;
        long syncBytes = ExtruderBenchmarkModel.concretePayloadBytes(recipeSnapshot);

        recipes = recipeSnapshot;
        byInput = index;
        commitCommon(relationSnapshot, epoch, side, syncBytes);
        long reloadNanos = System.nanoTime() - reloadStarted;
        return new Publication(
                epoch,
                side,
                expansionNanos,
                indexNanos,
                reloadNanos,
                relationSnapshot.size(),
                recipeSnapshot.size());
    }

    @Override
    public Optional<ExtruderRecipe> lookup(LookupRequest request) {
        List<ExtruderRecipe> candidates =
                byInput.getOrDefault(request.inputKey(), List.of());
        return candidates.stream()
                .filter(recipe -> recipe.relationId().equals(request.relationId()))
                .findFirst();
    }

    @Override
    public int indexedCandidateCount(LookupRequest request) {
        return byInput.getOrDefault(request.inputKey(), List.of()).size();
    }

    @Override
    public RecipeEnumeration enumerationView() {
        List<ExtruderRecipe> snapshot = recipes;
        return epochView(snapshot.size(), snapshot::get);
    }

    @Override
    public ProviderDiagnostics diagnostics() {
        return new ProviderDiagnostics(
                publishedEpoch,
                publishedRelations.size(),
                byInput.size(),
                recipes.size(),
                0,
                0,
                CachePolicy.FULLY_EAGER,
                0L,
                0L,
                0L,
                0L,
                publishedEpoch,
                publishedSide);
    }
}
