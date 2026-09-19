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
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/**
 * GT6 crucible/smeltery alloying index: first-level {@code mComponents} recipes
 * tagged {@code PROCESSING.CRUCIBLE_ALLOY}, plus {@code addAlloyingRecipe}
 * extras. Not a recursive flatten.
 */
public final class AlloyIndex {
    private static final String CRUCIBLE_ALLOY = "PROCESSING.CRUCIBLE_ALLOY";

    private final List<AlloyMatch> recipes;
    private final Map<Set<String>, List<AlloyMatch>> candidates;
    private final Map<String, List<AlloyMatch>> byComponent;
    private final Map<String, List<AlloyMatch>> byResult;

    public AlloyIndex(Collection<MaterialDefinition> definitions) {
        this(
                definitions,
                definition -> definition.forms().contains(MaterialPrefixes.INGOT),
                CrucibleAlloyingExtras.loadBundle());
    }

    public AlloyIndex(
            Collection<MaterialDefinition> definitions,
            Predicate<MaterialDefinition> hasRegisteredIngot) {
        this(definitions, hasRegisteredIngot, CrucibleAlloyingExtras.loadBundle());
    }

    public AlloyIndex(
            Collection<MaterialDefinition> definitions,
            Predicate<MaterialDefinition> hasRegisteredIngot,
            Collection<CrucibleAlloyingExtras.Recipe> extras) {
        this(
                definitions,
                hasRegisteredIngot,
                new CrucibleAlloyingExtras.Bundle(Map.of(), List.copyOf(extras)));
    }

    public AlloyIndex(
            Collection<MaterialDefinition> definitions,
            Predicate<MaterialDefinition> hasRegisteredIngot,
            CrucibleAlloyingExtras.Bundle extras) {
        Set<String> known = definitions.stream()
                .map(MaterialDefinition::id)
                .collect(Collectors.toUnmodifiableSet());
        Map<String, MaterialDefinition> byId = new LinkedHashMap<>();
        for (MaterialDefinition definition : definitions) {
            byId.put(definition.id(), definition);
        }
        Map<String, Integer> compositionOutputs = extras.compositionOutputs();
        List<AlloyMatch> built = new ArrayList<>();
        Set<String> recipeKeys = new HashSet<>();
        for (MaterialDefinition definition : definitions) {
            if (!definition.hasMaterialTag(CRUCIBLE_ALLOY)
                    || !hasRegisteredIngot.test(definition)
                    || definition.composition().size() < 2
                    || !knownComponents(definition.composition(), known)) {
                continue;
            }
            int output = compositionOutput(definition, compositionOutputs);
            if (output <= 0) {
                continue;
            }
            addRecipe(
                    built,
                    recipeKeys,
                    new AlloyMatch(
                            definition.id(),
                            definition.composition(),
                            output,
                            definition.id()));
        }
        for (CrucibleAlloyingExtras.Recipe extra : extras.recipes()) {
            MaterialDefinition result = byId.get(extra.result());
            if (result == null
                    || !hasRegisteredIngot.test(result)
                    || extra.inputs().size() < 2
                    || extra.output() <= 0
                    || !knownComponents(extra.inputs(), known)
                    || duplicatesComposition(result, extra, compositionOutputs)) {
                continue;
            }
            String key = extra.result() + "/" + componentKey(extra.inputs());
            addRecipe(
                    built,
                    recipeKeys,
                    new AlloyMatch(
                            extra.result(),
                            extra.inputs(),
                            extra.output(),
                            key));
        }
        recipes = List.copyOf(built);
        Map<Set<String>, List<AlloyMatch>> bySet = new HashMap<>();
        Map<String, List<AlloyMatch>> components = new HashMap<>();
        Map<String, List<AlloyMatch>> results = new HashMap<>();
        for (AlloyMatch recipe : recipes) {
            bySet.computeIfAbsent(recipe.costParts().keySet(), ignored -> new ArrayList<>())
                    .add(recipe);
            results.computeIfAbsent(recipe.resultId(), ignored -> new ArrayList<>()).add(recipe);
            for (String component : recipe.costParts().keySet()) {
                components.computeIfAbsent(component, ignored -> new ArrayList<>()).add(recipe);
            }
        }
        bySet.replaceAll((ignored, list) -> List.copyOf(list));
        components.replaceAll((ignored, list) -> List.copyOf(list));
        results.replaceAll((ignored, list) -> List.copyOf(list));
        candidates = Map.copyOf(bySet);
        byComponent = Map.copyOf(components);
        byResult = Map.copyOf(results);
    }

    private AlloyIndex() {
        recipes = List.of();
        candidates = Map.of();
        byComponent = Map.of();
        byResult = Map.of();
    }

    public static AlloyIndex empty() {
        return new AlloyIndex();
    }

