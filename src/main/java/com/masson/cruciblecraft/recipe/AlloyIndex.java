package com.masson.cruciblecraft.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final Map<String, List<AlloyMatch>> byComponent;

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
        Map<String, List<AlloyMatch>> indexed = new HashMap<>();
        for (List<AlloyMatch> recipes : candidates.values()) {
            for (AlloyMatch recipe : recipes) {
                for (String component : recipe.costPerIngot().keySet()) {
                    indexed.computeIfAbsent(component, ignored -> new ArrayList<>()).add(recipe);
                }
            }
        }
        indexed.replaceAll((ignored, recipes) -> List.copyOf(recipes));
        byComponent = Map.copyOf(indexed);
    }

    private AlloyIndex() {
        candidates = Map.of();
        byComponent = Map.of();
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

    /**
     * GT6 smeltery/crucible alloy tick: convert as many complete recipes as
     * will fit, allowing leftovers and at most one still-solid ingredient.
     */
    public Optional<Conversion> preferredCrucibleConversion(
            Map<String, Integer> contents, float temperature) {
        Conversion preferred = null;
        Set<String> checked = new HashSet<>();
        for (var entry : contents.entrySet()) {
            if (entry.getValue() <= 0 || !molten(entry.getKey(), temperature)) {
                continue;
            }
            List<AlloyMatch> recipes = byComponent.get(entry.getKey());
            if (recipes == null) {
                continue;
            }
            for (AlloyMatch recipe : recipes) {
                if (!checked.add(recipe.resultId())) {
                    continue;
                }
                Optional<Conversion> candidate = recipe.crucibleConversion(contents, temperature);
                if (candidate.isEmpty()) {
                    continue;
                }
                if (preferred == null || candidate.get().outputUnits() > preferred.outputUnits()) {
                    preferred = candidate.get();
                }
            }
        }
        return Optional.ofNullable(preferred);
    }

    private static boolean molten(String materialId, float temperature) {
        return MaterialCatalog.contains(materialId)
                && temperature >= MaterialCatalog.require(materialId).thermal().meltingPoint();
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

        Optional<Conversion> crucibleConversion(
                Map<String, Integer> contents, float temperature) {
            if (temperature < result().thermal().meltingPoint()) {
                return Optional.empty();
            }
            int nonMolten = 0;
            int batches = Integer.MAX_VALUE;
            for (var cost : costPerIngot.entrySet()) {
                int have = contents.getOrDefault(cost.getKey(), 0);
                if (have <= 0 || cost.getValue() <= 0) {
                    return Optional.empty();
                }
                if (!molten(cost.getKey(), temperature)) {
                    nonMolten++;
                }
                batches = Math.min(batches, have / cost.getValue());
            }
            if (nonMolten > 1 || batches <= 0) {
                return Optional.empty();
            }
            return Optional.of(new Conversion(this, batches));
        }
    }

    public record Conversion(AlloyMatch recipe, int batches) {
        public Conversion {
            if (recipe == null || batches <= 0) {
                throw new IllegalArgumentException("Alloy conversion requires a recipe and batches");
            }
        }

        public int outputUnits() {
            return Math.multiplyExact(batches, MaterialPrefixes.INGOT.units());
        }

        public Map<String, Integer> consumption() {
            Map<String, Integer> consumed = new LinkedHashMap<>();
            recipe.costPerIngot().forEach((material, units) ->
                    consumed.put(material, Math.multiplyExact(units, batches)));
            return Map.copyOf(consumed);
        }
    }
}
