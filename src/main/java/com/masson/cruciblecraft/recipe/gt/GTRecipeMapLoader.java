package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.rule.LegacyMaterialRuleAdapter;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRecipe;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleExpansion;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRuntimeMetadata;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.registry.ModRecipes;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/** Builds immutable concrete RecipeMap and material metadata snapshots. */
public final class GTRecipeMapLoader {
    private static final int LOOKUP_BENCHMARK_SAMPLES = 256;
    private static final int LOOKUP_BENCHMARK_ROUNDS = 4;
    private static volatile int rejectedUnindexedRecipeCount;
    private static volatile PublicationMetrics lastPublicationMetrics =
            PublicationMetrics.empty();

    private GTRecipeMapLoader() {}

    public static int rejectedUnindexedRecipeCount() {
        return rejectedUnindexedRecipeCount;
    }

    public static PublicationMetrics lastPublicationMetrics() {
        return lastPublicationMetrics;
    }

    public static synchronized void reload(RecipeManager manager) {
        long started = System.nanoTime();
        Map<ResourceLocation, RecipeMap> knownMaps = new HashMap<>();
        Map<RecipeMap, List<ResolvedRecipe>> resolved = new HashMap<>();
        for (RecipeMap map : ModRecipeMaps.ALL) {
            knownMaps.put(map.id(), map);
            resolved.put(map, new ArrayList<>());
        }

        for (RecipeHolder<GTRecipeEntry> holder
                : manager.getAllRecipesFor(ModRecipes.GT_RECIPE_TYPE.get())) {
            GTRecipeEntry entry = holder.value();
            RecipeMap map = knownMaps.get(entry.map());
            if (map == null) {
                throw recipeValidationError(
                        holder.id(),
                        "Unknown recipe map " + entry.map());
            }
            validateTarget(holder.id(), map, entry.recipe());
            resolved.get(map).add(new ResolvedRecipe(holder.id(), entry.recipe(), false));
        }

        List<RecipeHolder<MaterialRuleRecipe>> declarative = manager
                .getAllRecipesFor(ModRecipes.MATERIAL_RULE_TYPE.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
        MaterialRuleRuntimeMetadata.Publication metadata = buildRuntimeMetadata(declarative);
        if (!metadata.diagnostics().isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid material-rule metadata: " + String.join("; ", metadata.diagnostics()));
        }
        MaterialCatalog.RuntimePreview materialPreview = MaterialCatalog.previewRuntime(
                metadata.tunings(), metadata.preferences());
        var effectiveMaterials = materialPreview.definitions().values();
        MaterialRuleExpansion.FormIndexes formIndexes =
                MaterialRuleExpansion.FormIndexes.withRegisteredForms(
                        effectiveMaterials,
                        MaterialRegistrationGate.load(effectiveMaterials));

        List<RuleSource> rules = new ArrayList<>();
        declarative.stream()
                .filter(holder -> holder.value().rule().target().isPresent())
                .forEach(holder -> rules.add(
                        new RuleSource(holder.id(), holder.value().rule())));
        manager.getAllRecipesFor(ModRecipes.CRUSHER_TYPE.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .filter(holder ->
                        !LegacyMaterialRuleAdapter.replacedByConcreteOreChain(
                                holder.value()))
                .forEach(holder -> rules.add(new RuleSource(
                        holder.id(),
                        LegacyMaterialRuleAdapter.fromCrusher(holder.value()))));
        manager.getAllRecipesFor(ModRecipes.ANVIL_TYPE.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .forEach(holder -> rules.add(new RuleSource(
                        holder.id(),
                        LegacyMaterialRuleAdapter.fromAnvil(holder.value()))));
        rules.stream()
                .map(RuleSource::id)
                .map(GTRecipeMapLoader::authoredMaterialRuleStage)
                .filter(OptionalInt::isPresent)
                .mapToInt(OptionalInt::getAsInt)
                .distinct()
                .forEach(stage -> validateAuthoredMaterialRuleBudget(stage, 0));

        rules.stream()
                .sorted(Comparator.comparing(source -> source.id().toString()))
                .forEach(source -> expand(
                        source,
                        knownMaps,
                        resolved,
                        effectiveMaterials,
                        materialPreview.unificationPreferences(),
                        formIndexes));

        LinkedHashMap<RecipeMap, List<RecipeMap.Entry>> candidates = new LinkedHashMap<>();
        int rejectedUnindexed = 0;
        for (RecipeMap map : ModRecipeMaps.ALL) {
            List<RecipeMap.Entry> ordered = resolved.get(map).stream()
                    .sorted((left, right) -> RecipeExpansionRules.comparePriority(
                            left.materialSpecific(),
                            left.id().toString(),
                            right.materialSpecific(),
                            right.id().toString()))
                    .map(recipe -> new RecipeMap.Entry(recipe.id(), recipe.recipe()))
                    .toList();
            List<RecipeMap.Entry> accepted = new ArrayList<>();
            for (RecipeMap.Entry entry : ordered) {
                List<String> unsupported =
                        ComponentIngredientIndex.unsupportedIngredientTypes(
                                entry.recipe());
                if (unsupported.isEmpty()) {
                    accepted.add(entry);
                    continue;
                }
                rejectedUnindexed++;
                CrucibleCraft.LOGGER.error(
                        "Rejected unindexable recipe {} from map {}: ingredient types {}",
                        entry.id(),
                        map.id(),
                        unsupported);
            }
            validateT5RecipeProvenance(map, accepted);
            validateNoShadows(map, accepted);
            candidates.put(map, List.copyOf(accepted));
        }
        validateRequiredMaps(candidates);
        int t3Recipes = 0;
        int t4ToolRecipes = 0;
        int t5RecipesOnT3Maps = 0;
        for (var spec : ModProcessingMachines.T3_MACHINES) {
            List<RecipeMap.Entry> entries = candidates.get(spec.requireRecipeMap());
            t3Recipes += entries.size();
            t4ToolRecipes += (int) entries.stream()
                    .filter(entry -> isT4ToolRecipe(entry.id()))
                    .count();
            t5RecipesOnT3Maps += (int) entries.stream()
                    .filter(entry -> isT5ChemicalRecipe(entry.id()))
                    .count();
        }
        int t3ComponentRecipes =
                t3Recipes - t4ToolRecipes - t5RecipesOnT3Maps;
        int t5ChemicalRecipes = candidates.values().stream()
                .flatMap(List::stream)
                .mapToInt(entry -> isT5ChemicalRecipe(entry.id()) ? 1 : 0)
                .sum();
        Map<Integer, Integer> authoredMaterialRules = new HashMap<>();
        candidates.values().stream()
                .flatMap(List::stream)
                .forEach(entry -> authoredMaterialRuleStage(entry.id()).ifPresent(
                        stage -> authoredMaterialRules.merge(stage, 1, Integer::sum)));
        int t7AuthoredMaterialRules = authoredMaterialRules.getOrDefault(7, 0);
        int t8PipeMaterialRules = authoredMaterialRules.getOrDefault(8, 0);
        int t10KnownFormMaterialRules = authoredMaterialRules.getOrDefault(10, 0);
        int allPublishedRecipes = candidates.values().stream()
                .mapToInt(List::size)
                .sum();
        validateAuthoredMaterialRuleBudgets(authoredMaterialRules);
        validatePublicationBudgets(
                t3ComponentRecipes,
                t4ToolRecipes,
                t3Recipes,
                t5ChemicalRecipes,
                t5RecipesOnT3Maps,
                allPublishedRecipes);

        long indexStarted = System.nanoTime();
        List<RecipeMap.Prepared> prepared = candidates.entrySet().stream()
                .map(entry -> entry.getKey().prepareRecipes(entry.getValue()))
                .toList();
        validateNoUnindexed(ModRecipeMaps.ALL, prepared);
        long indexNanos = System.nanoTime() - indexStarted;
        long epoch = GTRecipeRuntimeEpoch.publish(materialPreview, prepared);
        long reloadNanos = System.nanoTime() - started;
        lastPublicationMetrics = new PublicationMetrics(
                t3ComponentRecipes,
                t4ToolRecipes,
                t3Recipes,
                t5ChemicalRecipes,
                t7AuthoredMaterialRules,
                t8PipeMaterialRules,
                t10KnownFormMaterialRules,
                allPublishedRecipes,
                reloadNanos / 1_000_000L,
                indexNanos / 1_000_000L);
        rejectedUnindexedRecipeCount = rejectedUnindexed;
        for (int index = 0; index < ModRecipeMaps.ALL.size(); index++) {
            RecipeMap map = ModRecipeMaps.ALL.get(index);
            RecipeMap.Prepared snapshot = prepared.get(index);
            CrucibleCraft.LOGGER.info(
                    "RecipeMap {} - {} recipes, {} unindexed",
                    map.id(), snapshot.entries().size(), snapshot.unindexedRecipeCount());
        }
        CrucibleCraft.LOGGER.info(
                "Published recipe epoch {} with {} T3 component, {} T4 tool, {} "
                        + "T5 chemical, {} T7 authored, {} T8 pipe, and {} T10 known-form "
                        + "material-rule recipes "
                        + "({} live T3-map, {} total) across {} maps in {} ms; "
                        + "indexes {} ms "
                        + "(count budgets {}/{}/{}/{}/{}/{}/{}/{})",
                epoch,
                t3ComponentRecipes,
                t4ToolRecipes,
                t5ChemicalRecipes,
                t7AuthoredMaterialRules,
                t8PipeMaterialRules,
                t10KnownFormMaterialRules,
                t3Recipes,
                allPublishedRecipes,
                ModRecipeMaps.ALL.size(),
                lastPublicationMetrics.reloadMillis(),
                lastPublicationMetrics.indexMillis(),
                ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET,
                ModProcessingMachines.T4_TOOL_EXPANSION_BUDGET,
                ModProcessingMachines.LIVE_T3_MAP_RECIPE_BUDGET,
                ModProcessingMachines.T5_CHEMICAL_RECIPE_BUDGET,
                ModProcessingMachines.T7_AUTHORED_MATERIAL_RULE_BUDGET,
                ModProcessingMachines.T8_PIPE_MATERIAL_RULE_BUDGET,
                ModProcessingMachines.T10_AUTHORED_MATERIAL_RULE_BUDGET,
                ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET);
    }

    /**
     * Runs the deliberately expensive T4 lookup benchmark for verification.
     * Production reloads must not pay this cost.
     */
    public static T4LookupMetrics benchmarkT4LookupsForVerification() {
        List<RecipeMap.Entry> entries = ModRecipeMaps.ASSEMBLER.entries().stream()
                .filter(entry -> isT4ToolRecipe(entry.id()))
                .toList();
        int sampleCount = Math.min(LOOKUP_BENCHMARK_SAMPLES, entries.size());
        List<GTRecipeQuery> queries = new ArrayList<>(sampleCount);
        for (int index = 0; index < sampleCount; index++) {
            int entryIndex = index * entries.size() / sampleCount;
            queries.add(queryFor(entries.get(entryIndex).recipe()));
        }
        for (GTRecipeQuery query : queries) {
            if (ModRecipeMaps.ASSEMBLER.findMatch(query).isEmpty()) {
                throw new IllegalStateException(
                        "Published T4 recipe failed lookup benchmark warm-up");
            }
        }
        long started = System.nanoTime();
        int samples = 0;
        long candidates = 0L;
        for (int round = 0; round < LOOKUP_BENCHMARK_ROUNDS; round++) {
            for (GTRecipeQuery query : queries) {
                candidates += ModRecipeMaps.ASSEMBLER
                        .indexedCandidateCount(query);
                if (ModRecipeMaps.ASSEMBLER.findMatch(query).isEmpty()) {
                    throw new IllegalStateException(
                            "Published T4 recipe failed lookup benchmark");
                }
                samples++;
            }
        }
        long elapsed = System.nanoTime() - started;
        return new T4LookupMetrics(
                samples,
                samples == 0 ? 0L : elapsed / samples,
                samples == 0 ? 0L : candidates / samples);
    }

    private static GTRecipeQuery queryFor(GTRecipe recipe) {
        List<ItemStack> items = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemStack[] candidates = recipe.itemInputs().get(index).getItems();
            if (candidates.length == 0) {
                throw new IllegalStateException(
                        "Indexed recipe has no concrete item candidates");
            }
            ItemStack sample = candidates[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(index)));
            items.add(sample);
        }
        return new GTRecipeQuery(items, recipe.fluidInputs());
    }

    public record PublicationMetrics(
            int t3ComponentRecipes,
            int t4ToolRecipes,
            int liveT3MapRecipes,
            int t5ChemicalRecipes,
            int t7AuthoredMaterialRules,
            int t8PipeMaterialRules,
            int t10KnownFormMaterialRules,
            int allPublishedRecipes,
            long reloadMillis,
            long indexMillis) {
        private static PublicationMetrics empty() {
            return new PublicationMetrics(
                    0, 0, 0, 0, 0, 0, 0, 0, 0L, 0L);
        }
    }

    public record T4LookupMetrics(
            int samples,
            long averageNanos,
            long averageCandidates) {}

    static void validateExpansionBudgets(
            int t3ComponentRecipes,
            int t4ToolRecipes,
            int liveT3MapRecipes) {
        validatePublicationBudgets(
                t3ComponentRecipes,
                t4ToolRecipes,
                liveT3MapRecipes,
                0,
                liveT3MapRecipes);
    }

    static void validatePublicationBudgets(
            int t3ComponentRecipes,
            int t4ToolRecipes,
            int liveT3MapRecipes,
            int t5ChemicalRecipes,
            int allPublishedRecipes) {
        validatePublicationBudgets(
                t3ComponentRecipes,
                t4ToolRecipes,
                liveT3MapRecipes,
                t5ChemicalRecipes,
                liveT3MapRecipes - t3ComponentRecipes - t4ToolRecipes,
                allPublishedRecipes);
    }

    static void validatePublicationBudgets(
            int t3ComponentRecipes,
            int t4ToolRecipes,
            int liveT3MapRecipes,
            int t5ChemicalRecipes,
            int t5RecipesOnT3Maps,
            int allPublishedRecipes) {
        if (t3ComponentRecipes < 0
                || t4ToolRecipes < 0
                || t5ChemicalRecipes < 0
                || t5RecipesOnT3Maps < 0
                || t5RecipesOnT3Maps > t5ChemicalRecipes
                || allPublishedRecipes < 0
                || liveT3MapRecipes
                        != t3ComponentRecipes + t4ToolRecipes + t5RecipesOnT3Maps
                || allPublishedRecipes
                        < (long) liveT3MapRecipes
                                + t5ChemicalRecipes - t5RecipesOnT3Maps) {
            throw new IllegalArgumentException(
                    "Recipe budget counts are inconsistent: component="
                            + t3ComponentRecipes + ", tools=" + t4ToolRecipes
                            + ", live=" + liveT3MapRecipes
                            + ", t5=" + t5ChemicalRecipes
                            + ", t5OnT3=" + t5RecipesOnT3Maps
                            + ", all=" + allPublishedRecipes);
        }
        if (t3ComponentRecipes
                > ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET) {
            throw new IllegalStateException(
                    "T3 component expansion " + t3ComponentRecipes
                            + " exceeds budget "
                            + ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET);
        }
        if (t4ToolRecipes > ModProcessingMachines.T4_TOOL_EXPANSION_BUDGET) {
            throw new IllegalStateException(
                    "T4 tool expansion " + t4ToolRecipes + " exceeds budget "
                            + ModProcessingMachines.T4_TOOL_EXPANSION_BUDGET);
        }
        if (liveT3MapRecipes
                > ModProcessingMachines.LIVE_T3_MAP_RECIPE_BUDGET) {
            throw new IllegalStateException(
                    "Live T3-map recipe count " + liveT3MapRecipes + " exceeds budget "
                            + ModProcessingMachines.LIVE_T3_MAP_RECIPE_BUDGET);
        }
        if (t5ChemicalRecipes > ModProcessingMachines.T5_CHEMICAL_RECIPE_BUDGET) {
            throw new IllegalStateException(
                    "T5 chemical recipe count " + t5ChemicalRecipes + " exceeds budget "
                            + ModProcessingMachines.T5_CHEMICAL_RECIPE_BUDGET);
        }
        if (allPublishedRecipes > ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET) {
            throw new IllegalStateException(
                    "All published recipe count " + allPublishedRecipes + " exceeds budget "
                            + ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET);
        }
    }

    static boolean isT4ToolRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("t4/assembler/");
    }

