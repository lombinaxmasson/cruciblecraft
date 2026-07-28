package com.masson.cruciblecraft.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

public final class AlloyIndex {
    private final Map<Set<String>, List<AlloyMatch>> candidates;

    public AlloyIndex(Collection<MaterialDefinition> definitions) {
        Map<Set<String>, List<AlloyMatch>> built = new HashMap<>();
        for (MaterialDefinition definition : definitions) {
            if (definition.composition().isEmpty()
                    || definition.noDecompose()
                    || !definition.forms().contains(MaterialForm.INGOT)) {
                continue;
            }
            Map<String, Integer> cost =
                    MaterialCatalog.decompose(definition, MaterialForm.INGOT.units());
            AlloyMatch match = new AlloyMatch(definition, cost);
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

    public record AlloyMatch(MaterialDefinition result, Map<String, Integer> costPerIngot) {
        public AlloyMatch {
            costPerIngot = Map.copyOf(costPerIngot);
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
