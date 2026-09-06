package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiProjectionCache;
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
    private static volatile PublicationCapacityReport lastCapacityReport =
            PublicationCapacityReport.empty();

    private GTRecipeMapLoader() {}

    public static PublicationMetrics lastPublicationMetrics() {
        return lastPublicationMetrics;
    }

    public static PublicationCapacityReport lastCapacityReport() {
        return lastCapacityReport;
    }

    public static synchronized void reload(RecipeManager manager) {
        reload(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.TAGS_UPDATED);
    }

    public static synchronized void reload(
            RecipeManager manager,
            ExtruderRecipeFamilyProvider.RuntimeSide runtimeSide) {
        reload(
                manager,
                runtimeSide,
                GTRecipeReloadCoordinator.Cause.TAGS_UPDATED);
    }

    public static synchronized void reload(
            RecipeManager manager,
            ExtruderRecipeFamilyProvider.RuntimeSide runtimeSide,
            GTRecipeReloadCoordinator.Cause cause) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(runtimeSide, "runtimeSide");
        Objects.requireNonNull(cause, "cause");
        GTRecipeReloadCoordinator.RequestIdentity identity =
                GTRecipeReloadCoordinator.identify(manager, runtimeSide, cause);
        GTRecipeReloadCoordinator.Decision decision =
                GTRecipeReloadCoordinator.decide(identity);
        if (!decision.publish()) {
            lastPublicationMetrics = lastPublicationMetrics.withControl(
                    new PublicationControlMetrics(
                            GTRecipeReloadCoordinator.requestCount(),
                            GTRecipeReloadCoordinator.publicationCount(),
                            decision.suppressedCount(),
                            identity.dataGeneration(),
                            cause.name()));
            RecipeLoadLog.flushEpoch(
                    GTRecipeRuntimeEpoch.epoch(),
                    decision.suppressedCount(),
                    cause.name(),
                    identity.dataGeneration());
            return;
        }
        long started = System.nanoTime();
        long phaseMark = started;
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
        long parseMillis = elapsedMs(phaseMark);
        phaseMark = System.nanoTime();

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
        long sourceCollectionMillis = elapsedMs(phaseMark);
        phaseMark = System.nanoTime();
        List<CompactDedupRuleDefinition> compactDedupRules =
                manager.getAllRecipesFor(
                                ModRecipes.COMPACT_DEDUP_RULE_TYPE.get())
                        .stream()
                        .sorted(Comparator.comparing(
                                holder -> holder.id().toString()))
                        .map(holder -> holder.value().definition())
                        .toList();
        CompactRecipeDeduplicator.validate(compactDedupRules);
        compactSources = CompactRecipeDeduplicator.applyPreSnapshot(
                compactSources, compactDedupRules);
        long dedupMillis = elapsedMs(phaseMark);
        phaseMark = System.nanoTime();
        Map<PublicationGroupKey, List<CompactRecipeFamilySource>>
                compactSourcesByGroup = new HashMap<>();
        for (CompactRecipeFamilySource source : compactSources) {
            PublicationGroupKey key = new PublicationGroupKey(
                    source.definition().targetMap(),
                    source.definition().resolvedPublicationGroup());
            compactSourcesByGroup
                    .computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(source);
        }
        List<CompactPublicationPolicyEntry> publicationPolicies =
                manager.getAllRecipesFor(
                                ModRecipes.COMPACT_PUBLICATION_POLICY_TYPE.get())
                        .stream()
                        .sorted(Comparator.comparing(
                                holder -> holder.id().toString()))
                        .map(holder -> holder.value())
                        .toList();
        Map<PublicationGroupKey,
                CompactRecipeFamilyProvider.MaterializationPolicy>
                compactPolicies = CompactPublicationPolicy.merge(
                        Map.of(), publicationPolicies);
        for (CompactPublicationPolicyEntry entry : publicationPolicies) {
            CompactPublicationPolicy.validateLiveSources(
                    entry.definition(),
                    compactSourcesByGroup.getOrDefault(
                            entry.definition().key(), List.of()));
        }
        for (CompactRecipeFamilySource source : compactSources) {
            PublicationGroupKey key = new PublicationGroupKey(
                    source.definition().targetMap(),
                    source.definition().resolvedPublicationGroup());
            if (!compactPolicies.containsKey(key)) {
                throw recipeValidationError(
                        source.id(),
                        "Undeclared compact publication_group "
                                + key.publicationGroup()
                                + " on target map "
                                + key.targetMap());
            }
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
                    if (isCompactExtruder(source)) {
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
        sourceCollectionMillis += elapsedMs(phaseMark);
        phaseMark = System.nanoTime();
        long preparedEpoch = GTRecipeRuntimeEpoch.nextEpoch();
        ExtruderRecipeFamilyProvider.Snapshot extruderFamily =
                ExtruderRecipeFamilyProvider.prepare(
                        extruderFamilySources,
                        effectiveMaterials,
                        materialPreview.unificationPreferences(),
                        formIndexes,
                        preparedEpoch,
                        runtimeSide);
        Map<PublicationGroupKey, CompactRecipeFamilyProvider.Snapshot>
                compactByPublicationGroup =
                CompactRecipeFamilyProvider.prepareByPublicationGroup(
                        compactSources,
                        knownMaps,
                        preparedEpoch,
                        compactSide(runtimeSide),
                        compactPolicies);
        LinkedHashMap<RecipeMap, List<RecipeMap.Entry>> candidates = new LinkedHashMap<>();
        Map<RecipeMap, List<RecipeMap.RecipeFamily>> families = new HashMap<>();
        for (RecipeMap map : ModRecipeMaps.ALL) {
            List<RecipeMap.RecipeFamily> mapFamilies = new ArrayList<>();
            if (map == ModRecipeMaps.EXTRUDER) {
                mapFamilies.add(extruderFamily);
            }
            compactByPublicationGroup.entrySet().stream()
                    .filter(entry -> entry.getKey().targetMap().equals(map.id()))
                    .map(Map.Entry::getValue)
                    .forEach(mapFamilies::add);
            families.put(map, List.copyOf(mapFamilies));
        }
        long familyPrepareMillis = elapsedMs(phaseMark);
        phaseMark = System.nanoTime();
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
                    map, ordered, families.get(map), preparedEpoch, compactDedupRules);
            candidates.put(map, ordered);
        }
        validateRequiredMaps(candidates);
        int componentHostRecipes = 0;
        int toolRecipes = 0;
        int chemicalRecipesOnComponentMaps = 0;
        for (var spec : ModProcessingMachines.COMPONENT_MACHINES) {
            List<RecipeMap.Entry> entries = candidates.get(spec.requireRecipeMap());
            int familyRows = families.get(spec.requireRecipeMap()).stream()
                    .mapToInt(RecipeMap.RecipeFamily::logicalRecipeCount)
                    .sum();
            componentHostRecipes += entries.size() + familyRows;
            toolRecipes += (int) entries.stream()
                    .filter(entry -> isToolRecipe(entry.id()))
                    .count();
            chemicalRecipesOnComponentMaps += (int) entries.stream()
                    .filter(entry -> isAuthoredChemicalRecipe(entry.id()))
                    .count();
        }
        int componentRecipes =
                componentHostRecipes - toolRecipes - chemicalRecipesOnComponentMaps;
        int chemicalPublishedRecipes = candidates.values().stream()
                .flatMap(List::stream)
                .mapToInt(entry -> isAuthoredChemicalRecipe(entry.id()) ? 1 : 0)
                .sum();
        Map<Integer, Integer> authoredMaterialRules = new HashMap<>();
        candidates.values().stream()
                .flatMap(List::stream)
                .filter(entry -> !isHydrocarbonRecipe(entry.id()))
                .forEach(entry -> authoredMaterialRuleStage(entry.id())
                        .ifPresent(stage -> authoredMaterialRules.merge(
                                stage, 1, Integer::sum)));
        int mortarAuthoredMaterialRules = authoredMaterialRules.getOrDefault(7, 0);
        int pipeMaterialRules = authoredMaterialRules.getOrDefault(8, 0);
        int ingotFormMaterialRules = authoredMaterialRules.getOrDefault(10, 0);
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
        validatePublicationInvariants(
                componentRecipes,
                toolRecipes,
                componentHostRecipes,
                chemicalPublishedRecipes,
                chemicalRecipesOnComponentMaps,
                eagerPublishedRecipes);
        int compactFamilyAuthoredEntries = compactSources.size();
        int compactFamilyLogicalRecipes = 0;
        int compactFamilyEagerRecipes = 0;
        int compactFamilyLazyRecipes = 0;
        int compactFamilyCacheCeiling = 0;
        int compactFamilyUnindexedRelations = 0;
        long compactFamilySyncBytes = 0L;
        for (CompactRecipeFamilyProvider.Snapshot compact
                : compactByPublicationGroup.values()) {
            compactFamilyLogicalRecipes += compact.logicalRecipeCount();
            compactFamilyEagerRecipes += compact.eagerRecipeCount();
            compactFamilyLazyRecipes += compact.lazyRecipeCount();
            compactFamilyCacheCeiling += compact.cacheCeiling();
            compactFamilyUnindexedRelations += compact.unindexedRelationCount();
            compactFamilySyncBytes += compact.syncPayloadBytes();
        }
        String compactFamilyStableFingerprint = compactFingerprint(
                compactByPublicationGroup);
        validateCompactLoadMaterializationInvariants(
                eagerPublishedRecipes,
                lazyLogicalRecipes,
                extruderFamily.cacheCeiling() + compactFamilyCacheCeiling);
        lastCapacityReport = evaluatePublicationCapacity(
                concretePublishedRecipes,
                compactFamilyEagerRecipes,
                eagerPublishedRecipes,
                lazyLogicalRecipes,
                extruderFamily.cacheCeiling() + compactFamilyCacheCeiling,
                compactFamilyAuthoredEntries,
                componentRecipes,
                toolRecipes,
                componentHostRecipes,
                chemicalPublishedRecipes);
        if (lastCapacityReport.unverifiedScale()) {
            CrucibleCraft.LOGGER.warn(
                    "UNVERIFIED_SCALE recipe publication capacity: {}",
                    lastCapacityReport);
        }

        long completeValidationMillis = elapsedMs(phaseMark);
        long temporaryIndexMillis = 0L;
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
        PublicationControlMetrics control = new PublicationControlMetrics(
                GTRecipeReloadCoordinator.requestCount(),
                GTRecipeReloadCoordinator.publicationCount() + 1,
                0,
                identity.dataGeneration(),
                cause.name());
        PublicationPhaseTimings phaseTimings = new PublicationPhaseTimings(
                parseMillis,
                sourceCollectionMillis,
                dedupMillis,
                familyPrepareMillis,
                completeValidationMillis,
                temporaryIndexMillis,
                indexNanos / 1_000_000L,
                0L);
        PublicationMetrics publishedMetrics = new PublicationMetrics(
                componentRecipes,
                toolRecipes,
                componentHostRecipes,
                chemicalPublishedRecipes,
                mortarAuthoredMaterialRules,
                pipeMaterialRules,
                ingotFormMaterialRules,
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
                indexNanos / 1_000_000L,
                control,
                phaseTimings,
                PublicationAllocationReport.pendingMeasurement());
        long epoch = GTRecipeRuntimeEpoch.publish(
                materialPreview,
                List.copyOf(preparedByMap.values()),
                () -> lastPublicationMetrics = publishedMetrics);
        ProcessingEmiProjectionCache.invalidate();
        long emiStarted = System.nanoTime();
        ProcessingEmiProjectionCache.planFor(
                ModProcessingMachines.CONFIGURED_MACHINES);
        long emiProjectionMillis = elapsedMs(emiStarted);
        PublicationMetrics candidateMetrics = publishedMetrics.withPhaseTimings(
                phaseTimings.withEmiProjectionMillis(emiProjectionMillis));
        lastPublicationMetrics = candidateMetrics;
        GTRecipeReloadCoordinator.markPublished(identity);
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
                || candidateMetrics.compactLoadExtruderSyncBytes()
                        + candidateMetrics.compactFamilySyncBytes()
                        > ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES) {
            CrucibleCraft.LOGGER.warn(
                    "Recipe publication exceeded an online compact-load budget: {}",
                    candidateMetrics);
        }
        int unindexedMaps = 0;
        for (var entry : preparedByMap.entrySet()) {
            int unindexed = entry.getValue().unindexedRecipeCount();
            if (unindexed != 0) {
                unindexedMaps++;
                CrucibleCraft.LOGGER.warn(
                        "RecipeMap {} - {} recipes, {} unindexed",
                        entry.getKey().id(),
                        entry.getValue().logicalRecipeCount(),
                        unindexed);
            }
        }
        RecipeLoadLog.flushEpoch(epoch, 0, cause.name(), identity.dataGeneration());
        RecipeLoadLog.publicationSummary(
                epoch,
                candidateMetrics,
                preparedByMap.size(),
                unindexedMaps);
    }

    /** Runs production Extruder lookup p95/candidate gates for verification. */
    public static CompactLoadLookupMetrics benchmarkCompactLoadLookupsForVerification() {
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
                        "Published compact-load recipe failed lookup benchmark warm-up");
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
                            "Published compact-load recipe failed lookup benchmark");
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
        return new CompactLoadLookupMetrics(
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
     * @param chemicalPublishedRecipes total recipes on dedicated chemical maps
     *     (electrolyzer/centrifuge/bath hosts, distillery/generifier,
     *     and mixer gunpowder families).
     */
    public record PublicationMetrics(
            int componentRecipes,
            int toolRecipes,
            int liveComponentMapRecipes,
            int chemicalPublishedRecipes,
            int mortarAuthoredMaterialRules,
            int pipeMaterialRules,
            int ingotFormMaterialRules,
            int allPublishedRecipes,
            int eagerPublishedRecipes,
            int lazyLogicalRecipes,
            int compactLoadExtruderLogicalRecipes,
            int compactLoadExtruderEagerRecipes,
            int compactLoadExtruderLazyRecipes,
            int compactLoadExtruderCacheCeiling,
            long compactLoadExtruderSyncBytes,
            int compactLoadExtruderAuthoredEntries,
            String compactLoadExtruderStableFingerprint,
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
            long indexMillis,
            PublicationControlMetrics control,
            PublicationPhaseTimings phaseTimings,
            PublicationAllocationReport allocation) {
        public PublicationMetrics {
            Objects.requireNonNull(control, "control");
            Objects.requireNonNull(phaseTimings, "phaseTimings");
            Objects.requireNonNull(allocation, "allocation");
        }

        private static PublicationMetrics empty() {
            return new PublicationMetrics(
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    "",
                    0, 0, 0, 0, 0, 0, 0L, "",
                    ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                    0L, 0L,
                    PublicationControlMetrics.empty(),
                    PublicationPhaseTimings.zero(),
                    PublicationAllocationReport.pendingMeasurement());
        }

        public PublicationMetrics withControl(PublicationControlMetrics control) {
            return new PublicationMetrics(
                    componentRecipes,
                    toolRecipes,
                    liveComponentMapRecipes,
                    chemicalPublishedRecipes,
                    mortarAuthoredMaterialRules,
                    pipeMaterialRules,
                    ingotFormMaterialRules,
                    allPublishedRecipes,
                    eagerPublishedRecipes,
                    lazyLogicalRecipes,
                    compactLoadExtruderLogicalRecipes,
                    compactLoadExtruderEagerRecipes,
                    compactLoadExtruderLazyRecipes,
                    compactLoadExtruderCacheCeiling,
                    compactLoadExtruderSyncBytes,
                    compactLoadExtruderAuthoredEntries,
                    compactLoadExtruderStableFingerprint,
                    compactFamilyAuthoredEntries,
                    compactFamilyLogicalRecipes,
                    compactFamilyEagerRecipes,
                    compactFamilyLazyRecipes,
                    compactFamilyCacheCeiling,
                    compactFamilyUnindexedRelations,
                    compactFamilySyncBytes,
                    compactFamilyStableFingerprint,
                    runtimeSide,
                    reloadMillis,
                    indexMillis,
                    control,
                    phaseTimings,
                    allocation);
        }

        public PublicationMetrics withPhaseTimings(
                PublicationPhaseTimings phaseTimings) {
            return new PublicationMetrics(
                    componentRecipes,
                    toolRecipes,
                    liveComponentMapRecipes,
                    chemicalPublishedRecipes,
                    mortarAuthoredMaterialRules,
                    pipeMaterialRules,
                    ingotFormMaterialRules,
                    allPublishedRecipes,
                    eagerPublishedRecipes,
                    lazyLogicalRecipes,
                    compactLoadExtruderLogicalRecipes,
                    compactLoadExtruderEagerRecipes,
                    compactLoadExtruderLazyRecipes,
                    compactLoadExtruderCacheCeiling,
                    compactLoadExtruderSyncBytes,
                    compactLoadExtruderAuthoredEntries,
                    compactLoadExtruderStableFingerprint,
                    compactFamilyAuthoredEntries,
                    compactFamilyLogicalRecipes,
                    compactFamilyEagerRecipes,
                    compactFamilyLazyRecipes,
                    compactFamilyCacheCeiling,
                    compactFamilyUnindexedRelations,
                    compactFamilySyncBytes,
                    compactFamilyStableFingerprint,
                    runtimeSide,
                    reloadMillis,
                    indexMillis,
                    control,
                    phaseTimings,
                    allocation);
        }
    }

    public record PublicationControlMetrics(
            int reloadRequestCount,
            int actualPublicationCount,
            int suppressedReloadCount,
            int dataGeneration,
            String requestCause) {
        public PublicationControlMetrics {
            Objects.requireNonNull(requestCause, "requestCause");
        }

        static PublicationControlMetrics empty() {
            return new PublicationControlMetrics(0, 0, 0, 0, "none");
        }
    }

    public record PublicationPhaseTimings(
            long parseMillis,
            long sourceCollectionMillis,
            long dedupMillis,
            long familyPrepareMillis,
            long completeValidationMillis,
            long temporaryIndexMillis,
            long finalIndexMillis,
            long emiProjectionMillis) {
        static PublicationPhaseTimings zero() {
            return new PublicationPhaseTimings(0, 0, 0, 0, 0, 0, 0, 0);
        }

        public PublicationPhaseTimings withEmiProjectionMillis(long emiProjectionMillis) {
            return new PublicationPhaseTimings(
                    parseMillis,
                    sourceCollectionMillis,
                    dedupMillis,
                    familyPrepareMillis,
                    completeValidationMillis,
                    temporaryIndexMillis,
                    finalIndexMillis,
                    emiProjectionMillis);
        }
    }

    public record PublicationAllocationReport(
            String reloadTransientAllocation,
            String retainedMemory) {
        public PublicationAllocationReport {
            Objects.requireNonNull(reloadTransientAllocation, "reloadTransientAllocation");
            Objects.requireNonNull(retainedMemory, "retainedMemory");
        }

        static PublicationAllocationReport pendingMeasurement() {
            return new PublicationAllocationReport(
                    "PENDING_MEASUREMENT",
                    "PENDING_MEASUREMENT");
        }
    }

    public record CompactLoadLookupMetrics(
            int timingSamples,
            int operations,
            long p95Nanos,
            long p95Candidates,
            long maxCandidates) {}

    public static CompactLoadOnlineBudgetGate evaluateCompactLoadOnlineBudgetGate(
            PublicationMetrics metrics,
            CompactLoadLookupMetrics lookup) {
        long reloadBudget = metrics.runtimeSide()
                == ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT
                ? ModProcessingMachines.CLIENT_RECIPE_RELOAD_BUDGET_MS
                : ModProcessingMachines.RECIPE_RELOAD_BUDGET_MS;
        return evaluateCompactLoadOnlineBudgetGate(metrics, lookup, reloadBudget);
    }

    public static CompactLoadOnlineBudgetGate evaluateCompactLoadOnlineBudgetGate(
            PublicationMetrics metrics,
            CompactLoadLookupMetrics lookup,
            long reloadBudget) {
        return evaluateCompactLoadOnlineBudgetGate(
                metrics,
                lookup,
                reloadBudget,
                ModProcessingMachines.RECIPE_LOOKUP_P95_BUDGET_NS);
    }

    public static CompactLoadOnlineBudgetGate evaluateCompactLoadOnlineBudgetGate(
            PublicationMetrics metrics,
            CompactLoadLookupMetrics lookup,
            long reloadBudget,
            long lookupP95Budget) {
        long indexBudget = metrics.runtimeSide()
                == ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT
                ? ModProcessingMachines.CLIENT_RECIPE_INDEX_BUILD_BUDGET_MS
                : ModProcessingMachines.RECIPE_INDEX_BUILD_BUDGET_MS;
        return new CompactLoadOnlineBudgetGate(
                metrics.reloadMillis() <= reloadBudget,
                metrics.indexMillis() <= indexBudget,
                metrics.compactLoadExtruderSyncBytes()
                        + metrics.compactFamilySyncBytes()
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

    public record CompactLoadOnlineBudgetGate(
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

    /**
     * Capacity telemetry. Count over a verified scale is
     * {@code UNVERIFIED_SCALE} and never aborts reload.
     */
    public record PublicationCapacityReport(
            int concreteEager,
            int compactEager,
            int eagerPublished,
            int lazyLogical,
            int cacheCeiling,
            int authoredEntries,
            int verifiedEagerScale,
            int temporaryCompatibilityCeiling,
            boolean unverifiedScale,
            List<String> warnings) {
        private static PublicationCapacityReport empty() {
            return new PublicationCapacityReport(
                    0, 0, 0, 0, 0, 0,
                    ModProcessingMachines.VERIFIED_OPENING_EAGER_PUBLISHED,
                    ModProcessingMachines.TEMPORARY_EAGER_COMPATIBILITY_CEILING,
                    false,
                    List.of());
        }
    }

    public record ReleasePerformanceVerdict(
            boolean reload,
            boolean index,
            boolean lookupP95,
            boolean lookupCandidates,
            boolean sync,
            boolean retained,
            boolean lookupAllocation) {
        public boolean allPass() {
            return reload
                    && index
                    && lookupP95
                    && lookupCandidates
                    && sync
                    && retained
                    && lookupAllocation;
        }
    }

    static void validateExpansionBudgets(
            int componentRecipes,
            int toolRecipes,
            int liveComponentMapRecipes) {
        validatePublicationBudgets(
                componentRecipes,
                toolRecipes,
                liveComponentMapRecipes,
                0,
                liveComponentMapRecipes);
    }

    static void validatePublicationBudgets(
            int componentRecipes,
            int toolRecipes,
            int liveComponentMapRecipes,
            int chemicalPublishedRecipes,
            int allPublishedRecipes) {
        validatePublicationBudgets(
                componentRecipes,
                toolRecipes,
                liveComponentMapRecipes,
                chemicalPublishedRecipes,
                liveComponentMapRecipes - componentRecipes - toolRecipes,
                allPublishedRecipes);
    }

    static void validatePublicationBudgets(
            int componentRecipes,
            int toolRecipes,
            int liveComponentMapRecipes,
            int chemicalPublishedRecipes,
            int chemicalRecipesOnComponentMaps,
            int allPublishedRecipes) {
        validatePublicationInvariants(
                componentRecipes,
                toolRecipes,
                liveComponentMapRecipes,
                chemicalPublishedRecipes,
                chemicalRecipesOnComponentMaps,
                allPublishedRecipes);
        evaluatePublicationCapacity(
                allPublishedRecipes,
                0,
                allPublishedRecipes,
                0,
                0,
                0,
                componentRecipes,
                toolRecipes,
                liveComponentMapRecipes,
                chemicalPublishedRecipes);
    }

    /**
     * Runtime correctness only: counts must be consistent and non-negative.
     * Recipe-count ceilings are not invariants.
     */
    static void validatePublicationInvariants(
            int componentRecipes,
            int toolRecipes,
            int liveComponentMapRecipes,
            int chemicalPublishedRecipes,
            int chemicalRecipesOnComponentMaps,
            int allPublishedRecipes) {
        if (componentRecipes < 0
                || toolRecipes < 0
                || chemicalPublishedRecipes < 0
                || chemicalRecipesOnComponentMaps < 0
                || chemicalRecipesOnComponentMaps > chemicalPublishedRecipes
                || allPublishedRecipes < 0
                || liveComponentMapRecipes
                        != componentRecipes + toolRecipes + chemicalRecipesOnComponentMaps
                || allPublishedRecipes
                        < (long) liveComponentMapRecipes
                                + chemicalPublishedRecipes - chemicalRecipesOnComponentMaps) {
            throw new IllegalArgumentException(
                    "Recipe publication invariants failed: component="
                            + componentRecipes + ", tools=" + toolRecipes
                            + ", live=" + liveComponentMapRecipes
                            + ", chemical=" + chemicalPublishedRecipes
                            + ", chemicalOnComponentMaps=" + chemicalRecipesOnComponentMaps
                            + ", all=" + allPublishedRecipes);
        }
    }

    /**
     * Capacity telemetry. Exceeding a verified scale emits
     * {@code UNVERIFIED_SCALE} and does not throw.
     */
    static PublicationCapacityReport evaluatePublicationCapacity(
            int concreteEager,
            int compactEager,
            int eagerPublished,
            int lazyLogical,
            int cacheCeiling,
            int authoredEntries,
            int componentRecipes,
            int toolRecipes,
            int liveComponentMapRecipes,
            int chemicalPublishedRecipes) {
        List<String> warnings = new ArrayList<>();
        maybeUnverified(
                warnings,
                "concrete_eager",
                concreteEager,
                ModProcessingMachines.VERIFIED_OPENING_CONCRETE_EAGER);
        maybeUnverified(
                warnings,
                "compact_eager",
                compactEager,
                ModProcessingMachines.VERIFIED_OPENING_COMPACT_EAGER);
        maybeUnverified(
                warnings,
                "eager_published",
                eagerPublished,
                ModProcessingMachines.VERIFIED_OPENING_EAGER_PUBLISHED);
        maybeUnverified(
                warnings,
                "lazy_logical",
                lazyLogical,
                ModProcessingMachines.VERIFIED_OPENING_LAZY_LOGICAL);
        maybeUnverified(
                warnings,
                "cache_ceiling",
                cacheCeiling,
                ModProcessingMachines.VERIFIED_OPENING_CACHE_CEILING);
        maybeUnverified(
                warnings,
                "authored_entries",
                authoredEntries,
                ModProcessingMachines.VERIFIED_OPENING_AUTHORED_ENTRIES);
        maybeUnverified(
                warnings,
                "component",
                componentRecipes,
                ModProcessingMachines.COMPONENT_EXPANSION_BUDGET);
        maybeUnverified(
                warnings,
                "tool",
                toolRecipes,
                ModProcessingMachines.TOOL_EXPANSION_BUDGET);
        maybeUnverified(
                warnings,
                "live_component",
                liveComponentMapRecipes,
                ModProcessingMachines.LIVE_COMPONENT_MAP_RECIPE_BUDGET);
        maybeUnverified(
                warnings,
                "chemical",
                chemicalPublishedRecipes,
                ModProcessingMachines.CHEMICAL_RECIPE_BUDGET);
        boolean unverified = !warnings.isEmpty();
        PublicationCapacityReport report = new PublicationCapacityReport(
                concreteEager,
                compactEager,
                eagerPublished,
                lazyLogical,
                cacheCeiling,
                authoredEntries,
                ModProcessingMachines.VERIFIED_OPENING_EAGER_PUBLISHED,
                ModProcessingMachines.TEMPORARY_EAGER_COMPATIBILITY_CEILING,
                unverified,
                List.copyOf(warnings));
        if (unverified) {
            CrucibleCraft.LOGGER.warn(
                    "UNVERIFIED_SCALE {}", String.join("; ", warnings));
        }
        return report;
    }

    private static void maybeUnverified(
            List<String> warnings,
            String axis,
            int actual,
            int verifiedScale) {
        if (actual > verifiedScale) {
            warnings.add(
                    "UNVERIFIED_SCALE:" + axis + ":" + actual + ">" + verifiedScale);
        }
    }

    /**
     * GameTest / CI hard door. Reload, index, lookup, retained memory,
     * sync, and lookup allocation may fail a release. Recipe counts do not.
     */
    public static void verifyReleasePerformance(
            PublicationMetrics metrics,
            CompactLoadLookupMetrics lookup,
            Long retainedMemoryBytes,
            Long lookupAllocationBytesPerOperation) {
        ReleasePerformanceVerdict verdict = evaluateReleasePerformance(
                metrics,
                lookup,
                retainedMemoryBytes,
                lookupAllocationBytesPerOperation);
        if (!verdict.allPass()) {
            throw new IllegalStateException(
                    "Release performance SLO failed: " + verdict
                            + " metrics=" + metrics
                            + " lookup=" + lookup
                            + " retained=" + retainedMemoryBytes
                            + " lookupAlloc=" + lookupAllocationBytesPerOperation);
        }
    }

    public static ReleasePerformanceVerdict evaluateReleasePerformance(
            PublicationMetrics metrics,
            CompactLoadLookupMetrics lookup,
            Long retainedMemoryBytes,
            Long lookupAllocationBytesPerOperation) {
        CompactLoadOnlineBudgetGate online = evaluateCompactLoadOnlineBudgetGate(
                metrics,
                lookup,
                ModProcessingMachines.VERIFICATION_RECIPE_RELOAD_BUDGET_MS,
                ModProcessingMachines.VERIFICATION_RECIPE_LOOKUP_P95_BUDGET_NS);
        long syncBytes = metrics.compactLoadExtruderSyncBytes()
                + metrics.compactFamilySyncBytes();
        boolean sync = syncBytes <= ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES;
        boolean retained = retainedMemoryBytes == null
                || retainedMemoryBytes <= (512L * 1024L * 1024L);
        boolean lookupAlloc = lookupAllocationBytesPerOperation == null
                || lookupAllocationBytesPerOperation <= (16L * 1024L * 1024L);
        return new ReleasePerformanceVerdict(
                online.sideReload(),
                online.sideIndex(),
                online.lookupP95(),
                online.lookupCandidates(),
                sync,
                retained,
                lookupAlloc);
    }

    static void validateCompactLoadMaterializationInvariants(
            int eagerPublishedRecipes,
            int lazyLogicalRecipes,
            int lazyCacheCeiling) {
        if (eagerPublishedRecipes < 0
                || lazyLogicalRecipes < 0
                || lazyCacheCeiling < 0) {
            throw new IllegalArgumentException(
                    "Compact-load materialization counts must not be negative");
        }
    }

    static void validateCompactLoadMaterializationBudgets(
            int eagerPublishedRecipes,
            int lazyLogicalRecipes,
            int lazyCacheCeiling) {
        validateCompactLoadMaterializationInvariants(
                eagerPublishedRecipes, lazyLogicalRecipes, lazyCacheCeiling);
        evaluatePublicationCapacity(
                eagerPublishedRecipes,
                0,
                eagerPublishedRecipes,
                lazyLogicalRecipes,
                lazyCacheCeiling,
                0,
                0,
                0,
                0,
                0);
    }

    static boolean isToolRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("tool/assembler/");
    }

    static boolean isAuthoredChemicalRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("chemical/");
    }

    static boolean isHydrocarbonProcessRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && (id.getPath().startsWith("hydrocarbon/distillery/")
                        || id.getPath().startsWith("hydrocarbon/generifier/"));
    }

    static boolean isPetroleumRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("petroleum/");
    }

    static boolean isMachineBootstrapRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("machine/bootstrap/");
    }

    public static boolean isBathRemainderCompactRecipe(ResourceLocation id) {
        return CompactWaveRecipeIds.isBathRemainderCompactRecipe(id);
    }

    private static boolean isCompactExtruder(RuleSource source) {
        return CrucibleCraft.MODID.equals(source.id().getNamespace())
                && source.id().getPath().startsWith("extruder/compact/")
                && source.rule().target().filter(
                        target -> target.equals(ModRecipeMaps.EXTRUDER.id()))
                        .isPresent()
                && source.rule().sparse().isPresent();
    }

    private static boolean isChemicalRecipe(ResourceLocation id) {
        return isAuthoredChemicalRecipe(id) || isHydrocarbonProcessRecipe(id);
    }

    private static boolean isHydrocarbonRecipe(ResourceLocation id) {
        return CrucibleCraft.MODID.equals(id.getNamespace())
                && (id.getPath().startsWith("hydrocarbon/distillery/")
                        || id.getPath().startsWith("hydrocarbon/generifier/")
                        || id.getPath().startsWith("hydrocarbon/fuels_engine/")
                        || id.getPath().startsWith("hydrocarbon/fuels_gas/"));
    }

    static OptionalInt authoredMaterialRuleStage(ResourceLocation id) {
        if (!CrucibleCraft.MODID.equals(id.getNamespace())) {
            return OptionalInt.empty();
        }
        String path = id.getPath();
        if (path.startsWith("mortar/ingot_to_dust/")
                || path.startsWith("mortar/gem_to_dust/")) {
            return OptionalInt.of(7);
        }
        if (path.startsWith("pipe/extruder/")) {
            return OptionalInt.of(8);
        }
        if (path.startsWith("ingot_form/anvil/")
                || path.startsWith("ingot_form/smelter/")) {
            return OptionalInt.of(10);
        }
        int slash = path.indexOf('/');
        if (slash < 2 || path.charAt(0) != 't') {
            return OptionalInt.empty();
        }
        try {
            int stage = Integer.parseInt(path.substring(1, slash));
            return stage >= 11 ? OptionalInt.of(stage) : OptionalInt.empty();
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
                    "Authored material-rule count cannot be negative for stage "
                            + stage);
        }
        Integer budget =
                ModProcessingMachines.AUTHORED_MATERIAL_RULE_BUDGETS.get(stage);
        if (budget == null) {
            throw new IllegalArgumentException(
                    "No authored material-rule budget for stage " + stage);
        }
        if (recipeCount > budget) {
            CrucibleCraft.LOGGER.warn(
                    "UNVERIFIED_SCALE:authored_material_rule_{}:{}>{}",
                    stage,
                    recipeCount,
                    budget);
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
        // Bronze chemical rows stay opening-sized; centrifuge compact rows use
        // the GT6 6-FO / 100k mB envelope.
        if (map == ModRecipeMaps.CENTRIFUGE
                && CompactWaveRecipeIds.isCentrifugeCompactRecipe(recipeId)) {
            var invalid = ModProcessingMachines.validateCentrifugeCompactEnvelope(recipe);
            if (invalid.isPresent()) {
                throw recipeValidationError(
                        recipeId,
                        "Centrifuge compact envelope rejected recipe for map "
                                + map.id() + " (" + invalid.get() + ")");
            }
        } else {
            boolean remainderCompact = isBathRemainderCompactRecipe(recipeId);
            boolean deferredRecycling =
                    CompactWaveRecipeIds.isSmelterDeferredRecyclingRecipe(recipeId);
            for (var machine : ModProcessingMachines.allForRecipeMap(
                    map.id())) {
                if ((remainderCompact || deferredRecycling)
                        && ModProcessingMachines.CHEMICAL_HOST_MACHINES.contains(machine)) {
                    // Bath remainder/identity/tiny-purified compact rows share
                    // Bath with reused chemical machines but are not
                    // chemical-dedicated. GT6 remainder IO can exceed the
                    // bronze envelope (2 FO, >4k mB).
                    // Smelter deferred MTE recovery keeps GT6 molten amounts;
                    // 13 families exceed the 8000 mB bronze output tank.
                    // Publication stays exact; GameTest executes tank-fitting
                    // representatives. Do not raise the Smelter tank gate.
                    continue;
                }
                var invalid = machine.validator().validate(recipe);
                if (invalid.isPresent()) {
                    throw recipeValidationError(
                            recipeId,
                            "Machine " + machine.id() + " rejected recipe for map "
                                    + map.id() + " (" + invalid.get() + ")");
                }
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
            boolean provisionedChemicalMap = ModProcessingMachines.CHEMICAL_DEDICATED_MACHINES.stream()
                    .map(ProcessingMachineSpec::requireRecipeMap)
                    .anyMatch(candidate -> candidate == map);
            if (entries.isEmpty() && !provisionedChemicalMap) {
                throw new IllegalArgumentException(
                        "Required playable map " + map.id() + " loaded zero recipes");
            }
        }
    }

    /**
     * Recipe ids are the mandatory stage provenance. This remains explicit
     * because bath and centrifuge host both pre-chemical and chemical recipe populations.
     */
    static void validateChemicalRecipeProvenance(
            RecipeMap map, List<RecipeMap.Entry> entries) {
        boolean chemicalMap = ModProcessingMachines.CHEMICAL_HOST_MACHINES.stream()
                .map(ProcessingMachineSpec::requireRecipeMap)
                .anyMatch(candidate -> candidate == map);
        boolean dedicatedChemicalMap = ModProcessingMachines.CHEMICAL_DEDICATED_MACHINES.stream()
                .map(ProcessingMachineSpec::requireRecipeMap)
                .anyMatch(candidate -> candidate == map);
        for (RecipeMap.Entry entry : entries) {
            boolean chemicalRecipe = isAuthoredChemicalRecipe(entry.id());
            boolean hydrocarbonProcess = isHydrocarbonProcessRecipe(entry.id());
            boolean petroleum = isPetroleumRecipe(entry.id());
            boolean machineBootstrap = isMachineBootstrapRecipe(entry.id());
            boolean hostCompact = CompactWaveRecipeIds.isCompactHostRecipe(
                    entry.id());
            boolean recovery = CompactWaveRecipeIds.isRoasterRecoveryRecipe(
                    entry.id());
            if (chemicalRecipe && !chemicalMap) {
                throw new IllegalArgumentException(
                        "Chemical recipe " + entry.id()
                                + " targets a non-chemical map " + map.id());
            }
            if (dedicatedChemicalMap && !chemicalRecipe && !hydrocarbonProcess
                    && !petroleum && !machineBootstrap && !hostCompact
                    && !recovery) {
                throw new IllegalArgumentException(
                        "Dedicated chemical map " + map.id()
                                + " requires a chemical, hydrocarbon, petroleum, "
                                + "bootstrap, or compact host recipe id: "
                                + entry.id());
            }
            if (hydrocarbonProcess
                    && map != ModRecipeMaps.DISTILLERY
                    && map != ModRecipeMaps.GENERIFIER) {
                throw new IllegalArgumentException(
                        "Hydrocarbon process recipe " + entry.id()
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
        validateCompleteReloadRows(map, concrete, families, preparedEpoch, List.of());
    }

    static void validateCompleteReloadRows(
            RecipeMap map,
            List<RecipeMap.Entry> concrete,
            List<RecipeMap.RecipeFamily> families,
            long preparedEpoch,
            List<CompactDedupRuleDefinition> dedupRules) {
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
        CompactRecipeDeduplicator.applyPostEnumeration(
                map.id(), complete, dedupRules);
        validateUniqueRecipeIds(map, complete);
        validateChemicalRecipeProvenance(map, complete);
        validateNoShadows(map, complete);
        for (RecipeMap.Entry entry : complete) {
            if (RecipeMap.wouldBeUnindexed(entry.recipe())) {
                throw new IllegalArgumentException(
                        "RecipeMap " + map.id() + " contains unindexed recipe "
                                + entry.id()
                                + "; add an explicit index before publishing "
                                + "non-simple ingredients");
            }
        }
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    static String recipeOutputIdentity(GTRecipe recipe) {
        return outputSignature(recipe) + "|" + recipe.duration() + "|" + recipe.eut();
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
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.Snapshot>
                    compactByGroup) {
        if (compactByGroup.isEmpty()) {
            return "";
        }
        List<CompactRecipeFamilyProvider.Snapshot> ordered =
                compactByGroup.values().stream()
                        .sorted(Comparator
                                .comparing((CompactRecipeFamilyProvider.Snapshot
                                        snapshot) -> snapshot.mapId().toString())
                                .thenComparing(snapshot ->
                                        snapshot.publicationGroup().toString()))
                        .toList();
        StringBuilder joined = new StringBuilder();
        for (CompactRecipeFamilyProvider.Snapshot snapshot : ordered) {
            joined.append(snapshot.mapId())
                    .append('/')
                    .append(snapshot.publicationGroup())
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

    /**
     * Stack-and-count consume identity used to recognize leftover-vanilla
     * assembler rows that wood-assembler later regenerated. Unlike {@link #inputSignature},
     * this ignores Ingredient instance toString, so independently materialized
     * {@code neoforge:components} circuit stacks still match.
     */
    static String logicalInputIdentity(GTRecipe recipe) {
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
                    + "@" + alternatives);
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

    static String inputSignature(GTRecipe recipe) {
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

    static String outputSignature(GTRecipe recipe) {
        List<String> items = recipe.itemOutputs().stream()
                .map(stack -> stack.getCount()
                        + "@" + BuiltInRegistries.ITEM.getKey(stack.getItem())
                        + "@" + stack.getComponentsPatch())
                .sorted()
                .toList();
        List<String> fluids = recipe.fluidOutputs().stream()
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