    static boolean isT5ChemicalRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("t5/");
    }

    static OptionalInt authoredMaterialRuleStage(ResourceLocation id) {
        if (!CrucibleCraft.MODID.equals(id.getNamespace())) {
            return OptionalInt.empty();
        }
        String path = id.getPath();
        int slash = path.indexOf('/');
        if (slash < 2 || path.charAt(0) != 't') {
            return OptionalInt.empty();
        }
        try {
            int stage = Integer.parseInt(path.substring(1, slash));
            return stage >= 7 ? OptionalInt.of(stage) : OptionalInt.empty();
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    static void validateAuthoredMaterialRuleBudgets(Map<Integer, Integer> recipeCounts) {
        recipeCounts.forEach(GTRecipeMapLoader::validateAuthoredMaterialRuleBudget);
    }

    static void validateAuthoredMaterialRuleBudget(int stage, int recipeCount) {
        if (recipeCount < 0) {
            throw new IllegalArgumentException(
                    "T" + stage + " authored material-rule count cannot be negative");
        }
        Integer budget =
                ModProcessingMachines.AUTHORED_MATERIAL_RULE_BUDGETS.get(stage);
        if (budget == null) {
            throw new IllegalArgumentException(
                    "No authored material-rule budget for T" + stage);
        }
        if (recipeCount > budget) {
            throw new IllegalStateException(
                    "T" + stage + " authored material-rule count " + recipeCount
                            + " exceeds budget " + budget);
        }
    }

    private static void expand(
            RuleSource source,
            Map<ResourceLocation, RecipeMap> knownMaps,
            Map<RecipeMap, List<ResolvedRecipe>> output,
            java.util.Collection<com.masson.cruciblecraft.material.def.MaterialDefinition>
                    effectiveMaterials,
            Map<String, String> candidatePreferences,
            MaterialRuleExpansion.FormIndexes formIndexes) {
        ResourceLocation target = source.rule().target().orElseThrow();
        RecipeMap map = knownMaps.get(target);
        if (map == null) {
            throw recipeValidationError(
                    source.id(),
                    "Unknown recipe map " + target);
        }
        for (MaterialRuleExpansion.Expanded expanded
                : MaterialRuleExpansion.expand(
                        source.id(),
                        source.rule(),
                        effectiveMaterials,
                        candidatePreferences,
                        formIndexes)) {
            validateTarget(expanded.id(), map, expanded.recipe());
            output.get(map).add(new ResolvedRecipe(
                    expanded.id(),
                    expanded.recipe(),
                    expanded.materialSpecific()));
        }
    }

    static void validateTarget(
            ResourceLocation recipeId,
            RecipeMap map,
            GTRecipe recipe) {
        boolean anvil = map == ModRecipeMaps.ANVIL
                || map == ModRecipeMaps.ANVIL_BEND_SMALL
                || map == ModRecipeMaps.ANVIL_BEND_BIG;
        if (anvil && !AnvilRecipeExecutionRules.supportsOutputs(
                recipe.itemOutputs().size(),
                recipe.outputChances().isEmpty()
                        ? -1
                        : recipe.outputChances().getFirst())) {
            throw recipeValidationError(
                    recipeId,
                    "Unsupported anvil output shape for map " + map.id());
        }
        var machine = ModProcessingMachines.forRecipeMap(map.id());
        if (machine.isPresent()) {
            var invalid = machine.get().validator().validate(recipe);
            if (invalid.isPresent()) {
                throw recipeValidationError(
                        recipeId,
                        "Machine " + machine.get().id() + " rejected recipe for map "
                                + map.id() + " (" + invalid.get() + ")");
            }
        }
    }

    static void validateRequiredMaps(
            Map<RecipeMap, List<RecipeMap.Entry>> candidates) {
        java.util.LinkedHashSet<RecipeMap> required = new java.util.LinkedHashSet<>();
        required.add(ModRecipeMaps.COKE_OVEN);
        required.add(ModRecipeMaps.CRUSHER);
        required.add(ModRecipeMaps.ANVIL);
        ModProcessingMachines.CONFIGURED_MACHINES.stream()
                .map(ProcessingMachineSpec::requireRecipeMap)
                .forEach(required::add);
        for (RecipeMap map : required) {
            List<RecipeMap.Entry> entries = candidates.get(map);
            if (entries == null) {
                throw new IllegalArgumentException("Missing required map candidate " + map.id());
            }
            boolean provisionedT5Map = ModProcessingMachines.T5_DEDICATED_MACHINES.stream()
                    .map(ProcessingMachineSpec::requireRecipeMap)
                    .anyMatch(candidate -> candidate == map);
            if (entries.isEmpty() && !provisionedT5Map) {
                throw new IllegalArgumentException(
                        "Required playable map " + map.id() + " loaded zero recipes");
            }
        }
    }

    /**
     * Recipe ids are the mandatory stage provenance. This remains explicit
     * because bath and centrifuge host both pre-T5 and T5 recipe populations.
     */
    static void validateT5RecipeProvenance(
            RecipeMap map, List<RecipeMap.Entry> entries) {
        boolean t5Map = ModProcessingMachines.T5_MACHINES.stream()
                .map(ProcessingMachineSpec::requireRecipeMap)
                .anyMatch(candidate -> candidate == map);
        boolean dedicatedT5Map = ModProcessingMachines.T5_DEDICATED_MACHINES.stream()
                .map(ProcessingMachineSpec::requireRecipeMap)
                .anyMatch(candidate -> candidate == map);
        for (RecipeMap.Entry entry : entries) {
            boolean t5Recipe = isT5ChemicalRecipe(entry.id());
            if (t5Recipe && !t5Map) {
                throw new IllegalArgumentException(
                        "T5 recipe " + entry.id() + " targets non-T5 map " + map.id());
            }
            if (dedicatedT5Map && !t5Recipe) {
                throw new IllegalArgumentException(
                        "Dedicated T5 map " + map.id() + " requires recipe-id prefix t5/: "
                                + entry.id());
            }
        }
    }

    static void validateNoUnindexed(
            List<RecipeMap> maps,
            List<RecipeMap.Prepared> prepared) {
        if (maps.size() != prepared.size()) {
            throw new IllegalArgumentException(
                    "Recipe map and prepared snapshot counts differ");
        }
        for (int index = 0; index < maps.size(); index++) {
            int unindexed = prepared.get(index).unindexedRecipeCount();
            if (unindexed != 0) {
                throw new IllegalArgumentException(
                        "RecipeMap " + maps.get(index).id() + " contains "
                                + unindexed + " unindexed recipes; add an explicit index "
                                + "before publishing non-simple ingredients");
            }
        }
    }

    static void validateNoShadows(RecipeMap map, List<RecipeMap.Entry> entries) {
        Map<String, ResourceLocation> signatures = new HashMap<>();
        for (RecipeMap.Entry entry : entries) {
            String signature = inputSignature(entry.recipe());
            ResourceLocation previous = signatures.putIfAbsent(signature, entry.id());
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Shadowed input signature in map " + map.id() + ": "
                                + describeLogicalResource(previous) + " conflicts with "
                                + describeLogicalResource(entry.id())
                                + "; signature=" + signature
                                + ". Change the recipe input or specificity to remove "
                                + "the conflict.");
            }
        }
    }

    static String logicalRecipeResourcePath(ResourceLocation recipeId) {
        return "data/" + recipeId.getNamespace() + "/recipe/"
                + recipeId.getPath() + ".json";
    }

    private static IllegalArgumentException recipeValidationError(
            ResourceLocation recipeId,
            String message) {
        return new IllegalArgumentException(
                message + "; " + describeLogicalResource(recipeId));
    }

    private static String describeLogicalResource(ResourceLocation recipeId) {
        return "recipe " + recipeId + " (logical resource path "
                + logicalRecipeResourcePath(recipeId) + ")";
    }

    private static String inputSignature(GTRecipe recipe) {
        List<String> items = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            var ingredient = recipe.itemInputs().get(index);
            String alternatives = java.util.Arrays.stream(ingredient.getItems())
                    .filter(stack -> !stack.isEmpty())
                    .map(GTRecipeMapLoader::stackIdentity)
                    .sorted()
                    .collect(java.util.stream.Collectors.joining("|"));
            items.add(recipe.itemInputCounts().get(index)
                    + ":" + recipe.itemInputActions().get(index)
                    + "@" + ingredient.getClass().getName()
                    + "@" + alternatives
                    + (ingredient.isCustom()
                            ? "@" + ingredient.getCustomIngredient()
                            : "@" + java.util.Arrays.toString(
                                    ingredient.getValues())));
        }
        items.sort(String::compareTo);
        List<String> fluids = recipe.fluidInputs().stream()
                .map(stack -> stack.getAmount()
                        + "@" + BuiltInRegistries.FLUID.getKey(stack.getFluid())
                        + "@" + stack.getComponentsPatch())
                .sorted()
                .toList();
        return String.join(",", items) + "||" + String.join(",", fluids);
    }

    private static String stackIdentity(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "@" + stack.getComponentsPatch();
    }

    private static MaterialRuleRuntimeMetadata.Publication buildRuntimeMetadata(
            List<RecipeHolder<MaterialRuleRecipe>> holders) {
        return MaterialRuleRuntimeMetadata.build(
                        holders.stream()
                                .map(holder -> new MaterialRuleRuntimeMetadata.Source(
                                        holder.id(), holder.value().rule()))
                                .toList(),
                        MaterialCatalog.startupValues(),
                        MaterialLookup::isValidPreference);
    }

    private record RuleSource(ResourceLocation id, MaterialRule rule) {}

    private record ResolvedRecipe(
            ResourceLocation id,
            GTRecipe recipe,
            boolean materialSpecific) {}
}
