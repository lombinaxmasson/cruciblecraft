package com.masson.cruciblecraft.benchmark.recipe;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRecipe;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRelation;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.LookupRequest;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.RecipeEnumeration;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.RuntimeSide;

/** Dependency-free semantic tests for the isolated T14b candidate contract. */
public final class RecipeFamilyProviderContractTest {
    private static final int CANDIDATES_PER_INPUT = 4;
    private static final int HOT_MODULO = 5;

    private RecipeFamilyProviderContractTest() {}

    public static void main(String[] args) {
        List<ExtruderRelation> relations = ExtruderBenchmarkModel.syntheticRelations(
                41, CANDIDATES_PER_INPUT, HOT_MODULO);
        List<String> expectedIds = relations.stream()
                .map(ExtruderBenchmarkModel::materialize)
                .map(ExtruderRecipe::stableId)
                .toList();

        for (RecipeFamilyProvider provider : providers()) {
            verifyServerSemantics(provider, relations, expectedIds);
            verifyDedicatedClient(provider, relations, expectedIds);
            verifyIntegratedClientGate(provider, relations);
        }
        verifyCandidateSpecificBoundaries(relations);
        System.out.println(
                "T14b RecipeFamilyProvider contract tests passed for "
                        + providers().size() + " candidates.");
    }

    private static void verifyServerSemantics(
            RecipeFamilyProvider provider,
            List<ExtruderRelation> relations,
            List<String> expectedIds) {
        provider.publish(relations, 1L, RuntimeSide.SERVER);
        check(provider.epoch() == 1L, "server epoch was not published");
        check(provider.diagnostics().relations() == relations.size(),
                "relation count drifted");

        List<String> lookedUp = new ArrayList<>();
        for (ExtruderRelation relation : relations) {
            LookupRequest request = ExtruderBenchmarkModel.request(relation);
            int candidates = provider.indexedCandidateCount(request);
            check(candidates >= 1 && candidates <= CANDIDATES_PER_INPUT,
                    "lookup did not use the bounded input index");
            ExtruderRecipe recipe = provider.lookup(request).orElseThrow();
            check(recipe.relationId().equals(relation.relationId()),
                    "indexed lookup selected a different relation");
            lookedUp.add(recipe.stableId());
        }
        check(lookedUp.equals(expectedIds), "stable lookup ids drifted");

        int cacheBeforeEnumeration = provider.diagnostics().cachedRecipes();
        RecipeEnumeration staleView = provider.enumerationView();
        check(staleView.epoch() == 1L, "enumeration epoch drifted");
        check(enumeratedIds(staleView).equals(expectedIds),
                "server enumeration is incomplete or reordered");
        if (provider instanceof OnDemandExtruderProvider
                || provider instanceof HybridExtruderProvider) {
            check(provider.diagnostics().cachedRecipes() == cacheBeforeEnumeration,
                    "enumeration retained long-tail recipes");
        }

        provider.publish(relations, 2L, RuntimeSide.SERVER);
        check(provider.diagnostics().cacheEpoch() == 2L,
                "cache epoch was not advanced");
        check(provider.diagnostics().cachedRecipes() == 0,
                "prior-epoch lookup cache survived publication");
        expectFailure(staleView::size, IllegalStateException.class,
                "prior enumeration view survived epoch invalidation");
        expectFailure(
                () -> provider.publish(relations, 2L, RuntimeSide.SERVER),
                IllegalArgumentException.class,
                "duplicate epoch was accepted");
        check(provider.lookup(ExtruderBenchmarkModel.request(relations.get(7)))
                        .orElseThrow().stableId().equals(expectedIds.get(7)),
                "stable id changed across epochs");
    }

    private static void verifyDedicatedClient(
            RecipeFamilyProvider prototype,
            List<ExtruderRelation> relations,
            List<String> expectedIds) {
        RecipeFamilyProvider client = newProvider(prototype.candidateId());
        client.publish(relations, 17L, RuntimeSide.DEDICATED_CLIENT);
        check(client.diagnostics().side() == RuntimeSide.DEDICATED_CLIENT,
                "dedicated client publication side drifted");
        check(enumeratedIds(client.enumerationView()).equals(expectedIds),
                "dedicated client enumeration differs from the server contract");
        ExtruderRelation last = relations.getLast();
        check(client.lookup(ExtruderBenchmarkModel.request(last)).isPresent(),
                "dedicated client indexed lookup failed");
    }

    private static void verifyIntegratedClientGate(
            RecipeFamilyProvider prototype,
            List<ExtruderRelation> relations) {
        check(!RuntimeSide.INTEGRATED_CLIENT.requiresIndependentExpansion(),
                "integrated side unexpectedly requests re-expansion");
        RecipeFamilyProvider integrated = newProvider(prototype.candidateId());
        expectFailure(
                () -> integrated.publish(
                        relations, 1L, RuntimeSide.INTEGRATED_CLIENT),
                IllegalArgumentException.class,
                "integrated client performed a second expansion");
    }

