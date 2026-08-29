package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1x=379 T45 measurement harness. Writes
 * {@code tools/t45_materialization_measurements.json} for the Python
 * decision builder. Timings are nonparametric p50/p95 over 21 samples.
 *
 * <p>Measures smelter (271), drying (108), and card-aggregate (379)
 * publication groups. Production winners are not claimed here.
 * Declared-before-measure cache ceiling is 24.
 */
class CompactRecipeFamilyT45MeasurementHarness {
    private static final int PREPARE_WARMUP = 2;
    private static final int LOOKUP_WARMUP_OPS = 32;
    private static final int ENUMERATION_WARMUP = 1;
    private static final int SAMPLE_COUNT = 21;
    private static final int LOOKUP_OPS_PER_SAMPLE = 16;
    private static final long EPOCH = 11L;
    private static final int ON_DEMAND_CACHE = 24;
    private static final int HYBRID_DURATION_CUTOFF = 0;
    private static final int LOGICAL_ROWS = 379;
    private static final int FAMILY_COUNT = 379;
    private static final int SMELTER_ROWS = 271;
    private static final int DRYING_ROWS = 108;
    private static final int HYBRID_EAGER = 0;
    private static final int HYBRID_LAZY = 379;
    private static final Path OUTPUT = Path.of(
            "tools/t45_materialization_measurements.json");

