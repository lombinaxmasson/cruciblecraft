package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRecipe;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleExpansion;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRuntimeMetadata;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.registry.ModRecipes;
import com.masson.cruciblecraft.registry.ModFuelGenerators;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/** Builds immutable concrete RecipeMap and material metadata snapshots. */
public final class GTRecipeMapLoader {
    private static final int LOOKUP_BENCHMARK_SAMPLES = 256;
    private static final int LOOKUP_BENCHMARK_TIMING_SAMPLES = 61;
    private static final int LOOKUP_BENCHMARK_OPERATIONS_PER_SAMPLE = 32;
    private static volatile PublicationMetrics lastPublicationMetrics =
            PublicationMetrics.empty();

    private GTRecipeMapLoader() {}

    public static PublicationMetrics lastPublicationMetrics() {
        return lastPublicationMetrics;
    }

    public static synchronized void reload(RecipeManager manager) {
        reload(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER);
    }

    public static synchronized void reload(
            RecipeManager manager,
            ExtruderRecipeFamilyProvider.RuntimeSide runtimeSide) {
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

        List<CompactRecipeFamilySource> compactSources = new ArrayList<>();
        for (RecipeHolder<CompactGTRecipeFamilyEntry> holder
                : manager.getAllRecipesFor(ModRecipes.COMPACT_GT_RECIPE_FAMILY_TYPE.get())) {
            CompactGTRecipeFamilyDefinition definition = holder.value().definition();
            if (!knownMaps.containsKey(definition.targetMap())) {
                throw recipeValidationError(
                        holder.id(),
                        "Unknown recipe map " + definition.targetMap());
            }
            compactSources.add(new CompactRecipeFamilySource(holder.id(), definition));
        }
        compactSources.sort(Comparator.comparing(source -> source.id().toString()));

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
        rules.stream()
                .map(RuleSource::id)
                .map(GTRecipeMapLoader::authoredMaterialRuleStage)
                .filter(OptionalInt::isPresent)
                .mapToInt(OptionalInt::getAsInt)
                .distinct()
                .forEach(stage -> validateAuthoredMaterialRuleBudget(stage, 0));

        List<ExtruderRecipeFamilyProvider.Source> extruderFamilySources =
                new ArrayList<>();
        rules.stream()
                .sorted(Comparator.comparing(source -> source.id().toString()))
                .forEach(source -> {
                    if (isT14CompactExtruder(source)) {
                        extruderFamilySources.add(
                                new ExtruderRecipeFamilyProvider.Source(
                                        source.id(), source.rule()));
                    } else {
                        expand(
                                source,
                                knownMaps,
                                resolved,
                                effectiveMaterials,
                                materialPreview.unificationPreferences(),
                                formIndexes);
                    }
                });
        long preparedEpoch = GTRecipeRuntimeEpoch.nextEpoch();
        ExtruderRecipeFamilyProvider.Snapshot extruderFamily =
                ExtruderRecipeFamilyProvider.prepare(
                        extruderFamilySources,
                        effectiveMaterials,
                        materialPreview.unificationPreferences(),
                        formIndexes,
                        preparedEpoch,
                        runtimeSide);
        Map<ResourceLocation, CompactRecipeFamilyProvider.Snapshot> compactByMap =
                CompactRecipeFamilyProvider.prepareByTarget(
                        compactSources,
                        knownMaps,
                        preparedEpoch,
                        compactSide(runtimeSide),
                        CompactRecipeFamilyProvider.t37ProductionPolicy(
                                compactSources));
        LinkedHashMap<RecipeMap, List<RecipeMap.Entry>> candidates = new LinkedHashMap<>();
        Map<RecipeMap, List<RecipeMap.RecipeFamily>> families = new HashMap<>();
        for (RecipeMap map : ModRecipeMaps.ALL) {
            List<RecipeMap.RecipeFamily> mapFamilies = new ArrayList<>();
            if (map == ModRecipeMaps.EXTRUDER) {
                mapFamilies.add(extruderFamily);
            }
            CompactRecipeFamilyProvider.Snapshot compact = compactByMap.get(map.id());
            if (compact != null) {
                mapFamilies.add(compact);
            }
            families.put(map, List.copyOf(mapFamilies));
        }
        for (RecipeMap map : ModRecipeMaps.ALL) {
            List<RecipeMap.Entry> ordered = resolved.get(map).stream()
                    .sorted((left, right) -> RecipeExpansionRules.comparePriority(
                            left.materialSpecific(),
                            left.id().toString(),
                            right.materialSpecific(),
                            right.id().toString()))
                    .map(recipe -> new RecipeMap.Entry(recipe.id(), recipe.recipe()))
                    .toList();
            validateCompleteReloadRows(
                    map, ordered, families.get(map), preparedEpoch);
            candidates.put(map, ordered);
        }
        validateRequiredMaps(candidates);
        int t3Recipes = 0;
        int t4ToolRecipes = 0;
        int t5RecipesOnT3Maps = 0;
        for (var spec : ModProcessingMachines.T3_MACHINES) {
            List<RecipeMap.Entry> entries = candidates.get(spec.requireRecipeMap());
            int familyRows = families.get(spec.requireRecipeMap()).stream()
                    .mapToInt(RecipeMap.RecipeFamily::logicalRecipeCount)
                    .sum();
            t3Recipes += entries.size() + familyRows;
            t4ToolRecipes += (int) entries.stream()
                    .filter(entry -> isT4ToolRecipe(entry.id()))
                    .count();
            t5RecipesOnT3Maps += (int) entries.stream()
                    .filter(entry -> isChemicalRecipe(entry.id()))
                    .count();
        }
        int t3ComponentRecipes =
                t3Recipes - t4ToolRecipes - t5RecipesOnT3Maps;
        int t5ChemicalRecipes = candidates.values().stream()
                .flatMap(List::stream)
                .mapToInt(entry -> isChemicalRecipe(entry.id()) ? 1 : 0)
                .sum();
        Map<Integer, Integer> authoredMaterialRules = new HashMap<>();
        candidates.values().stream()
                .flatMap(List::stream)
                .filter(entry -> !isT11FixedRecipe(entry.id()))
                .forEach(entry -> authoredMaterialRuleStage(entry.id())
                        .ifPresent(stage -> authoredMaterialRules.merge(
                                stage, 1, Integer::sum)));
        int t7AuthoredMaterialRules = authoredMaterialRules.getOrDefault(7, 0);
        int t8PipeMaterialRules = authoredMaterialRules.getOrDefault(8, 0);
        int t10KnownFormMaterialRules = authoredMaterialRules.getOrDefault(10, 0);
        int concretePublishedRecipes = candidates.values().stream()
                .mapToInt(List::size)
                .sum();
        int lazyLogicalRecipes = families.values().stream()
                .flatMap(List::stream)
                .mapToInt(RecipeMap.RecipeFamily::lazyRecipeCount)
                .sum();
        int eagerFamilyRecipes = families.values().stream()
                .flatMap(List::stream)
                .mapToInt(RecipeMap.RecipeFamily::eagerRecipeCount)
                .sum();
        int familyLogicalRecipes = families.values().stream()
                .flatMap(List::stream)
                .mapToInt(RecipeMap.RecipeFamily::logicalRecipeCount)
                .sum();
        int eagerPublishedRecipes =
                concretePublishedRecipes + eagerFamilyRecipes;
        int allPublishedRecipes =
                concretePublishedRecipes + familyLogicalRecipes;
        validateAuthoredMaterialRuleBudgets(authoredMaterialRules);
        validatePublicationBudgets(
                t3ComponentRecipes,
                t4ToolRecipes,
                t3Recipes,
                t5ChemicalRecipes,
                t5RecipesOnT3Maps,
                allPublishedRecipes);
        int compactFamilyAuthoredEntries = compactSources.size();
        int compactFamilyLogicalRecipes = 0;
        int compactFamilyEagerRecipes = 0;
        int compactFamilyLazyRecipes = 0;
        int compactFamilyCacheCeiling = 0;
        int compactFamilyUnindexedRelations = 0;
        long compactFamilySyncBytes = 0L;
        for (CompactRecipeFamilyProvider.Snapshot compact : compactByMap.values()) {
            compactFamilyLogicalRecipes += compact.logicalRecipeCount();
            compactFamilyEagerRecipes += compact.eagerRecipeCount();
            compactFamilyLazyRecipes += compact.lazyRecipeCount();
            compactFamilyCacheCeiling += compact.cacheCeiling();
            compactFamilyUnindexedRelations += compact.unindexedRelationCount();
            compactFamilySyncBytes += compact.syncPayloadBytes();
        }
        String compactFamilyStableFingerprint = compactFingerprint(compactByMap);
        validateT14MaterializationBudgets(
                eagerPublishedRecipes,
                lazyLogicalRecipes,
                extruderFamily.cacheCeiling() + compactFamilyCacheCeiling);

        long indexStarted = System.nanoTime();
        LinkedHashMap<RecipeMap, RecipeMap.Prepared> preparedByMap =
                new LinkedHashMap<>();
        for (var entry : candidates.entrySet()) {
            RecipeMap map = entry.getKey();
            RecipeMap.Prepared prepared = map.prepareRecipes(
                    entry.getValue(),
                    families.get(map),
                    preparedEpoch);
            assertPreparedOwner(map, prepared);
            preparedByMap.put(map, prepared);
        }
        validateNoUnindexed(preparedByMap);
        long indexNanos = System.nanoTime() - indexStarted;
        long reloadNanos = System.nanoTime() - started;
        PublicationMetrics candidateMetrics = new PublicationMetrics(
                t3ComponentRecipes,
                t4ToolRecipes,
                t3Recipes,
                t5ChemicalRecipes,
                t7AuthoredMaterialRules,
                t8PipeMaterialRules,
                t10KnownFormMaterialRules,
                allPublishedRecipes,
                eagerPublishedRecipes,
                lazyLogicalRecipes,
                extruderFamily.logicalRecipeCount(),
                extruderFamily.eagerRecipeCount(),
                extruderFamily.lazyRecipeCount(),
                extruderFamily.cacheCeiling(),
                extruderFamily.syncPayloadBytes(),
                extruderFamilySources.size(),
                extruderFamily.stableFingerprint(),
                compactFamilyAuthoredEntries,
                compactFamilyLogicalRecipes,
                compactFamilyEagerRecipes,
                compactFamilyLazyRecipes,
                compactFamilyCacheCeiling,
                compactFamilyUnindexedRelations,
                compactFamilySyncBytes,
                compactFamilyStableFingerprint,
                runtimeSide,
                reloadNanos / 1_000_000L,
                indexNanos / 1_000_000L);
        long epoch = GTRecipeRuntimeEpoch.publish(
                materialPreview,
                List.copyOf(preparedByMap.values()),
                () -> lastPublicationMetrics = candidateMetrics);
        long sideReloadBudget = runtimeSide
                == ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT
                ? ModProcessingMachines.CLIENT_RECIPE_RELOAD_BUDGET_MS
                : ModProcessingMachines.RECIPE_RELOAD_BUDGET_MS;
        long sideIndexBudget = runtimeSide
                == ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT
                ? ModProcessingMachines.CLIENT_RECIPE_INDEX_BUILD_BUDGET_MS
                : ModProcessingMachines.RECIPE_INDEX_BUILD_BUDGET_MS;
        if (candidateMetrics.reloadMillis() > sideReloadBudget
                || candidateMetrics.indexMillis() > sideIndexBudget
                || candidateMetrics.t14ExtruderSyncBytes()
                        > ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES) {
            CrucibleCraft.LOGGER.warn(
                    "Recipe publication exceeded an online T14 budget: {}",
                    candidateMetrics);
        }
        for (var entry : preparedByMap.entrySet()) {
            RecipeMap map = entry.getKey();
            RecipeMap.Prepared snapshot = entry.getValue();
            CrucibleCraft.LOGGER.info(
                    "RecipeMap {} - {} recipes, {} unindexed",
                    map.id(),
                    snapshot.logicalRecipeCount(),
                    snapshot.unindexedRecipeCount());
        }
        CrucibleCraft.LOGGER.info(
                "Published recipe epoch {} with {} T3 component, {} T4 tool, {} "
                        + "T5 chemical, {} T7 authored, {} T8 pipe, and {} T10 known-form "
                        + "material-rule recipes; T14c Extruder {} logical = "
                        + "{} eager + {} lazy (cache ceiling {}, {} authored) "
                        + "({} live T3-map, {} logical total, {} eager total) "
                        + "across {} maps in {} ms; "
                        + "indexes {} ms "
                        + "(count budgets {}/{}/{}/{}/{}/{}/{}/{})",
                epoch,
                t3ComponentRecipes,
                t4ToolRecipes,
                t5ChemicalRecipes,
                t7AuthoredMaterialRules,
                t8PipeMaterialRules,
                t10KnownFormMaterialRules,
                extruderFamily.logicalRecipeCount(),
                extruderFamily.eagerRecipeCount(),
                extruderFamily.lazyRecipeCount(),
                extruderFamily.cacheCeiling(),
                extruderFamilySources.size(),
                t3Recipes,
                allPublishedRecipes,
                eagerPublishedRecipes,
                ModRecipeMaps.ALL.size(),
                candidateMetrics.reloadMillis(),
                candidateMetrics.indexMillis(),
                ModProcessingMachines.T3_COMPONENT_EXPANSION_BUDGET,
                ModProcessingMachines.T4_TOOL_EXPANSION_BUDGET,
                ModProcessingMachines.LIVE_T3_MAP_RECIPE_BUDGET,
                ModProcessingMachines.T5_CHEMICAL_RECIPE_BUDGET,
                ModProcessingMachines.T7_AUTHORED_MATERIAL_RULE_BUDGET,
                ModProcessingMachines.T8_PIPE_MATERIAL_RULE_BUDGET,
                ModProcessingMachines.T10_AUTHORED_MATERIAL_RULE_BUDGET,
                ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET);
    }

