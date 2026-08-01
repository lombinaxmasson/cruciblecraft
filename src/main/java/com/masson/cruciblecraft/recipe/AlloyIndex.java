package com.masson.cruciblecraft.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

public final class AlloyIndex {
    private final Map<Set<String>, List<AlloyMatch>> candidates;

    public AlloyIndex(Collection<MaterialDefinition> definitions) {
        this(
                definitions,
                definition -> MaterialCatalog.decompose(
                        definition,
                        MaterialPrefixes.INGOT.units()));
    }

    public AlloyIndex(
            Collection<MaterialDefinition> definitions,
            Function<MaterialDefinition, Map<String, Integer>> decomposer) {
        this(
                definitions,
                decomposer,
                definition -> definition.forms().contains(MaterialPrefixes.INGOT));
    }

    public AlloyIndex(
            Collection<MaterialDefinition> definitions,
            Function<MaterialDefinition, Map<String, Integer>> decomposer,
            Predicate<MaterialDefinition> hasRegisteredIngot) {
        Map<Set<String>, List<AlloyMatch>> built = new HashMap<>();
        for (MaterialDefinition definition : definitions) {
            if (definition.composition().isEmpty()
                    || definition.noDecompose()
                    || !hasRegisteredIngot.test(definition)) {
                continue;
            }
            Map<String, Integer> cost = decomposer.apply(definition);
            // Unary source compositions describe refining/identity provenance,
            // not an alloying recipe. Indexing them would make a pure base
            // material nondeterministically resolve to one of its derivatives.
            if (cost.size() < 2) {
                continue;
            }
            AlloyMatch match = new AlloyMatch(definition.id(), cost);
            built.computeIfAbsent(cost.keySet(), ignored -> new ArrayList<>()).add(match);
        }
        candidates = Map.copyOf(built);
    }

    private AlloyIndex() {
        candidates = Map.of();
    }

    public static AlloyIndex empty() {
        return new AlloyIndex();
    }

    public Optional<AlloyMatch> match(Map<String, Integer> contents) {
        List<AlloyMatch> possible = candidates.get(contents.keySet());
        if (possible == null) {
            return Optional.empty();
        }
        return possible.stream().filter(candidate -> candidate.matches(contents)).findFirst();
    }

    public record AlloyMatch(String resultId, Map<String, Integer> costPerIngot) {
        public AlloyMatch {
            costPerIngot = Map.copyOf(costPerIngot);
        }

        public MaterialDefinition result() {
            return MaterialCatalog.require(resultId);
        }

        public boolean matches(Map<String, Integer> contents) {
            long referenceAmount = -1;
            long referenceCost = -1;
            for (var cost : costPerIngot.entrySet()) {
                int amount = contents.getOrDefault(cost.getKey(), 0);
                if (amount <= 0) {
                    return false;
                }
                if (referenceAmount < 0) {
                    referenceAmount = amount;
                    referenceCost = cost.getValue();
                } else if ((long) amount * referenceCost
                        != referenceAmount * cost.getValue()) {
                    return false;
                }
            }
            return referenceAmount > 0;
        }
    }
}