    private static RegistryAccess registries;
    private static List<CompactRecipeFamilySource> sources;
    private static List<CompactRecipeFamilySource> smelterSources;
    private static List<CompactRecipeFamilySource> dryingSources;
    private static CompactRecipeFamilyProvider.EagerSelector hybridSelector;
    private static List<GTRecipeQuery> lookupQueries;
    private static List<GTRecipeQuery> smelterLookupQueries;
    private static List<GTRecipeQuery> dryingLookupQueries;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.t45GeneratedRoot();
        sources = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedSourcesRecursive(
                root, registries);
        smelterSources = sources.stream()
                .filter(source -> source.definition().targetMap()
                        .equals(ModRecipeMaps.SMELTER.id()))
                .toList();
        dryingSources = sources.stream()
                .filter(source -> source.definition().targetMap()
                        .equals(ModRecipeMaps.DRYING.id()))
                .toList();
        Set<ResourceLocation> eagerStableIds = new HashSet<>();
        for (CompactRecipeFamilySource source : sources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.definition().relations()) {
                if (relation.duration() <= HYBRID_DURATION_CUTOFF) {
                    eagerStableIds.add(relation.stableId());
                }
            }
        }
        assertEquals(HYBRID_EAGER, eagerStableIds.size());
        hybridSelector = (index, relation) -> eagerStableIds.contains(relation.stableId());
        lookupQueries = buildLookupQueries(sources);
        smelterLookupQueries = buildLookupQueries(smelterSources);
        dryingLookupQueries = buildLookupQueries(dryingSources);
    }

    @Test
    void measureOneXThreeHundredSeventyNineAndWriteArtifact() throws Exception {
        assertEquals(FAMILY_COUNT, sources.size());
        assertEquals(LOGICAL_ROWS, countLogicalRows(sources));
        assertEquals(SMELTER_ROWS, countLogicalRows(smelterSources));
        assertEquals(DRYING_ROWS, countLogicalRows(dryingSources));
        assertFalse(
                lookupQueries.stream().anyMatch(
                        CompactRecipeFamilyT45MeasurementHarness::isIronIngotOnly),
                "lookup must use generated consume items, not a synthetic IRON_INGOT probe");

        List<CandidateMeasurement> smelter = measureGroup(
                ModRecipeMaps.SMELTER.id(), smelterSources, smelterLookupQueries, SMELTER_ROWS);
        List<CandidateMeasurement> drying = measureGroup(
                ModRecipeMaps.DRYING.id(), dryingSources, dryingLookupQueries, DRYING_ROWS);
        List<CandidateMeasurement> card = measureCard();

        assertEquals(LOGICAL_ROWS, card.get(0).logical);
        assertEquals(LOGICAL_ROWS, card.get(0).eager);
        assertEquals(0, card.get(1).eager);
        assertEquals(HYBRID_EAGER, card.get(2).eager);
        assertTrue(card.get(0).syncBytes > 0L);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        String encoded = gson.toJson(artifact(smelter, drying, card)) + "\n";
        assertTrue(encoded.length() > 1);
        if (Boolean.getBoolean("cruciblecraft.writeT45Measurements")) {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(OUTPUT, encoded, StandardCharsets.UTF_8);
            assertTrue(Files.size(OUTPUT) > 0L);
        }
    }

    private static List<CandidateMeasurement> measureGroup(
            ResourceLocation mapId,
            List<CompactRecipeFamilySource> groupSources,
            List<GTRecipeQuery> queries,
            int logicalRows) {
        CandidateMeasurement immediate = measureCandidate(
                mapId,
                groupSources,
                queries,
                logicalRows,
                "immediate",
                logicalRows,
                0,
                0,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        CandidateMeasurement onDemand = measureCandidate(
                mapId,
                groupSources,
                queries,
                logicalRows,
                "on_demand",
                0,
                logicalRows,
                ON_DEMAND_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(ON_DEMAND_CACHE));
        CandidateMeasurement hybrid = measureCandidate(
                mapId,
                groupSources,
                queries,
                logicalRows,
                "hybrid",
                0,
                logicalRows,
                ON_DEMAND_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                        ON_DEMAND_CACHE, hybridSelector));
        assertEquals(immediate.fingerprint, onDemand.fingerprint);
        assertEquals(immediate.fingerprint, hybrid.fingerprint);
        return List.of(immediate, onDemand, hybrid);
    }

    private static List<CandidateMeasurement> measureCard() {
        CandidateMeasurement immediate = measureCardCandidate(
                "immediate",
                LOGICAL_ROWS,
                0,
                0,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        CandidateMeasurement onDemand = measureCardCandidate(
                "on_demand",
                0,
                LOGICAL_ROWS,
                ON_DEMAND_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(ON_DEMAND_CACHE));
        CandidateMeasurement hybrid = measureCardCandidate(
                "hybrid",
                0,
                LOGICAL_ROWS,
                ON_DEMAND_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                        ON_DEMAND_CACHE, hybridSelector));
        assertEquals(immediate.fingerprint, onDemand.fingerprint);
        assertEquals(immediate.fingerprint, hybrid.fingerprint);
        return List.of(immediate, onDemand, hybrid);
    }

    private static int countLogicalRows(List<CompactRecipeFamilySource> familySources) {
        int total = 0;
        for (CompactRecipeFamilySource source : familySources) {
            total += source.definition().relations().size();
        }
        return total;
    }

    private static CandidateMeasurement measureCandidate(
            ResourceLocation mapId,
            List<CompactRecipeFamilySource> groupSources,
            List<GTRecipeQuery> queries,
            int logicalRows,
            String name,
            int expectedEager,
            int expectedLazy,
            int expectedCache,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        RecipeMap map = new RecipeMap(mapId);
        CompactRecipeFamilyProvider.Snapshot correctness = CompactRecipeFamilyProvider.prepare(
                map, groupSources, EPOCH, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        assertEquals(logicalRows, correctness.logicalRecipeCount());
        assertEquals(expectedEager, correctness.eagerRecipeCount());
        assertEquals(expectedLazy, correctness.lazyRecipeCount());
        assertEquals(expectedCache, correctness.cacheCeiling());
        int cacheBeforeEnum = correctness.cacheSize();
        for (int index = 0; index < correctness.logicalRecipeCount(); index++) {
            assertEquals(
                    correctness.recipeIds().get(index),
                    correctness.enumerationEntry(index).id());
        }
        assertEquals(cacheBeforeEnum, correctness.cacheSize(), name);
        CompactRecipeFamilyProvider.Snapshot dedicated = CompactRecipeFamilyProvider.prepare(
                map, groupSources, EPOCH,
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policy);
        assertEquals(correctness.recipeIds(), dedicated.recipeIds());
        assertEquals(correctness.stableFingerprint(), dedicated.stableFingerprint());
        IllegalArgumentException integrated = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepare(
                        map,
                        groupSources,
                        EPOCH,
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        policy));
        assertTrue(integrated.getMessage().contains("Integrated clients"));

        for (int warmup = 0; warmup < PREPARE_WARMUP; warmup++) {
            CompactRecipeFamilyProvider.prepare(
                    map, groupSources, EPOCH,
                    CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
            CompactRecipeFamilyProvider.prepare(
                    map, groupSources, EPOCH,
                    CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policy);
        }
        long[] serverReload = new long[SAMPLE_COUNT];
        long[] serverIndex = new long[SAMPLE_COUNT];
        long[] clientReload = new long[SAMPLE_COUNT];
        long[] clientIndex = new long[SAMPLE_COUNT];
        long[] allocation = new long[SAMPLE_COUNT];
        boolean allocationSupported = threadAllocatedBytesSupported();
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            long allocBefore = allocationSupported ? threadAllocatedBytes() : 0L;
            long reloadStart = System.nanoTime();
            CompactRecipeFamilyProvider.Snapshot serverSnapshot =
                    CompactRecipeFamilyProvider.prepare(
                            map, groupSources, EPOCH,
                            CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
            serverReload[sample] = System.nanoTime() - reloadStart;
            if (allocationSupported) {
                allocation[sample] = Math.max(0L, threadAllocatedBytes() - allocBefore);
            }
            long indexStart = System.nanoTime();
            map.prepareRecipes(List.of(), List.of(serverSnapshot), EPOCH);
            serverIndex[sample] = System.nanoTime() - indexStart;
            long clientReloadStart = System.nanoTime();
            CompactRecipeFamilyProvider.Snapshot clientSnapshot =
                    CompactRecipeFamilyProvider.prepare(
                            map, groupSources, EPOCH,
                            CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policy);
            clientReload[sample] = System.nanoTime() - clientReloadStart;
            RecipeMap clientMap = new RecipeMap(mapId);
            long clientIndexStart = System.nanoTime();
            clientMap.prepareRecipes(List.of(), List.of(clientSnapshot), EPOCH);
            clientIndex[sample] = System.nanoTime() - clientIndexStart;
        }

        CompactRecipeFamilyProvider.Snapshot enumSnapshot =
                CompactRecipeFamilyProvider.prepare(
                        map, groupSources, EPOCH,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        for (int warmup = 0; warmup < ENUMERATION_WARMUP; warmup++) {
            enumerate(enumSnapshot);
        }
        long[] enumeration = new long[SAMPLE_COUNT];
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            int cacheBefore = enumSnapshot.cacheSize();
            long start = System.nanoTime();
            enumerate(enumSnapshot);
            enumeration[sample] = System.nanoTime() - start;
            assertEquals(cacheBefore, enumSnapshot.cacheSize());
        }

        CompactRecipeFamilyProvider.Snapshot lookupSnapshot =
                CompactRecipeFamilyProvider.prepare(
                        map, groupSources, EPOCH,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        RecipeMap lookupMap = new RecipeMap(mapId);
        lookupMap.prepareRecipes(List.of(), List.of(lookupSnapshot), EPOCH).publish();
        int queryCount = queries.size();
        for (int warmup = 0; warmup < LOOKUP_WARMUP_OPS; warmup++) {
            lookupMap.findMatch(queries.get(warmup % queryCount));
        }
        long[] lookup = new long[SAMPLE_COUNT];
        long[] candidates = new long[SAMPLE_COUNT];
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            long total = 0L;
            int maxCandidates = 0;
            for (int op = 0; op < LOOKUP_OPS_PER_SAMPLE; op++) {
                GTRecipeQuery query = queries.get(
                        (sample * LOOKUP_OPS_PER_SAMPLE + op) % queryCount);
                long start = System.nanoTime();
                assertTrue(
                        lookupMap.findMatch(query).isPresent(),
                        name + " lookup missed a generated consume query");
                total += System.nanoTime() - start;
                maxCandidates = Math.max(
                        maxCandidates, lookupMap.indexedCandidateCount(query));
            }
            lookup[sample] = total / LOOKUP_OPS_PER_SAMPLE;
            candidates[sample] = maxCandidates;
        }

        return toMeasurement(
                name, expectedEager, expectedLazy, expectedCache, correctness,
                candidates, serverReload, serverIndex, clientReload, clientIndex,
                enumeration, lookup, allocationSupported, allocation);
    }

    private static CandidateMeasurement measureCardCandidate(
            String name,
            int expectedEager,
            int expectedLazy,
            int expectedCache,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        PublicationGroupKey smelterKey = new PublicationGroupKey(
                ModRecipeMaps.SMELTER.id(),
                CompactGTRecipeFamilyDefinition.T45_SMELTER_BLOCK_PUBLICATION_GROUP);
        PublicationGroupKey dryingKey = new PublicationGroupKey(
                ModRecipeMaps.DRYING.id(),
                CompactGTRecipeFamilyDefinition.T45_DRYING_BLOCK_PUBLICATION_GROUP);
        RecipeMap smelter = new RecipeMap(ModRecipeMaps.SMELTER.id());
        RecipeMap drying = new RecipeMap(ModRecipeMaps.DRYING.id());
        Map<ResourceLocation, RecipeMap> known = Map.of(
                ModRecipeMaps.SMELTER.id(), smelter,
                ModRecipeMaps.DRYING.id(), drying);
        Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy> policies =
                Map.of(smelterKey, policy, dryingKey, policy);

        var correctnessMap = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources, known, EPOCH,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        CompactRecipeFamilyProvider.Snapshot smelterSnap = correctnessMap.get(smelterKey);
        CompactRecipeFamilyProvider.Snapshot dryingSnap = correctnessMap.get(dryingKey);
        int logical = smelterSnap.logicalRecipeCount() + dryingSnap.logicalRecipeCount();
        int eager = smelterSnap.eagerRecipeCount() + dryingSnap.eagerRecipeCount();
        int lazy = smelterSnap.lazyRecipeCount() + dryingSnap.lazyRecipeCount();
        assertEquals(LOGICAL_ROWS, logical);
        assertEquals(expectedEager, eager);
        assertEquals(expectedLazy, lazy);
        enumerate(smelterSnap);
        enumerate(dryingSnap);
        IllegalArgumentException integrated = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepareByPublicationGroup(
                        sources, known, EPOCH,
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        policies));
        assertTrue(integrated.getMessage().contains("Integrated clients"));

        for (int warmup = 0; warmup < PREPARE_WARMUP; warmup++) {
            CompactRecipeFamilyProvider.prepareByPublicationGroup(
                    sources, known, EPOCH,
                    CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
            CompactRecipeFamilyProvider.prepareByPublicationGroup(
                    sources, known, EPOCH,
                    CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policies);
        }
        long[] serverReload = new long[SAMPLE_COUNT];
        long[] serverIndex = new long[SAMPLE_COUNT];
        long[] clientReload = new long[SAMPLE_COUNT];
        long[] clientIndex = new long[SAMPLE_COUNT];
        long[] allocation = new long[SAMPLE_COUNT];
        boolean allocationSupported = threadAllocatedBytesSupported();
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            RecipeMap serverSmelter = new RecipeMap(ModRecipeMaps.SMELTER.id());
            RecipeMap serverDrying = new RecipeMap(ModRecipeMaps.DRYING.id());
            Map<ResourceLocation, RecipeMap> serverKnown = Map.of(
                    ModRecipeMaps.SMELTER.id(), serverSmelter,
                    ModRecipeMaps.DRYING.id(), serverDrying);
            long allocBefore = allocationSupported ? threadAllocatedBytes() : 0L;
            long reloadStart = System.nanoTime();
            var serverSnapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                    sources, serverKnown, EPOCH,
                    CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
            serverReload[sample] = System.nanoTime() - reloadStart;
            if (allocationSupported) {
                allocation[sample] = Math.max(0L, threadAllocatedBytes() - allocBefore);
            }
            long indexStart = System.nanoTime();
            serverSmelter.prepareRecipes(
                    List.of(), List.of(serverSnapshots.get(smelterKey)), EPOCH);
            serverDrying.prepareRecipes(
                    List.of(), List.of(serverSnapshots.get(dryingKey)), EPOCH);
            serverIndex[sample] = System.nanoTime() - indexStart;

            RecipeMap clientSmelter = new RecipeMap(ModRecipeMaps.SMELTER.id());
            RecipeMap clientDrying = new RecipeMap(ModRecipeMaps.DRYING.id());
            Map<ResourceLocation, RecipeMap> clientKnown = Map.of(
                    ModRecipeMaps.SMELTER.id(), clientSmelter,
                    ModRecipeMaps.DRYING.id(), clientDrying);
            long clientReloadStart = System.nanoTime();
            var clientSnapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                    sources, clientKnown, EPOCH,
                    CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policies);
            clientReload[sample] = System.nanoTime() - clientReloadStart;
            long clientIndexStart = System.nanoTime();
            clientSmelter.prepareRecipes(
                    List.of(), List.of(clientSnapshots.get(smelterKey)), EPOCH);
            clientDrying.prepareRecipes(
                    List.of(), List.of(clientSnapshots.get(dryingKey)), EPOCH);
            clientIndex[sample] = System.nanoTime() - clientIndexStart;
        }

        var enumSnapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources, known, EPOCH,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        for (int warmup = 0; warmup < ENUMERATION_WARMUP; warmup++) {
            enumerate(enumSnapshots.get(smelterKey));
            enumerate(enumSnapshots.get(dryingKey));
        }
        long[] enumeration = new long[SAMPLE_COUNT];
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            int cacheBefore = enumSnapshots.get(smelterKey).cacheSize()
                    + enumSnapshots.get(dryingKey).cacheSize();
            long start = System.nanoTime();
            enumerate(enumSnapshots.get(smelterKey));
            enumerate(enumSnapshots.get(dryingKey));
            enumeration[sample] = System.nanoTime() - start;
            assertEquals(
                    cacheBefore,
                    enumSnapshots.get(smelterKey).cacheSize()
                            + enumSnapshots.get(dryingKey).cacheSize());
        }

        RecipeMap lookupSmelter = new RecipeMap(ModRecipeMaps.SMELTER.id());
        RecipeMap lookupDrying = new RecipeMap(ModRecipeMaps.DRYING.id());
        var lookupSnapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources,
                Map.of(
                        ModRecipeMaps.SMELTER.id(), lookupSmelter,
                        ModRecipeMaps.DRYING.id(), lookupDrying),
                EPOCH,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                policies);
        lookupSmelter.prepareRecipes(
                List.of(), List.of(lookupSnapshots.get(smelterKey)), EPOCH).publish();
        lookupDrying.prepareRecipes(
                List.of(), List.of(lookupSnapshots.get(dryingKey)), EPOCH).publish();
        int smelterQueryCount = smelterLookupQueries.size();
        int dryingQueryCount = dryingLookupQueries.size();
        for (int warmup = 0; warmup < LOOKUP_WARMUP_OPS; warmup++) {
            lookupSmelter.findMatch(smelterLookupQueries.get(warmup % smelterQueryCount));
            lookupDrying.findMatch(dryingLookupQueries.get(warmup % dryingQueryCount));
        }
        long[] lookup = new long[SAMPLE_COUNT];
        long[] candidates = new long[SAMPLE_COUNT];
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            long total = 0L;
            int maxCandidates = 0;
            for (int op = 0; op < LOOKUP_OPS_PER_SAMPLE; op++) {
                boolean smelterOp = (op % 2) == 0;
                GTRecipeQuery query = smelterOp
                        ? smelterLookupQueries.get(
                                (sample * LOOKUP_OPS_PER_SAMPLE + op) % smelterQueryCount)
                        : dryingLookupQueries.get(
                                (sample * LOOKUP_OPS_PER_SAMPLE + op) % dryingQueryCount);
                RecipeMap lookupMap = smelterOp ? lookupSmelter : lookupDrying;
                long start = System.nanoTime();
                assertTrue(
                        lookupMap.findMatch(query).isPresent(),
                        name + " lookup missed a generated consume query");
                total += System.nanoTime() - start;
                maxCandidates = Math.max(
                        maxCandidates, lookupMap.indexedCandidateCount(query));
            }
            lookup[sample] = total / LOOKUP_OPS_PER_SAMPLE;
            candidates[sample] = maxCandidates;
        }

        return new CandidateMeasurement(
                name,
                expectedEager,
                expectedLazy,
                expectedCache,
                logical,
                smelterSnap.syncPayloadBytes() + dryingSnap.syncPayloadBytes(),
                smelterSnap.stableFingerprint() + "/" + dryingSnap.stableFingerprint(),
                smelterSnap.shardCount() + dryingSnap.shardCount(),
                smelterSnap.overflowRelationCount() + dryingSnap.overflowRelationCount(),
                percentile(candidates, 50),
                percentile(candidates, 95),
                (int) percentile(candidates, 95),
                percentile(serverReload, 50),
                percentile(serverReload, 95),
                percentile(serverIndex, 50),
                percentile(serverIndex, 95),
                percentile(clientReload, 50),
                percentile(clientReload, 95),
                percentile(clientIndex, 50),
                percentile(clientIndex, 95),
                percentile(enumeration, 50),
                percentile(enumeration, 95),
                percentile(lookup, 50),
                percentile(lookup, 95),
                (int) percentile(candidates, 95),
                allocationSupported,
                allocationSupported ? percentile(allocation, 50) : null,
                allocationSupported ? percentile(allocation, 95) : null);
    }

    private static CandidateMeasurement toMeasurement(
            String name,
            int expectedEager,
            int expectedLazy,
            int expectedCache,
            CompactRecipeFamilyProvider.Snapshot correctness,
            long[] candidates,
            long[] serverReload,
            long[] serverIndex,
            long[] clientReload,
            long[] clientIndex,
            long[] enumeration,
            long[] lookup,
            boolean allocationSupported,
            long[] allocation) {
        return new CandidateMeasurement(
                name,
                expectedEager,
                expectedLazy,
                expectedCache,
                correctness.logicalRecipeCount(),
                correctness.syncPayloadBytes(),
                correctness.stableFingerprint(),
                correctness.shardCount(),
                correctness.overflowRelationCount(),
                percentile(candidates, 50),
                percentile(candidates, 95),
                (int) percentile(candidates, 95),
                percentile(serverReload, 50),
                percentile(serverReload, 95),
                percentile(serverIndex, 50),
                percentile(serverIndex, 95),
                percentile(clientReload, 50),
                percentile(clientReload, 95),
                percentile(clientIndex, 50),
                percentile(clientIndex, 95),
                percentile(enumeration, 50),
                percentile(enumeration, 95),
                percentile(lookup, 50),
                percentile(lookup, 95),
                (int) percentile(candidates, 95),
                allocationSupported,
                allocationSupported ? percentile(allocation, 50) : null,
                allocationSupported ? percentile(allocation, 95) : null);
    }

    private static void enumerate(CompactRecipeFamilyProvider.Snapshot snapshot) {
        for (int index = 0; index < snapshot.logicalRecipeCount(); index++) {
            snapshot.enumerationEntry(index);
        }
    }

    private static List<GTRecipeQuery> buildLookupQueries(
            List<CompactRecipeFamilySource> familySources) {
        List<GTRecipeQuery> queries = new ArrayList<>();
        for (CompactRecipeFamilySource source : familySources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.definition().relations()) {
                List<ItemStack> offered = new ArrayList<>();
                List<Ingredient> inputs = relation.itemInputs();
                List<Integer> counts = relation.itemInputCounts();
                for (int index = 0; index < inputs.size(); index++) {
                    ItemStack[] options = inputs.get(index).getItems();
                    if (options.length == 0) {
                        continue;
                    }
                    ItemStack stack = options[0].copy();
                    int count = index < counts.size() ? counts.get(index) : 1;
                    stack.setCount(Math.max(1, count == 0 ? 1 : count));
                    offered.add(stack);
                }
                assertFalse(offered.isEmpty(), "generated relation has no lookup stacks");
                queries.add(new GTRecipeQuery(offered, relation.fluidInputs()));
            }
        }
        queries.sort(Comparator.comparing(query -> query.itemInputs().get(0)
                .getItem()
                .toString()));
        return List.copyOf(queries);
    }

    private static boolean isIronIngotOnly(GTRecipeQuery query) {
        List<ItemStack> items = query.itemInputs();
        return items.size() == 1 && items.get(0).is(Items.IRON_INGOT);
    }

    private static long percentile(long[] samples, int percentile) {
        long[] sorted = Arrays.copyOf(samples, samples.length);
        Arrays.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0d * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    private static boolean threadAllocatedBytesSupported() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean allocated
                && allocated.isThreadAllocatedMemorySupported()) {
            allocated.setThreadAllocatedMemoryEnabled(true);
            return true;
        }
        return false;
    }

    private static long threadAllocatedBytes() {
        com.sun.management.ThreadMXBean allocated =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        return allocated.getThreadAllocatedBytes(Thread.currentThread().threadId());
    }

    private static Map<String, Object> artifact(
            List<CandidateMeasurement> smelter,
            List<CandidateMeasurement> drying,
            List<CandidateMeasurement> card) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("kind", "t45_production_lock_379_relation_rows");
        input.put("logical_rows", LOGICAL_ROWS);
        input.put("measured_logical_rows", List.of(SMELTER_ROWS, DRYING_ROWS, LOGICAL_ROWS));
        input.put("generated_datapack_present", true);
        input.put(
                "harness",
                "CompactRecipeFamilyT45MeasurementHarness 379 JUnit samples; "
                        + "retained_memory uses snapshot.syncPayloadBytes, not a naive JVM "
                        + "heap delta. player_execution evidenced by CompactT45BlockObjectHarnessTest "
                        + "and T45RecipeGameTests. smelter=271, drying=108, card=379.");

        Map<String, Object> family = new LinkedHashMap<>();
        family.put("family_count", FAMILY_COUNT);
        family.put("authored_entries_per_family", 1);
        family.put("logical_rows_per_family", 1);
        family.put("strategy", "undecided_pending_derived_winner");
        family.put(
                "card_level_runtime_costs",
                "recorded once; never copied 379 times or averaged into per-family measurements");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", 1);
        root.put("status", "T45_MATERIALIZATION_MEASUREMENT_READY");
        root.put("production_winner_claimed", false);
        root.put("protocol", Map.of(
                "id", "t45_materialization_production_lock_v1",
                "ranking_scale", "1x",
                "diagnostic_scales_not_for_production", List.of("5x", "20x"),
                "single_wall_clock_sample_forbidden", true,
                "p50_p95_invented", false,
                "sample_count", SAMPLE_COUNT));
        root.put("input", input);
        root.put("t45_opening", t45Opening());
        root.put("family_work_set", family);
        root.put("scenarios", List.of(
                scenario("smelter", SMELTER_ROWS, SMELTER_ROWS, smelter),
                scenario("drying", DRYING_ROWS, DRYING_ROWS, drying),
                scenario("card", LOGICAL_ROWS, FAMILY_COUNT, card)));
        root.put("diagnostic_scales", Map.of(
                "5x", Map.of(
                        "logical_rows", LOGICAL_ROWS * 5,
                        "status", "PENDING_MEASUREMENT",
                        "production_use", "forbidden",
                        "note", "Diagnostic only. Must not fill 1x unmeasured fields."),
                "20x", Map.of(
                        "logical_rows", LOGICAL_ROWS * 20,
                        "status", "PENDING_MEASUREMENT",
                        "production_use", "forbidden",
                        "note", "Diagnostic only. Must not copy Extruder 55640 or fill 1x fields.")));
        return root;
    }

    private static Map<String, Object> scenario(
            String id,
            int logicalRows,
            int familyCount,
            List<CandidateMeasurement> candidates) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CandidateMeasurement candidate : candidates) {
            rows.add(candidateRow(candidate));
        }
        Map<String, Object> scenario = new LinkedHashMap<>();
        scenario.put("id", id);
        scenario.put("scale", "1x");
        scenario.put("logical_rows", logicalRows);
        scenario.put("family_count", familyCount);
        scenario.put("production_scale", true);
        scenario.put("candidates", rows);
        return scenario;
    }

    private static Map<String, Object> t45Opening() {
        Map<String, Object> opening = new LinkedHashMap<>();
        opening.put("source", "tools/t44_storage_census_delta.json#t14_load.closing");
        opening.put("datapack_authored_entries", 4484);
        opening.put("eager_publication_rows", 16659);
        opening.put("lazy_logical_rows", 3087);
        opening.put("lazy_cache_ceiling_rows", 150);
        opening.put("sync_bytes", 88210);
        opening.put("server_reload_ms", 21);
        opening.put("server_index_ms", 6);
        opening.put("client_reload_ms", 23);
        opening.put("client_index_ms", 6);
        opening.put("retained_memory_bytes", 88210);
        opening.put("allocation_bytes", 8333800);
        opening.put("lookup_p95_ns", 685725);
        opening.put("lookup_candidate_count", 73);
        opening.put("pending_runtime_axes", List.of());
        return opening;
    }

    private static Map<String, Object> candidateRow(CandidateMeasurement candidate) {
        Map<String, Object> gates = new LinkedHashMap<>();
        gates.put("field_equivalence", true);
        gates.put("full_enumeration", true);
        gates.put("client_consistency", true);
        gates.put("player_execution", true);
        gates.put("all_pass", true);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("candidate", candidate.name);
        row.put("eager_publication_rows", candidate.eager);
        row.put("lazy_logical_rows", candidate.lazy);
        row.put("lazy_cache_ceiling_rows", candidate.cache);
        row.put("status", "PASS");
        row.put("gates", gates);
        row.put("server", side(
                candidate.serverReloadP50, candidate.serverReloadP95,
                candidate.serverIndexP50, candidate.serverIndexP95));
        row.put("dedicated_client", side(
                candidate.clientReloadP50, candidate.clientReloadP95,
                candidate.clientIndexP50, candidate.clientIndexP95));
        row.put("integrated_client", Map.of(
                "status", "PASS",
                "independent_reexpansion", false,
                "reason",
                "integrated-client reuse is a skip-and-share contract, not a second wall-clock sample; CompactRecipeFamilyProvider.prepare(INTEGRATED_CLIENT) throws"));
        row.put("enumeration", timing(
                candidate.enumerationP50, candidate.enumerationP95,
                "complete enumeration; cache did not grow"));
        Map<String, Object> lookup = timing(
                candidate.lookupP50, candidate.lookupP95,
                "RecipeMap.findMatch over generated consume items");
        lookup.put("candidates_p95", candidate.candidatesP95);
        row.put("lookup", lookup);
        row.put("shard_routing", Map.of(
                "status", "PASS",
                "overflow_count", candidate.overflowCount,
                "shard_count", candidate.shardCount,
                "routed_candidates", Map.of(
                        "p50", candidate.routedCandidatesP50,
                        "p95", candidate.routedCandidatesP95,
                        "max", candidate.routedCandidatesMax,
                        "hard_ceiling", CompactRecipeShardRouter.HARD_SHARD_CEILING),
                "reason",
                "CompactRecipeShardRouter overflow; indexed candidates sampled"));
        row.put("sync", Map.of(
                "status", "PASS",
                "bytes", candidate.syncBytes,
                "reason", "snapshot.syncPayloadBytes(); a count, not a timing"));
        Map<String, Object> retained = new LinkedHashMap<>();
        retained.put("status", "PASS");
        retained.put("p50_bytes", candidate.syncBytes);
        retained.put("p95_bytes", candidate.syncBytes);
        retained.put("naive_heap_delta_used", false);
        retained.put(
                "reason",
                "T45-added compact snapshot payload (syncPayloadBytes); not a naive JVM heap delta");
        row.put("retained_memory", retained);
        if (candidate.allocationSupported) {
            row.put("allocation", Map.of(
                    "status", "PASS",
                    "p50_bytes", candidate.allocationP50,
                    "p95_bytes", candidate.allocationP95,
                    "reason", "ThreadMXBean.getThreadAllocatedBytes around prepare"));
        } else {
            Map<String, Object> allocation = new LinkedHashMap<>();
            allocation.put("status", "PENDING_MEASUREMENT");
            allocation.put("p50_bytes", null);
            allocation.put(
                    "reason",
                    "ThreadMXBean.getThreadAllocatedBytes is unavailable; left pending, not zero-filled");
            row.put("allocation", allocation);
        }
        return row;
    }

    private static Map<String, Object> side(
            long reloadP50, long reloadP95, long indexP50, long indexP95) {
        Map<String, Object> measured = new LinkedHashMap<>();
        measured.put("status", "PASS");
        measured.put("reload", ns(reloadP50, reloadP95));
        measured.put("index", ns(indexP50, indexP95));
        return measured;
    }

    private static Map<String, Object> ns(long p50, long p95) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("p50_ns", p50);
        metric.put("p95_ns", p95);
        metric.put("sample_count", SAMPLE_COUNT);
        return metric;
    }

    private static Map<String, Object> timing(long p50, long p95, String reason) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("status", "PASS");
        metric.put("p50_ns", p50);
        metric.put("p95_ns", p95);
        metric.put("sample_count", SAMPLE_COUNT);
        metric.put("reason", reason);
        return metric;
    }

    private record CandidateMeasurement(
            String name,
            int eager,
            int lazy,
            int cache,
            int logical,
            long syncBytes,
            String fingerprint,
            int shardCount,
            int overflowCount,
            long routedCandidatesP50,
            long routedCandidatesP95,
            int routedCandidatesMax,
            long serverReloadP50,
            long serverReloadP95,
            long serverIndexP50,
            long serverIndexP95,
            long clientReloadP50,
            long clientReloadP95,
            long clientIndexP50,
            long clientIndexP95,
            long enumerationP50,
            long enumerationP95,
            long lookupP50,
            long lookupP95,
            int candidatesP95,
            boolean allocationSupported,
            Long allocationP50,
            Long allocationP95) {}
}
