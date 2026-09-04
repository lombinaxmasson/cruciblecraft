package com.masson.cruciblecraft.benchmark.recipe;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Benchmark-only contract for comparing recipe-family materialization policies.
 *
 * <p>The contract intentionally has no Minecraft or production RecipeMap
 * dependency. T14c may use the measurements and semantics, but must make an
 * explicit production integration decision.
 */
public interface RecipeFamilyProvider {
    String candidateId();

    /**
     * Publishes a complete relation snapshot. Epochs are strictly increasing;
     * publishing invalidates caches and enumeration views from the prior epoch.
     */
    Publication publish(List<ExtruderRelation> relations, long epoch, RuntimeSide side);

    Optional<ExtruderRecipe> lookup(LookupRequest request);

    int indexedCandidateCount(LookupRequest request);

    RecipeEnumeration enumerationView();

    long epoch();

    ProviderDiagnostics diagnostics();

    long syncPayloadBytes();

    enum CachePolicy {
        FULLY_EAGER,
        NO_CACHE,
        BOUNDED_ACCESS_ORDER_LRU
    }

    enum RuntimeSide {
        SERVER,
        DEDICATED_CLIENT,
        INTEGRATED_CLIENT;

        public boolean requiresIndependentExpansion() {
            return this != INTEGRATED_CLIENT;
        }
    }

    record ExtruderRelation(
            int ordinal,
            String relationId,
            String inputKey,
            String outputKey,
            int durationTicks,
            int eut,
            boolean eagerEligible) {
        public ExtruderRelation {
            if (ordinal < 0 || durationTicks <= 0 || eut <= 0) {
                throw new IllegalArgumentException("Invalid Extruder relation numbers");
            }
            requireText(relationId, "relationId");
            requireText(inputKey, "inputKey");
            requireText(outputKey, "outputKey");
        }
    }

    record ExtruderRecipe(
            String stableId,
            String relationId,
            String inputKey,
            String outputKey,
            int durationTicks,
            int eut) {
        public ExtruderRecipe {
            requireText(stableId, "stableId");
            requireText(relationId, "relationId");
            requireText(inputKey, "inputKey");
            requireText(outputKey, "outputKey");
            if (durationTicks <= 0 || eut <= 0) {
                throw new IllegalArgumentException("Invalid Extruder recipe numbers");
            }
        }
    }

    record LookupRequest(String inputKey, String relationId) {
        public LookupRequest {
            requireText(inputKey, "inputKey");
            requireText(relationId, "relationId");
        }
    }

    record Publication(
            long epoch,
            RuntimeSide side,
            long expansionNanos,
            long indexNanos,
            long reloadNanos,
            int relationCount,
            int eagerlyMaterializedRecipes) {
        public Publication {
            Objects.requireNonNull(side, "side");
            if (epoch <= 0L
                    || expansionNanos < 0L
                    || indexNanos < 0L
                    || reloadNanos < 0L
                    || relationCount < 0
                    || eagerlyMaterializedRecipes < 0
                    || eagerlyMaterializedRecipes > relationCount) {
                throw new IllegalArgumentException("Invalid publication metrics");
            }
        }
    }

    record ProviderDiagnostics(
            long epoch,
            int relations,
            int indexedInputKeys,
            int eagerlyMaterializedRecipes,
            int cachedRecipes,
            int cacheCeiling,
            CachePolicy cachePolicy,
            long cacheHits,
            long cacheMisses,
            long materializations,
            long evictions,
            long cacheEpoch,
            RuntimeSide side) {
        public ProviderDiagnostics {
            Objects.requireNonNull(cachePolicy, "cachePolicy");
            Objects.requireNonNull(side, "side");
            if (relations < 0
                    || indexedInputKeys < 0
                    || eagerlyMaterializedRecipes < 0
                    || cachedRecipes < 0
                    || cacheCeiling < 0
                    || cachedRecipes > cacheCeiling
                    || cacheHits < 0L
                    || cacheMisses < 0L
                    || materializations < 0L
                    || evictions < 0L) {
                throw new IllegalArgumentException(
                        "Invalid provider cache diagnostics");
            }
        }
    }

    interface RecipeEnumeration extends Iterable<ExtruderRecipe> {
        int size();

        ExtruderRecipe get(int index);

        long epoch();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