    /** Runs production Extruder lookup p95/candidate gates for verification. */
    public static T14LookupMetrics benchmarkT14LookupsForVerification() {
        RecipeMap.RecipeFamily family = ModRecipeMaps.EXTRUDER
                .family(ExtruderRecipeFamilyProvider.FAMILY_ID)
                .orElseThrow();
        int sampleCount = Math.min(
                LOOKUP_BENCHMARK_SAMPLES, family.logicalRecipeCount());
        List<GTRecipeQuery> queries = new ArrayList<>(sampleCount);
        for (int index = 0; index < sampleCount; index++) {
            int entryIndex = index * family.logicalRecipeCount() / sampleCount;
            queries.add(queryFor(family.enumerationEntry(entryIndex).recipe()));
        }
        for (GTRecipeQuery query : queries) {
            if (ModRecipeMaps.EXTRUDER.findMatch(query).isEmpty()) {
                throw new IllegalStateException(
                        "Published T14 recipe failed lookup benchmark warm-up");
            }
        }
        long[] nanos = new long[LOOKUP_BENCHMARK_TIMING_SAMPLES];
        int[] candidates = new int[
                LOOKUP_BENCHMARK_TIMING_SAMPLES
                        * LOOKUP_BENCHMARK_OPERATIONS_PER_SAMPLE];
        int candidateSample = 0;
        for (int sample = 0;
                sample < LOOKUP_BENCHMARK_TIMING_SAMPLES;
                sample++) {
            long started = System.nanoTime();
            for (int operation = 0;
                    operation < LOOKUP_BENCHMARK_OPERATIONS_PER_SAMPLE;
                    operation++) {
                GTRecipeQuery query = queries.get(Math.floorMod(
                        sample * 8191 + operation * 104729,
                        queries.size()));
                candidates[candidateSample++] = ModRecipeMaps.EXTRUDER
                        .indexedCandidateCount(query);
                if (ModRecipeMaps.EXTRUDER.findMatch(query).isEmpty()) {
                    throw new IllegalStateException(
                            "Published T14 recipe failed lookup benchmark");
                }
            }
            nanos[sample] = (System.nanoTime() - started)
                    / LOOKUP_BENCHMARK_OPERATIONS_PER_SAMPLE;
        }
        java.util.Arrays.sort(nanos);
        java.util.Arrays.sort(candidates);
        int p95Index = Math.max(
                0, (int) Math.ceil(nanos.length * 0.95D) - 1);
        int candidateP95Index = Math.max(
                0, (int) Math.ceil(candidates.length * 0.95D) - 1);
        return new T14LookupMetrics(
                nanos.length,
                candidates.length,
                nanos[p95Index],
                candidates[candidateP95Index],
                candidates[candidates.length - 1]);
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

    /**
     * @param t5ChemicalRecipes total recipes on dedicated chemical maps
     *     (T5 electrolyzer/centrifuge/bath/…, T11 distillery/generifier,
     *     and T21 mixer families).  The name retains "t5" for API stability;
     *     the counter has covered t11 since T11 and t21 since T21.
     */
    public record PublicationMetrics(
            int t3ComponentRecipes,
            int t4ToolRecipes,
            int liveT3MapRecipes,
            int t5ChemicalRecipes,
            int t7AuthoredMaterialRules,
            int t8PipeMaterialRules,
            int t10KnownFormMaterialRules,
            int allPublishedRecipes,
            int eagerPublishedRecipes,
            int lazyLogicalRecipes,
            int t14ExtruderLogicalRecipes,
            int t14ExtruderEagerRecipes,
            int t14ExtruderLazyRecipes,
            int t14ExtruderCacheCeiling,
            long t14ExtruderSyncBytes,
            int t14ExtruderAuthoredEntries,
            String t14ExtruderStableFingerprint,
            int compactFamilyAuthoredEntries,
            int compactFamilyLogicalRecipes,
            int compactFamilyEagerRecipes,
            int compactFamilyLazyRecipes,
            int compactFamilyCacheCeiling,
            int compactFamilyUnindexedRelations,
            long compactFamilySyncBytes,
            String compactFamilyStableFingerprint,
            ExtruderRecipeFamilyProvider.RuntimeSide runtimeSide,
            long reloadMillis,
            long indexMillis) {
        private static PublicationMetrics empty() {
            return new PublicationMetrics(
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    "",
                    0, 0, 0, 0, 0, 0, 0L, "",
                    ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                    0L, 0L);
        }
    }

    public record T14LookupMetrics(
            int timingSamples,
            int operations,
            long p95Nanos,
            long p95Candidates,
            long maxCandidates) {}

    public static T14OnlineBudgetGate evaluateT14OnlineBudgetGate(
            PublicationMetrics metrics,
            T14LookupMetrics lookup) {
        long reloadBudget = metrics.runtimeSide()
                == ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT
                ? ModProcessingMachines.CLIENT_RECIPE_RELOAD_BUDGET_MS
                : ModProcessingMachines.RECIPE_RELOAD_BUDGET_MS;
        return evaluateT14OnlineBudgetGate(metrics, lookup, reloadBudget);
    }

    public static T14OnlineBudgetGate evaluateT14OnlineBudgetGate(
            PublicationMetrics metrics,
            T14LookupMetrics lookup,
            long reloadBudget) {
        return evaluateT14OnlineBudgetGate(
                metrics,
                lookup,
                reloadBudget,
                ModProcessingMachines.RECIPE_LOOKUP_P95_BUDGET_NS);
    }

    public static T14OnlineBudgetGate evaluateT14OnlineBudgetGate(
            PublicationMetrics metrics,
            T14LookupMetrics lookup,
            long reloadBudget,
            long lookupP95Budget) {
        long indexBudget = metrics.runtimeSide()
                == ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT
                ? ModProcessingMachines.CLIENT_RECIPE_INDEX_BUILD_BUDGET_MS
                : ModProcessingMachines.RECIPE_INDEX_BUILD_BUDGET_MS;
        return new T14OnlineBudgetGate(
                metrics.reloadMillis() <= reloadBudget,
                metrics.indexMillis() <= indexBudget,
                metrics.t14ExtruderSyncBytes()
                        <= ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES,
                lookup.p95Nanos()
                        <= lookupP95Budget,
                lookup.p95Candidates()
                                <= ModProcessingMachines
                                        .RECIPE_LOOKUP_P95_CANDIDATE_BUDGET
                        && lookup.maxCandidates()
                                <= ModProcessingMachines
                                        .RECIPE_LOOKUP_MAX_CANDIDATE_HARD_CEILING);
    }

    public record T14OnlineBudgetGate(
            boolean sideReload,
            boolean sideIndex,
            boolean sync,
            boolean lookupP95,
            boolean lookupCandidates) {
        public boolean allPass() {
            return sideReload
                    && sideIndex
                    && sync
                    && lookupP95
                    && lookupCandidates;
        }
    }

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

    static void validateT14MaterializationBudgets(
            int eagerPublishedRecipes,
            int lazyLogicalRecipes,
            int lazyCacheCeiling) {
        if (eagerPublishedRecipes < 0
                || lazyLogicalRecipes < 0
                || lazyCacheCeiling < 0) {
            throw new IllegalArgumentException(
                    "T14 materialization counts must not be negative");
        }
        if (eagerPublishedRecipes
                > ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET) {
            throw new IllegalStateException(
                    "Eager recipe publication "
                            + eagerPublishedRecipes
                            + " exceeds hard ceiling "
                            + ModProcessingMachines
                                    .ALL_PUBLISHED_RECIPE_BUDGET);
        }
        if (eagerPublishedRecipes
                > ModProcessingMachines
                        .ALL_EAGER_PUBLICATION_SOFT_BUDGET) {
            CrucibleCraft.LOGGER.warn(
                    "Eager recipe publication {} exceeds soft budget {} "
                            + "but remains below hard ceiling {}",
                    eagerPublishedRecipes,
                    ModProcessingMachines.ALL_EAGER_PUBLICATION_SOFT_BUDGET,
                    ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET);
        }
        if (lazyLogicalRecipes
                > ModProcessingMachines
                        .ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING) {
            throw new IllegalStateException(
                    "Lazy logical recipe count "
                            + lazyLogicalRecipes
                            + " exceeds hard ceiling "
                            + ModProcessingMachines
                                    .ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING);
        }
        if (lazyCacheCeiling
                > ModProcessingMachines
                        .ALL_LAZY_RECIPE_CACHE_HARD_CEILING) {
            throw new IllegalStateException(
                    "Lazy recipe cache ceiling "
                            + lazyCacheCeiling
                            + " exceeds hard ceiling "
                            + ModProcessingMachines
                                    .ALL_LAZY_RECIPE_CACHE_HARD_CEILING);
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

    static boolean isT11ChemicalRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && (id.getPath().startsWith("t11/distillery/")
                        || id.getPath().startsWith("t11/generifier/"));
    }

    static boolean isT21ChemicalRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("t21/");
    }

    static boolean isT22PetroleumRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("t22/");
    }