    public List<AlloyMatch> recipes() {
        return recipes;
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
     * Every recipe of an alloy is considered once the alloy is first reached
     * from a molten component.
     */
    public Optional<Conversion> preferredCrucibleConversion(
            Map<String, Integer> contents, float temperature) {
        Conversion preferred = null;
        Set<String> scannedAlloys = new HashSet<>();
        for (var entry : contents.entrySet()) {
            if (entry.getValue() <= 0 || !molten(entry.getKey(), temperature)) {
                continue;
            }
            List<AlloyMatch> recipesForComponent = byComponent.get(entry.getKey());
            if (recipesForComponent == null) {
                continue;
            }
            for (AlloyMatch recipe : recipesForComponent) {
                if (!scannedAlloys.add(recipe.resultId())) {
                    continue;
                }
                List<AlloyMatch> recipesForResult = byResult.get(recipe.resultId());
                if (recipesForResult == null) {
                    continue;
                }
                for (AlloyMatch candidate : recipesForResult) {
                    Optional<Conversion> conversion =
                            candidate.crucibleConversion(contents, temperature);
                    if (conversion.isEmpty()) {
                        continue;
                    }
                    if (preferred == null
                            || conversion.get().outputUnits() > preferred.outputUnits()) {
                        preferred = conversion.get();
                    }
                }
            }
        }
        return Optional.ofNullable(preferred);
    }

    private static void addRecipe(
            List<AlloyMatch> built, Set<String> recipeKeys, AlloyMatch recipe) {
        String key = recipe.recipeKey();
        if (!recipeKeys.add(key)) {
            key = key + "/" + recipe.outputDivider();
            if (!recipeKeys.add(key)) {
                return;
            }
            recipe = new AlloyMatch(
                    recipe.resultId(),
                    recipe.costParts(),
                    recipe.outputDivider(),
                    key);
        }
        built.add(recipe);
    }

    private static boolean knownComponents(Map<String, Integer> parts, Set<String> known) {
        for (var entry : parts.entrySet()) {
            if (entry.getKey() == null
                    || entry.getKey().isBlank()
                    || entry.getValue() == null
                    || entry.getValue() <= 0
                    || !known.contains(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    private static boolean duplicatesComposition(
            MaterialDefinition result,
            CrucibleAlloyingExtras.Recipe extra,
            Map<String, Integer> compositionOutputs) {
        return result.composition().equals(extra.inputs())
                && extra.output() == compositionOutput(result, compositionOutputs);
    }

    private static int compositionOutput(
            MaterialDefinition definition, Map<String, Integer> compositionOutputs) {
        int sum = definition.composition().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        int explicit = compositionOutputs.getOrDefault(definition.id(), sum);
        return explicit > 0 ? explicit : sum;
    }

    private static String componentKey(Map<String, Integer> parts) {
        return new TreeSet<>(parts.keySet()).stream().collect(Collectors.joining("-"));
    }

    private static boolean molten(String materialId, float temperature) {
        return MaterialCatalog.contains(materialId)
                && temperature >= MaterialCatalog.require(materialId).thermal().meltingPoint();
    }

    public record AlloyMatch(
            String resultId,
            Map<String, Integer> costParts,
            int outputDivider,
            String recipeKey) {
        public AlloyMatch {
            if (resultId == null || resultId.isBlank()
                    || recipeKey == null || recipeKey.isBlank()
                    || outputDivider <= 0
                    || costParts == null
                    || costParts.size() < 2) {
                throw new IllegalArgumentException("Invalid alloy recipe");
            }
            costParts = Map.copyOf(new LinkedHashMap<>(costParts));
        }

        public Map<String, Integer> costPerIngot() {
            return perIngotCost().orElseGet(this::batchCost);
        }

        public Optional<Map<String, Integer>> perIngotCost() {
            int ingot = MaterialPrefixes.INGOT.units();
            LinkedHashMap<String, Integer> perIngot = new LinkedHashMap<>();
            for (var part : costParts.entrySet()) {
                long numerator = (long) part.getValue() * ingot;
                if (numerator % outputDivider != 0L) {
                    return Optional.empty();
                }
                int units = Math.toIntExact(numerator / outputDivider);
                if (units <= 0) {
                    return Optional.empty();
                }
                perIngot.put(part.getKey(), units);
            }
            return Optional.of(Map.copyOf(perIngot));
        }

        public Map<String, Integer> batchCost() {
            int ingot = MaterialPrefixes.INGOT.units();
            LinkedHashMap<String, Integer> cost = new LinkedHashMap<>();
            costParts.forEach((material, parts) ->
                    cost.put(material, Math.multiplyExact(parts, ingot)));
            return Map.copyOf(cost);
        }

        public MaterialDefinition result() {
            return MaterialCatalog.require(resultId);
        }

        public boolean matches(Map<String, Integer> contents) {
            long referenceAmount = -1;
            long referenceCost = -1;
            for (var cost : costParts.entrySet()) {
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
            int conversions = Integer.MAX_VALUE;
            for (var cost : costParts.entrySet()) {
                int have = contents.getOrDefault(cost.getKey(), 0);
                if (have <= 0 || cost.getValue() <= 0) {
                    return Optional.empty();
                }
                if (!molten(cost.getKey(), temperature)) {
                    nonMolten++;
                }
                conversions = Math.min(conversions, have / cost.getValue());
            }
            if (nonMolten > 1 || conversions <= 0) {
                return Optional.empty();
            }
            return Optional.of(new Conversion(this, conversions));
        }
    }

    public record Conversion(AlloyMatch recipe, int conversions) {
        public Conversion {
            if (recipe == null || conversions <= 0) {
                throw new IllegalArgumentException("Alloy conversion requires a recipe and conversions");
            }
        }

        public int batches() {
            return conversions;
        }

        public int outputUnits() {
            return Math.multiplyExact(conversions, recipe.outputDivider());
        }

        public Map<String, Integer> consumption() {
            Map<String, Integer> consumed = new LinkedHashMap<>();
            recipe.costParts().forEach((material, parts) ->
                    consumed.put(material, Math.multiplyExact(parts, conversions)));
            return Map.copyOf(consumed);
        }
    }
}