    private static void verifyCandidateSpecificBoundaries(
            List<ExtruderRelation> relations) {
        RecipeFamilyProvider immediate = new ImmediateExtruderProvider();
        immediate.publish(relations, 1L, RuntimeSide.SERVER);
        check(immediate.diagnostics().eagerlyMaterializedRecipes()
                        == relations.size(),
                "Immediate did not materialize the complete relation set");

        RecipeFamilyProvider onDemand = new OnDemandExtruderProvider();
        onDemand.publish(relations, 1L, RuntimeSide.SERVER);
        check(onDemand.diagnostics().eagerlyMaterializedRecipes() == 0,
                "OnDemand eagerly materialized recipes");
        check(onDemand.diagnostics().cachedRecipes() == 0,
                "OnDemand cache was non-empty before lookup");
        ExtruderRelation onDemandRelation = relations.get(1);
        onDemand.lookup(ExtruderBenchmarkModel.request(onDemandRelation))
                .orElseThrow();
        onDemand.lookup(ExtruderBenchmarkModel.request(onDemandRelation))
                .orElseThrow();
        check(onDemand.diagnostics().cachePolicy()
                        == RecipeFamilyProvider.CachePolicy.NO_CACHE
                        && onDemand.diagnostics().cacheCeiling() == 0
                        && onDemand.diagnostics().cachedRecipes() == 0
                        && onDemand.diagnostics().cacheHits() == 0
                        && onDemand.diagnostics().cacheMisses() == 2
                        && onDemand.diagnostics().materializations() == 2,
                "OnDemand did not preserve its explicit zero-cache policy");

        RecipeFamilyProvider hybrid = new HybridExtruderProvider();
        hybrid.publish(relations, 1L, RuntimeSide.SERVER);
        long expectedEager = relations.stream()
                .filter(ExtruderRelation::eagerEligible)
                .count();
        check(hybrid.diagnostics().eagerlyMaterializedRecipes() == expectedEager,
                "Hybrid eager/long-tail boundary drifted");
        ExtruderRelation longTail = relations.stream()
                .filter(relation -> !relation.eagerEligible())
                .findFirst()
                .orElseThrow();
        hybrid.lookup(ExtruderBenchmarkModel.request(longTail)).orElseThrow();
        check(hybrid.diagnostics().cachedRecipes() == 1,
                "Hybrid long-tail lookup did not populate its epoch cache");
        check(hybrid.diagnostics().cachePolicy()
                        == RecipeFamilyProvider.CachePolicy
                                .BOUNDED_ACCESS_ORDER_LRU
                        && hybrid.diagnostics().cacheCeiling()
                                == HybridExtruderProvider.CACHE_CEILING,
                "Hybrid cache policy differs from production");

        List<ExtruderRelation> evictionRelations =
                ExtruderBenchmarkModel.syntheticRelations(
                        700, CANDIDATES_PER_INPUT, HOT_MODULO);
        RecipeFamilyProvider evictionHybrid = new HybridExtruderProvider();
        evictionHybrid.publish(
                evictionRelations, 1L, RuntimeSide.SERVER);
        for (ExtruderRelation relation : evictionRelations) {
            if (!relation.eagerEligible()) {
                evictionHybrid.lookup(ExtruderBenchmarkModel.request(relation))
                        .orElseThrow();
            }
        }
        check(evictionHybrid.diagnostics().cachedRecipes()
                        == HybridExtruderProvider.CACHE_CEILING,
                "Hybrid did not enforce the 512-row cache ceiling");
        check(evictionHybrid.diagnostics().evictions() > 0,
                "Hybrid cache ceiling did not perform real eviction");
        long misses = evictionHybrid.diagnostics().cacheMisses();
        ExtruderRelation evicted = evictionRelations.stream()
                .filter(relation -> !relation.eagerEligible())
                .findFirst()
                .orElseThrow();
        evictionHybrid.lookup(ExtruderBenchmarkModel.request(evicted))
                .orElseThrow();
        check(evictionHybrid.diagnostics().cacheMisses() == misses + 1,
                "Hybrid access-order cache retained its eldest evicted row");
    }

    private static List<String> enumeratedIds(RecipeEnumeration view) {
        List<String> result = new ArrayList<>(view.size());
        for (ExtruderRecipe recipe : view) {
            result.add(recipe.stableId());
        }
        return result;
    }

    private static List<RecipeFamilyProvider> providers() {
        return List.of(
                new ImmediateExtruderProvider(),
                new OnDemandExtruderProvider(),
                new HybridExtruderProvider());
    }

    static RecipeFamilyProvider newProvider(String candidateId) {
        return switch (candidateId) {
            case "immediate" -> new ImmediateExtruderProvider();
            case "on_demand" -> new OnDemandExtruderProvider();
            case "hybrid" -> new HybridExtruderProvider();
            default -> throw new IllegalArgumentException(
                    "Unknown T14b candidate " + candidateId);
        };
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectFailure(
            Runnable operation,
            Class<? extends Throwable> expected,
            String message) {
        try {
            operation.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                return;
            }
            throw new AssertionError(message, failure);
        }
        throw new AssertionError(message);
    }
}