    static boolean isT36BootstrapRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("t36/");
    }

    private static boolean isT14CompactExtruder(RuleSource source) {
        return CrucibleCraft.MODID.equals(source.id().getNamespace())
                && source.id().getPath().startsWith("extruder/compact/")
                && source.rule().target().filter(
                        target -> target.equals(ModRecipeMaps.EXTRUDER.id()))
                        .isPresent()
                && source.rule().sparse().isPresent();
    }

    private static boolean isChemicalRecipe(ResourceLocation id) {
        return isT5ChemicalRecipe(id) || isT11ChemicalRecipe(id)
                || isT21ChemicalRecipe(id);
    }

    private static boolean isT11FixedRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && (id.getPath().startsWith("t11/distillery/")
                        || id.getPath().startsWith("t11/generifier/")
                        || id.getPath().startsWith("t11/fuels_engine/")
                        || id.getPath().startsWith("t11/fuels_gas/"));
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
        for (var machine : ModProcessingMachines.allForRecipeMap(
                map.id())) {
            var invalid = machine.validator().validate(recipe);
            if (invalid.isPresent()) {
                throw recipeValidationError(
                        recipeId,
                        "Machine " + machine.id() + " rejected recipe for map "
                                + map.id() + " (" + invalid.get() + ")");
            }
        }
        var generator = ModFuelGenerators.forRecipeMap(map.id());
        if (generator.isPresent()) {
            var invalid = generator.get().validate(recipe);
            if (invalid.isPresent()) {
                throw recipeValidationError(
                        recipeId,
                        "Fuel generator " + generator.get().id()
                                + " rejected recipe for map "
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
        ModFuelGenerators.ALL.stream()
                .map(com.masson.cruciblecraft.machine.generation
                        .FuelGeneratorSpec::requireRecipeMap)
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
            boolean t11Chemical = isT11ChemicalRecipe(entry.id());
            boolean t21Chemical = isT21ChemicalRecipe(entry.id());
            boolean t22Petroleum = isT22PetroleumRecipe(entry.id());
            boolean t36Bootstrap = isT36BootstrapRecipe(entry.id());
            if (t5Recipe && !t5Map) {
                throw new IllegalArgumentException(
                        "T5 recipe " + entry.id() + " targets non-T5 map " + map.id());
            }
            if (dedicatedT5Map && !t5Recipe && !t11Chemical && !t21Chemical
                    && !t22Petroleum && !t36Bootstrap) {
                throw new IllegalArgumentException(
                        "Dedicated T5 map " + map.id()
                                + " requires recipe-id prefix t5/, t11/, t21/, t22/, or t36/: "
                                + entry.id());
            }
            if (t11Chemical
                    && map != ModRecipeMaps.DISTILLERY
                    && map != ModRecipeMaps.GENERIFIER) {
                throw new IllegalArgumentException(
                        "T11 chemical recipe " + entry.id()
                                + " targets unsupported map " + map.id());
            }
        }
    }

    static void validateNoUnindexed(
            Map<RecipeMap, RecipeMap.Prepared> preparedByMap) {
        for (var entry : preparedByMap.entrySet()) {
            RecipeMap map = entry.getKey();
            RecipeMap.Prepared prepared = entry.getValue();
            assertPreparedOwner(map, prepared);
            int unindexed = prepared.unindexedRecipeCount();
            if (unindexed != 0) {
                throw new IllegalArgumentException(
                        "RecipeMap " + map.id() + " contains "
                                + unindexed + " unindexed recipes; add an explicit index "
                                + "before publishing non-simple ingredients");
            }
        }
    }

    private static void assertPreparedOwner(
            RecipeMap map, RecipeMap.Prepared prepared) {
        if (!prepared.belongsTo(map)) {
            throw new IllegalArgumentException(
                    "Prepared snapshot owner does not match RecipeMap " + map.id());
        }
    }

    /**
     * Materializes every family row transiently and applies the same complete
     * pre-publication validation used for concrete rows. Enumeration must not
     * populate a family's long-term lookup cache.
     */
    static void validateCompleteReloadRows(
            RecipeMap map,
            List<RecipeMap.Entry> concrete,
            List<RecipeMap.RecipeFamily> families,
            long preparedEpoch) {
        List<RecipeMap.Entry> complete = new ArrayList<>(concrete);
        for (RecipeMap.RecipeFamily family : families) {
            if (family.epoch() != preparedEpoch) {
                throw new IllegalArgumentException(
                        "Recipe family epoch does not match reload epoch");
            }
            int cacheBefore = family.cacheSize();
            for (int index = 0; index < family.logicalRecipeCount(); index++) {
                RecipeMap.Entry entry = family.enumerationEntry(index);
                if (!entry.id().equals(family.recipeIds().get(index))) {
                    throw recipeValidationError(
                            entry.id(),
                            "Recipe family enumeration identity drifted");
                }
                complete.add(entry);
            }
            if (family.cacheSize() != cacheBefore) {
                throw new IllegalStateException(
                        "Recipe family enumeration populated lookup cache "
                                + family.familyId());
            }
        }
        for (RecipeMap.Entry entry : complete) {
            validateTarget(entry.id(), map, entry.recipe());
            List<String> unsupported =
                    ComponentIngredientIndex.unsupportedIngredientTypes(
                            entry.recipe());
            if (!unsupported.isEmpty()) {
                throw recipeValidationError(
                        entry.id(),
                        "Unsupported indexed ingredient types for map "
                                + map.id() + ": " + unsupported);
            }
        }
        validateUniqueRecipeIds(map, complete);
        validateT5RecipeProvenance(map, complete);
        validateNoShadows(map, complete);
        if (!families.isEmpty()) {
            RecipeMap.Prepared transientIndex = map.prepareRecipes(complete);
            validateNoUnindexed(Map.of(map, transientIndex));
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

    static void validateUniqueRecipeIds(
            RecipeMap map,
            List<RecipeMap.Entry> entries) {
        Set<ResourceLocation> ids = new java.util.HashSet<>();
        for (RecipeMap.Entry entry : entries) {
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException(
                        "Duplicate stable recipe id in map " + map.id() + ": "
                                + describeLogicalResource(entry.id())
                                + ". Compact family expansion must fail before epoch "
                                + "publication.");
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

    private static CompactRecipeFamilyProvider.RuntimeSide compactSide(
            ExtruderRecipeFamilyProvider.RuntimeSide side) {
        return switch (side) {
            case SERVER -> CompactRecipeFamilyProvider.RuntimeSide.SERVER;
            case DEDICATED_CLIENT ->
                    CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT;
            case INTEGRATED_CLIENT ->
                    CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT;
        };
    }

    private static String compactFingerprint(
            Map<ResourceLocation, CompactRecipeFamilyProvider.Snapshot> compactByMap) {
        if (compactByMap.isEmpty()) {
            return "";
        }
        List<CompactRecipeFamilyProvider.Snapshot> ordered =
                compactByMap.values().stream()
                        .sorted(Comparator.comparing(snapshot ->
                                snapshot.mapId().toString()))
                        .toList();
        if (ordered.size() == 1) {
            return ordered.getFirst().stableFingerprint();
        }
        StringBuilder joined = new StringBuilder();
        for (CompactRecipeFamilyProvider.Snapshot snapshot : ordered) {
            joined.append(snapshot.mapId())
                    .append('=')
                    .append(snapshot.stableFingerprint())
                    .append('\n');
        }
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(joined.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
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
