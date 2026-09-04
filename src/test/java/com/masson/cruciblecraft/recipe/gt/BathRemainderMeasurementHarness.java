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

import org.junit.jupiter.api.Assumptions;
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
import net.minecraft.world.item.crafting.Ingredient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * bath-remainder Bath remainder measurement harness. Writes
 * {@code tools/bath_remainder_materialization_measurements.json} for the Python
 * decision builder. Timings are nonparametric p50/p95 over 21 samples.
 *
 * <p>Measures exact (189), exact_multi (13519), and card-aggregate (13708)
 * publication groups independently. Production winners are not claimed here.
 * Declared-before-measure cache ceiling is 128 per group (bath-remainder lookup/shard
 * hard envelope), hybrid duration_ticks_lte 16. Not a copied bath-mte
 * hybrid/cache-24 / duration_ticks_lte 0 winner. Skips when
 * {@link CompactGTRecipeFamilyGeneratedSupport#bathRemainderGeneratedRoot()} has no
 * generated families.
 */
class BathRemainderMeasurementHarness {
    private static final int PREPARE_WARMUP = 2;
    private static final int LOOKUP_WARMUP_OPS = 32;
    private static final int ENUMERATION_WARMUP = 1;
    private static final int SAMPLE_COUNT = 21;
    private static final int LOOKUP_OPS_PER_SAMPLE = 16;
    private static final long EPOCH = 11L;
    private static final int CACHE_CEILING = 128;
    private static final int HYBRID_DURATION_CUTOFF = 16;
    private static final int LOCKED_FAMILIES = 395;
    private static final int LOCKED_RELATIONS = 13708;
    private static final int EXACT_ROWS = 189;
    private static final int EXACT_MULTI_ROWS = 13519;
    private static final int EXACT_FAMILIES = 189;
    private static final int EXACT_MULTI_FAMILIES = 206;
    private static final Path OUTPUT = Path.of(
            "tools/bath_remainder_materialization_measurements.json");

    private static RegistryAccess registries;
    private static List<CompactRecipeFamilySource> sources;
    private static List<CompactRecipeFamilySource> exactSources;
    private static List<CompactRecipeFamilySource> multiSources;
    private static CompactRecipeFamilyProvider.EagerSelector hybridSelector;
    private static List<GTRecipeQuery> lookupQueries;
    private static List<GTRecipeQuery> exactLookupQueries;
    private static List<GTRecipeQuery> multiLookupQueries;
    private static int hybridEagerExact;
    private static int hybridEagerMulti;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.bathRemainderGeneratedRoot();
        Assumptions.assumeTrue(
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamiliesRecursive(root),
                () -> "bath-remainder generated compact families are not available at " + root);
        sources = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedSourcesRecursive(
                root, registries);
        exactSources = sources.stream()
                .filter(source -> CompactPublicationGroups.BATH_REMAINDER_EXACT
                        .equals(source.definition().resolvedPublicationGroup()))
                .toList();
        multiSources = sources.stream()
                .filter(source -> CompactPublicationGroups.BATH_REMAINDER_EXACT_MULTI
                        .equals(source.definition().resolvedPublicationGroup()))
                .toList();
        Set<ResourceLocation> eagerStableIds = new HashSet<>();
        for (CompactRecipeFamilySource source : sources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.authoredRelations()) {
                if (relation.duration() <= HYBRID_DURATION_CUTOFF) {
                    eagerStableIds.add(relation.stableId());
                }
            }
        }
        hybridEagerExact = countHybridEager(exactSources, eagerStableIds);
        hybridEagerMulti = countHybridEager(multiSources, eagerStableIds);
        hybridSelector = (index, relation) -> eagerStableIds.contains(relation.stableId());
        lookupQueries = buildLookupQueries(sources);
        exactLookupQueries = buildLookupQueries(exactSources);
        multiLookupQueries = buildLookupQueries(multiSources);
    }

    @Test
    void measureOneXExactExactMultiAndCardAndWriteArtifact() throws Exception {
        assertEquals(LOCKED_FAMILIES, sources.size());
        assertEquals(LOCKED_RELATIONS, countLogicalRows(sources));
        assertEquals(EXACT_FAMILIES, exactSources.size());
        assertEquals(EXACT_MULTI_FAMILIES, multiSources.size());
        assertEquals(EXACT_ROWS, countLogicalRows(exactSources));
        assertEquals(EXACT_MULTI_ROWS, countLogicalRows(multiSources));
        assertEquals(LOCKED_RELATIONS, lookupQueries.size());
        assertEquals(EXACT_ROWS, exactLookupQueries.size());
        assertEquals(EXACT_MULTI_ROWS, multiLookupQueries.size());
        assertTrue(
                lookupQueries.stream().noneMatch(query -> query.itemInputs().isEmpty()),
                "lookup must use generated consume items, not an empty probe");

        List<CandidateMeasurement> exact = measureGroup(
                exactSources, exactLookupQueries, EXACT_ROWS, hybridEagerExact);
        List<CandidateMeasurement> multi = measureGroup(
                multiSources, multiLookupQueries, EXACT_MULTI_ROWS, hybridEagerMulti);
        List<CandidateMeasurement> card = measureCard();

        assertEquals(LOCKED_RELATIONS, card.get(0).logical);
        assertEquals(LOCKED_RELATIONS, card.get(0).eager);
        assertEquals(0, card.get(1).eager);
        assertEquals(hybridEagerExact + hybridEagerMulti, card.get(2).eager);
        assertTrue(hybridEagerExact > 0, "bath-remainder exact hybrid must not copy bath-mte zero-eager");
        assertTrue(hybridEagerMulti > 0, "bath-remainder exact_multi hybrid must not copy bath-mte zero-eager");
        assertTrue(card.get(0).syncBytes > 0L);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        String encoded = gson.toJson(artifact(exact, multi, card)) + "\n";
        assertTrue(encoded.length() > 1);
        if (Boolean.getBoolean("cruciblecraft.writeBathRemainderMeasurements")) {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(OUTPUT, encoded, StandardCharsets.UTF_8);
            assertTrue(Files.size(OUTPUT) > 0L);
        }
    }

    private static int countLogicalRows(List<CompactRecipeFamilySource> familySources) {
        int total = 0;
        for (CompactRecipeFamilySource source : familySources) {
            total += source.authoredRelations().size();
        }
        return total;
    }

    private static int countHybridEager(
            List<CompactRecipeFamilySource> familySources,
            Set<ResourceLocation> eagerStableIds) {
        int total = 0;
        for (CompactRecipeFamilySource source : familySources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.authoredRelations()) {
                if (eagerStableIds.contains(relation.stableId())) {
                    total++;
                }
            }
        }
        return total;
    }

    private static List<CandidateMeasurement> measureGroup(
            List<CompactRecipeFamilySource> groupSources,
            List<GTRecipeQuery> queries,
            int logicalRows,
            int hybridEager) {
        CandidateMeasurement immediate = measureCandidate(
                groupSources,
                queries,
                logicalRows,
                "immediate",
                logicalRows,
                0,
                0,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        CandidateMeasurement onDemand = measureCandidate(
                groupSources,
                queries,
                logicalRows,
                "on_demand",
                0,
                logicalRows,
                Math.min(CACHE_CEILING, logicalRows),
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(
                        Math.min(CACHE_CEILING, logicalRows)));
        int hybridCache = Math.min(CACHE_CEILING, logicalRows - hybridEager);
        CandidateMeasurement hybrid = measureCandidate(
                groupSources,
                queries,
                logicalRows,
                "hybrid",
                hybridEager,
                logicalRows - hybridEager,
                hybridCache,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                        hybridCache, hybridSelector));
        assertEquals(immediate.fingerprint, onDemand.fingerprint);
        assertEquals(immediate.fingerprint, hybrid.fingerprint);
        return List.of(immediate, onDemand, hybrid);
    }

    private static List<CandidateMeasurement> measureCard() {
        PublicationGroupKey exactKey = new PublicationGroupKey(
                ModRecipeMaps.BATH.id(),
                CompactPublicationGroups.BATH_REMAINDER_EXACT);
        PublicationGroupKey multiKey = new PublicationGroupKey(
                ModRecipeMaps.BATH.id(),
                CompactPublicationGroups.BATH_REMAINDER_EXACT_MULTI);
        int exactOnDemand = Math.min(CACHE_CEILING, EXACT_ROWS);
        int multiOnDemand = Math.min(CACHE_CEILING, EXACT_MULTI_ROWS);
        int exactHybrid = Math.min(CACHE_CEILING, EXACT_ROWS - hybridEagerExact);
        int multiHybrid = Math.min(CACHE_CEILING, EXACT_MULTI_ROWS - hybridEagerMulti);
        int cardOnDemand = Math.min(CACHE_CEILING, LOCKED_RELATIONS);
        int cardHybrid = Math.min(
                CACHE_CEILING, LOCKED_RELATIONS - hybridEagerExact - hybridEagerMulti);
        CandidateMeasurement immediate = measureCardCandidate(
                "immediate",
                LOCKED_RELATIONS,
                0,
                0,
                Map.of(
                        exactKey, CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                        multiKey, CompactRecipeFamilyProvider.MaterializationPolicy.immediate()),
                0,
                0);
        CandidateMeasurement onDemand = measureCardCandidate(
                "on_demand",
                0,
                LOCKED_RELATIONS,
                cardOnDemand,
                Map.of(
                        exactKey, CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(
                                exactOnDemand),
                        multiKey, CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(
                                multiOnDemand)),
                exactOnDemand,
                multiOnDemand);
        CandidateMeasurement hybrid = measureCardCandidate(
                "hybrid",
                hybridEagerExact + hybridEagerMulti,
                LOCKED_RELATIONS - hybridEagerExact - hybridEagerMulti,
                cardHybrid,
                Map.of(
                        exactKey, CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                exactHybrid, hybridSelector),
                        multiKey, CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                multiHybrid, hybridSelector)),
                exactHybrid,
                multiHybrid);
        assertEquals(immediate.fingerprint, onDemand.fingerprint);
        assertEquals(immediate.fingerprint, hybrid.fingerprint);
        return List.of(immediate, onDemand, hybrid);
    }

    private static CandidateMeasurement measureCandidate(
            List<CompactRecipeFamilySource> groupSources,
            List<GTRecipeQuery> queries,
            int logicalRows,
            String name,
            int expectedEager,
            int expectedLazy,
            int expectedCache,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        RecipeMap map = new RecipeMap(ModRecipeMaps.BATH.id());
        CompactRecipeFamilyProvider.Snapshot correctness = prepare(
                map, groupSources, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        assertEquals(logicalRows, correctness.logicalRecipeCount());
        assertEquals(expectedEager, correctness.eagerRecipeCount());
        assertEquals(expectedLazy, correctness.lazyRecipeCount());
        assertEquals(expectedCache, correctness.cacheCeiling());
        assertEquals(logicalRows, correctness.shardCount(), name + " shard_count");
        assertEquals(0, correctness.overflowRelationCount(), name + " overflow");
        int cacheBeforeEnum = correctness.cacheSize();
        for (int index = 0; index < correctness.logicalRecipeCount(); index++) {
            assertEquals(
                    correctness.recipeIds().get(index),
                    correctness.enumerationEntry(index).id());
        }
        assertEquals(cacheBeforeEnum, correctness.cacheSize(), name);
        CompactRecipeFamilyProvider.Snapshot dedicated = prepare(
                map, groupSources,
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policy);
        assertEquals(correctness.recipeIds(), dedicated.recipeIds());
        assertEquals(correctness.stableFingerprint(), dedicated.stableFingerprint());
        IllegalArgumentException integrated = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        map,
                        groupSources,
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        policy));
        assertTrue(integrated.getMessage().contains("Integrated clients"));

        for (int warmup = 0; warmup < PREPARE_WARMUP; warmup++) {
            prepare(map, groupSources, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
            prepare(
                    map, groupSources,
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
            CompactRecipeFamilyProvider.Snapshot serverSnapshot = prepare(
                    map, groupSources, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
            serverReload[sample] = System.nanoTime() - reloadStart;
            if (allocationSupported) {
                allocation[sample] = Math.max(0L, threadAllocatedBytes() - allocBefore);
            }
            long indexStart = System.nanoTime();
            map.prepareRecipes(List.of(), List.of(serverSnapshot), EPOCH);
            serverIndex[sample] = System.nanoTime() - indexStart;
            long clientReloadStart = System.nanoTime();
            CompactRecipeFamilyProvider.Snapshot clientSnapshot = prepare(
                    map, groupSources,
                    CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policy);
            clientReload[sample] = System.nanoTime() - clientReloadStart;
            RecipeMap clientMap = new RecipeMap(ModRecipeMaps.BATH.id());
            long clientIndexStart = System.nanoTime();
            clientMap.prepareRecipes(List.of(), List.of(clientSnapshot), EPOCH);
            clientIndex[sample] = System.nanoTime() - clientIndexStart;
        }

        CompactRecipeFamilyProvider.Snapshot enumSnapshot = prepare(
                map, groupSources, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
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

        CompactRecipeFamilyProvider.Snapshot lookupSnapshot = prepare(
                map, groupSources, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        RecipeMap lookupMap = new RecipeMap(ModRecipeMaps.BATH.id());
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

    private static CandidateMeasurement measureCardCandidate(
            String name,
            int expectedEager,
            int expectedLazy,
            int expectedCardCache,
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy> policies,
            int expectedExactCache,
            int expectedMultiCache) {
        PublicationGroupKey exactKey = new PublicationGroupKey(
                ModRecipeMaps.BATH.id(),
                CompactPublicationGroups.BATH_REMAINDER_EXACT);
        PublicationGroupKey multiKey = new PublicationGroupKey(
                ModRecipeMaps.BATH.id(),
                CompactPublicationGroups.BATH_REMAINDER_EXACT_MULTI);
        RecipeMap bath = new RecipeMap(ModRecipeMaps.BATH.id());
        Map<ResourceLocation, RecipeMap> known = Map.of(ModRecipeMaps.BATH.id(), bath);

        var correctnessMap = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources, known, EPOCH,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        CompactRecipeFamilyProvider.Snapshot exactSnap = correctnessMap.get(exactKey);
        CompactRecipeFamilyProvider.Snapshot multiSnap = correctnessMap.get(multiKey);
        int logical = exactSnap.logicalRecipeCount() + multiSnap.logicalRecipeCount();
        int eager = exactSnap.eagerRecipeCount() + multiSnap.eagerRecipeCount();
        int lazy = exactSnap.lazyRecipeCount() + multiSnap.lazyRecipeCount();
        assertEquals(LOCKED_RELATIONS, logical);
        assertEquals(expectedEager, eager);
        assertEquals(expectedLazy, lazy);
        assertEquals(expectedExactCache, exactSnap.cacheCeiling());
        assertEquals(expectedMultiCache, multiSnap.cacheCeiling());
        assertEquals(0, exactSnap.overflowRelationCount() + multiSnap.overflowRelationCount());
        enumerate(exactSnap);
        enumerate(multiSnap);
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
            RecipeMap serverMap = new RecipeMap(ModRecipeMaps.BATH.id());
            Map<ResourceLocation, RecipeMap> serverKnown =
                    Map.of(ModRecipeMaps.BATH.id(), serverMap);
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
            serverMap.prepareRecipes(
                    List.of(),
                    List.of(serverSnapshots.get(exactKey), serverSnapshots.get(multiKey)),
                    EPOCH);
            serverIndex[sample] = System.nanoTime() - indexStart;

            RecipeMap clientMap = new RecipeMap(ModRecipeMaps.BATH.id());
            Map<ResourceLocation, RecipeMap> clientKnown =
                    Map.of(ModRecipeMaps.BATH.id(), clientMap);
            long clientReloadStart = System.nanoTime();
            var clientSnapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                    sources, clientKnown, EPOCH,
                    CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policies);
            clientReload[sample] = System.nanoTime() - clientReloadStart;
            long clientIndexStart = System.nanoTime();
            clientMap.prepareRecipes(
                    List.of(),
                    List.of(clientSnapshots.get(exactKey), clientSnapshots.get(multiKey)),
                    EPOCH);
            clientIndex[sample] = System.nanoTime() - clientIndexStart;
        }

        var enumSnapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources, known, EPOCH,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        for (int warmup = 0; warmup < ENUMERATION_WARMUP; warmup++) {
            enumerate(enumSnapshots.get(exactKey));
            enumerate(enumSnapshots.get(multiKey));
        }
        long[] enumeration = new long[SAMPLE_COUNT];
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            int cacheBefore = enumSnapshots.get(exactKey).cacheSize()
                    + enumSnapshots.get(multiKey).cacheSize();
            long start = System.nanoTime();
            enumerate(enumSnapshots.get(exactKey));
            enumerate(enumSnapshots.get(multiKey));
            enumeration[sample] = System.nanoTime() - start;
            assertEquals(
                    cacheBefore,
                    enumSnapshots.get(exactKey).cacheSize()
                            + enumSnapshots.get(multiKey).cacheSize());
        }

        RecipeMap lookupMap = new RecipeMap(ModRecipeMaps.BATH.id());
        var lookupSnapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources,
                Map.of(ModRecipeMaps.BATH.id(), lookupMap),
                EPOCH,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                policies);
        lookupMap.prepareRecipes(
                List.of(),
                List.of(lookupSnapshots.get(exactKey), lookupSnapshots.get(multiKey)),
                EPOCH).publish();
        int queryCount = lookupQueries.size();
        for (int warmup = 0; warmup < LOOKUP_WARMUP_OPS; warmup++) {
            lookupMap.findMatch(lookupQueries.get(warmup % queryCount));
        }
        long[] lookup = new long[SAMPLE_COUNT];
        long[] candidates = new long[SAMPLE_COUNT];
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            long total = 0L;
            int maxCandidates = 0;
            for (int op = 0; op < LOOKUP_OPS_PER_SAMPLE; op++) {
                GTRecipeQuery query = lookupQueries.get(
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

        return new CandidateMeasurement(
                name,
                expectedEager,
                expectedLazy,
                expectedCardCache,
                logical,
                exactSnap.syncPayloadBytes() + multiSnap.syncPayloadBytes(),
                exactSnap.stableFingerprint() + "/" + multiSnap.stableFingerprint(),
                exactSnap.shardCount() + multiSnap.shardCount(),
                exactSnap.overflowRelationCount() + multiSnap.overflowRelationCount(),
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

    private static CompactRecipeFamilyProvider.Snapshot prepare(
            RecipeMap map,
            List<CompactRecipeFamilySource> groupSources,
            CompactRecipeFamilyProvider.RuntimeSide side,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        return CompactRecipeFamilyProvider.prepare(map, groupSources, EPOCH, side, policy);
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
                    : source.authoredRelations()) {
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
            List<CandidateMeasurement> exact,
            List<CandidateMeasurement> multi,
            List<CandidateMeasurement> card) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("kind", "bath_remainder_generated_rows");
        input.put("logical_rows", LOCKED_RELATIONS);
        input.put("measured_logical_rows", List.of(EXACT_ROWS, EXACT_MULTI_ROWS, LOCKED_RELATIONS));
        input.put("generated_datapack_present", true);
        input.put(
                "harness",
                "BathRemainderMeasurementHarness JUnit samples; "
                        + "retained_memory uses snapshot.syncPayloadBytes, not a naive JVM "
                        + "heap delta. player_execution evidenced by BathRemainderHarnessTest "
                        + "and BathRemainderGameTests. exact, exact_multi, and card are independent.");

        Map<String, Object> family = new LinkedHashMap<>();
        family.put("family_count", LOCKED_FAMILIES);
        family.put("authored_entries_per_family", 1);
        family.put("logical_rows_per_family", "exact_or_exact_multi");
        family.put("strategy", "undecided_pending_derived_winner");
        family.put(
                "card_level_runtime_costs",
                "recorded once per scenario; never copied " + LOCKED_FAMILIES
                        + " times or averaged into per-family measurements");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", 1);
        root.put("status", "BATH_REMAINDER_MATERIALIZATION_MEASUREMENT_READY");
        root.put("production_winner_claimed", false);
        root.put("protocol", Map.of(
                "id", "bath_remainder_materialization_production_lock_v1",
                "ranking_scale", "1x",
                "diagnostic_scales_not_for_production", List.of("5x", "20x"),
                "single_wall_clock_sample_forbidden", true,
                "p50_p95_invented", false,
                "sample_count", SAMPLE_COUNT));
        root.put("input", input);
        root.put("bath_remainder_opening", bathRemainderOpening());
        root.put("family_work_set", family);
        root.put("scenarios", List.of(
                scenario("exact", EXACT_ROWS, EXACT_FAMILIES, exact),
                scenario("exact_multi", EXACT_MULTI_ROWS, EXACT_MULTI_FAMILIES, multi),
                scenario("card", LOCKED_RELATIONS, LOCKED_FAMILIES, card)));
        root.put("diagnostic_scales", Map.of(
                "5x", Map.of(
                        "logical_rows", LOCKED_RELATIONS * 5,
                        "status", "PENDING_MEASUREMENT",
                        "production_use", "forbidden",
                        "note", "Diagnostic only. Must not fill 1x unmeasured fields."),
                "20x", Map.of(
                        "logical_rows", LOCKED_RELATIONS * 20,
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

    private static Map<String, Object> bathRemainderOpening() {
        Map<String, Object> opening = new LinkedHashMap<>();
        opening.put("source", "tools/bath_mte_readiness.json#bath_remainder_opening.recipe_load_closing");
        opening.put("datapack_authored_entries", 5706);
        opening.put("eager_publication_rows", 14);
        opening.put("lazy_logical_rows", 2758);
        opening.put("lazy_cache_ceiling_rows", 222);
        opening.put("sync_bytes", 273899);
        opening.put("server_reload_ms", 30);
        opening.put("client_reload_ms", 31);
        opening.put("lookup_p95_ns", 74437);
        opening.put("lookup_candidate_count", 10);
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
                "bath-remainder-added compact snapshot payload (syncPayloadBytes); not a naive JVM heap delta");
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
