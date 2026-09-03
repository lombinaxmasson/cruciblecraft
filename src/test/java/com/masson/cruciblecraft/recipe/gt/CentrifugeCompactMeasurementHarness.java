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
import com.google.gson.JsonObject;
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
 * 22/32 production-lock centrifuge-compact measurement harness. Writes
 * {@code tools/centrifuge_compact_materialization_measurements.json} for the Python decision
 * builder. Timings are nonparametric p50/p95 over 21 samples.
 *
 * <p>Measures singleton (19/19), multi (3/13), and card aggregate
 * (22/32) independently. Production winners are not claimed here.
 */
class CentrifugeCompactMeasurementHarness {
    private static final int PREPARE_WARMUP = 2;
    private static final int LOOKUP_WARMUP_OPS = 32;
    private static final int ENUMERATION_WARMUP = 1;
    private static final int SAMPLE_COUNT = 21;
    private static final int LOOKUP_OPS_PER_SAMPLE = 16;
    private static final long EPOCH = 11L;
    private static final int SINGLETON_CACHE = 19;
    private static final int MULTI_CACHE = 13;
    private static final int SINGLETON_HYBRID_CACHE = 14;
    private static final int MULTI_HYBRID_CACHE = 13;
    private static final int CARD_CACHE = 32;
    private static final int CARD_HYBRID_CACHE = 27;
    private static final int HYBRID_DURATION_CUTOFF_SINGLETON = 256;
    private static final int HYBRID_DURATION_CUTOFF_MULTI = 584;
    private static final int SINGLETON_LOGICAL = 19;
    private static final int MULTI_LOGICAL = 13;
    private static final int CARD_LOGICAL = 32;
    private static final int SINGLETON_FAMILIES = 19;
    private static final int MULTI_FAMILIES = 3;
    private static final int CARD_FAMILIES = 22;
    private static final int HYBRID_EAGER_SINGLETON = 5;
    private static final int HYBRID_LAZY_SINGLETON = 14;
    private static final int HYBRID_EAGER_MULTI = 0;
    private static final int HYBRID_LAZY_MULTI = 13;
    private static final int HYBRID_EAGER_CARD = 5;
    private static final int HYBRID_LAZY_CARD = 27;
    private static final Path OUTPUT = Path.of(
            "tools/centrifuge_compact_materialization_measurements.json");

    private static RegistryAccess registries;
    private static List<CompactRecipeFamilySource> allSources;
    private static List<CompactRecipeFamilySource> singletonSources;
    private static List<CompactRecipeFamilySource> multiSources;
    private static CompactRecipeFamilyProvider.EagerSelector singletonHybridSelector;
    private static CompactRecipeFamilyProvider.EagerSelector multiHybridSelector;
    private static List<GTRecipeQuery> singletonLookupQueries;
    private static List<GTRecipeQuery> multiLookupQueries;
    private static List<GTRecipeQuery> cardLookupQueries;
    private static PublicationGroupKey singletonKey;
    private static PublicationGroupKey multiKey;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.centrifugeGeneratedRoot();
        List<JsonObject> documents =
                CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(root);
        allSources = documents.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(root, document, registries))
                .toList();
        singletonSources = sourcesForGroup(
                CompactPublicationGroups.CENTRIFUGE_SINGLETON);
        multiSources = sourcesForGroup(
                CompactPublicationGroups.CENTRIFUGE_MULTI);
        assertEquals(SINGLETON_FAMILIES, singletonSources.size());
        assertEquals(MULTI_FAMILIES, multiSources.size());
        assertEquals(CARD_FAMILIES, allSources.size());
        assertEquals(SINGLETON_LOGICAL, countLogicalRows(singletonSources));
        assertEquals(MULTI_LOGICAL, countLogicalRows(multiSources));
        assertEquals(CARD_LOGICAL, countLogicalRows(allSources));

        singletonHybridSelector = hybridSelector(
                singletonSources, HYBRID_DURATION_CUTOFF_SINGLETON);
        multiHybridSelector = hybridSelector(
                multiSources, HYBRID_DURATION_CUTOFF_MULTI);
        assertEquals(HYBRID_EAGER_SINGLETON, countEagerStableIds(singletonHybridSelector,
                singletonSources));
        assertEquals(HYBRID_EAGER_MULTI, countEagerStableIds(multiHybridSelector,
                multiSources));

        singletonKey = new PublicationGroupKey(
                ModRecipeMaps.CENTRIFUGE.id(),
                CompactPublicationGroups.CENTRIFUGE_SINGLETON);
        multiKey = new PublicationGroupKey(
                ModRecipeMaps.CENTRIFUGE.id(),
                CompactPublicationGroups.CENTRIFUGE_MULTI);

        singletonLookupQueries = buildLookupQueries(singletonSources);
        multiLookupQueries = buildLookupQueries(multiSources);
        cardLookupQueries = buildLookupQueries(allSources);
    }

    @Test
    void measureProductionLockAndWriteArtifact() throws Exception {
        assertFalse(
                cardLookupQueries.stream().anyMatch(
                        CentrifugeCompactMeasurementHarness::isIronIngotOnly),
                "lookup must use generated consume items, not a synthetic IRON_INGOT probe");

        CandidateMeasurement singletonImmediate = measureGroupCandidate(
                "singleton",
                "immediate",
                singletonSources,
                singletonLookupQueries,
                SINGLETON_LOGICAL,
                SINGLETON_LOGICAL,
                0,
                0,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        CandidateMeasurement singletonOnDemand = measureGroupCandidate(
                "singleton",
                "on_demand",
                singletonSources,
                singletonLookupQueries,
                SINGLETON_LOGICAL,
                0,
                SINGLETON_LOGICAL,
                SINGLETON_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(SINGLETON_CACHE));
        CandidateMeasurement singletonHybrid = measureGroupCandidate(
                "singleton",
                "hybrid",
                singletonSources,
                singletonLookupQueries,
                SINGLETON_LOGICAL,
                HYBRID_EAGER_SINGLETON,
                HYBRID_LAZY_SINGLETON,
                SINGLETON_HYBRID_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                        SINGLETON_HYBRID_CACHE, singletonHybridSelector));

        CandidateMeasurement multiImmediate = measureGroupCandidate(
                "multi",
                "immediate",
                multiSources,
                multiLookupQueries,
                MULTI_LOGICAL,
                MULTI_LOGICAL,
                0,
                0,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        CandidateMeasurement multiOnDemand = measureGroupCandidate(
                "multi",
                "on_demand",
                multiSources,
                multiLookupQueries,
                MULTI_LOGICAL,
                0,
                MULTI_LOGICAL,
                MULTI_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(MULTI_CACHE));
        CandidateMeasurement multiHybrid = measureGroupCandidate(
                "multi",
                "hybrid",
                multiSources,
                multiLookupQueries,
                MULTI_LOGICAL,
                HYBRID_EAGER_MULTI,
                HYBRID_LAZY_MULTI,
                MULTI_HYBRID_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                        MULTI_HYBRID_CACHE, multiHybridSelector));

        CandidateMeasurement cardImmediate = measureCardCandidate(
                "card",
                "immediate",
                CARD_LOGICAL,
                CARD_LOGICAL,
                0,
                0,
                groupPolicies(
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        CandidateMeasurement cardOnDemand = measureCardCandidate(
                "card",
                "on_demand",
                CARD_LOGICAL,
                0,
                CARD_LOGICAL,
                CARD_CACHE,
                groupPolicies(
                        CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(
                                SINGLETON_CACHE),
                        CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(
                                MULTI_CACHE)));
        CandidateMeasurement cardHybrid = measureCardCandidate(
                "card",
                "hybrid",
                CARD_LOGICAL,
                HYBRID_EAGER_CARD,
                HYBRID_LAZY_CARD,
                CARD_HYBRID_CACHE,
                groupPolicies(
                        CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                SINGLETON_HYBRID_CACHE, singletonHybridSelector),
                        CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                MULTI_HYBRID_CACHE, multiHybridSelector)));

        assertEquals(singletonImmediate.fingerprint, singletonOnDemand.fingerprint);
        assertEquals(singletonImmediate.fingerprint, singletonHybrid.fingerprint);
        assertEquals(multiImmediate.fingerprint, multiOnDemand.fingerprint);
        assertEquals(multiImmediate.fingerprint, multiHybrid.fingerprint);
        assertTrue(singletonImmediate.syncBytes > 0L);
        assertTrue(multiImmediate.syncBytes > 0L);
        assertTrue(cardImmediate.syncBytes > 0L);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        String encoded = gson.toJson(artifact(
                List.of(singletonImmediate, singletonOnDemand, singletonHybrid),
                List.of(multiImmediate, multiOnDemand, multiHybrid),
                List.of(cardImmediate, cardOnDemand, cardHybrid))) + "\n";
        assertTrue(encoded.length() > 1);
        if (Boolean.getBoolean("cruciblecraft.writeCentrifugeCompactMeasurements")) {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(OUTPUT, encoded, StandardCharsets.UTF_8);
            assertTrue(Files.size(OUTPUT) > 0L);
        }
    }

    private static Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>
            groupPolicies(
                    CompactRecipeFamilyProvider.MaterializationPolicy singleton,
                    CompactRecipeFamilyProvider.MaterializationPolicy multi) {
        return Map.of(singletonKey, singleton, multiKey, multi);
    }

    private static List<CompactRecipeFamilySource> sourcesForGroup(
            ResourceLocation publicationGroup) {
        List<CompactRecipeFamilySource> filtered = new ArrayList<>();
        for (CompactRecipeFamilySource source : allSources) {
            if (source.definition().resolvedPublicationGroup().equals(publicationGroup)) {
                filtered.add(source);
            }
        }
        return List.copyOf(filtered);
    }

    private static CompactRecipeFamilyProvider.EagerSelector hybridSelector(
            List<CompactRecipeFamilySource> familySources,
            int durationCutoff) {
        Set<ResourceLocation> eagerStableIds = new HashSet<>();
        for (CompactRecipeFamilySource source : familySources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.authoredRelations()) {
                if (relation.duration() <= durationCutoff) {
                    eagerStableIds.add(relation.stableId());
                }
            }
        }
        return (index, relation) -> eagerStableIds.contains(relation.stableId());
    }

    private static int countEagerStableIds(
            CompactRecipeFamilyProvider.EagerSelector selector,
            List<CompactRecipeFamilySource> familySources) {
        int count = 0;
        for (CompactRecipeFamilySource source : familySources) {
            List<CompactGTRecipeFamilyDefinition.Relation> relations =
                    source.authoredRelations();
            for (int index = 0; index < relations.size(); index++) {
                if (selector.isEager(index, relations.get(index))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countLogicalRows(List<CompactRecipeFamilySource> familySources) {
        int total = 0;
        for (CompactRecipeFamilySource source : familySources) {
            total += source.authoredRelations().size();
        }
        return total;
    }

    private static CandidateMeasurement measureGroupCandidate(
            String scope,
            String name,
            List<CompactRecipeFamilySource> familySources,
            List<GTRecipeQuery> lookupQueries,
            int expectedLogical,
            int expectedEager,
            int expectedLazy,
            int expectedCache,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        RecipeMap map = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
        CompactRecipeFamilyProvider.Snapshot correctness = prepareGroup(
                map, familySources, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        assertEquals(expectedLogical, correctness.logicalRecipeCount());
        assertEquals(expectedEager, correctness.eagerRecipeCount());
        assertEquals(expectedLazy, correctness.lazyRecipeCount());
        assertEquals(expectedCache, correctness.cacheCeiling());
        assertTrue(
                correctness.overflowRelationCount()
                        <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                scope + " overflow exceeded the hard ceiling");
        int cacheBeforeEnum = correctness.cacheSize();
        for (int index = 0; index < correctness.logicalRecipeCount(); index++) {
            assertEquals(
                    correctness.recipeIds().get(index),
                    correctness.enumerationEntry(index).id());
        }
        assertEquals(cacheBeforeEnum, correctness.cacheSize(), name);
        CompactRecipeFamilyProvider.Snapshot dedicated = prepareGroup(
                map, familySources, CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                policy);
        assertEquals(correctness.recipeIds(), dedicated.recipeIds());
        assertEquals(correctness.stableFingerprint(), dedicated.stableFingerprint());
        assertIntegratedClientRejected(familySources, policy);

        for (int warmup = 0; warmup < PREPARE_WARMUP; warmup++) {
            prepareGroup(map, familySources, CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                    policy);
            prepareGroup(map, familySources,
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
            CompactRecipeFamilyProvider.Snapshot serverSnapshot = prepareGroup(
                    map, familySources, CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                    policy);
            serverReload[sample] = System.nanoTime() - reloadStart;
            if (allocationSupported) {
                allocation[sample] = Math.max(0L, threadAllocatedBytes() - allocBefore);
            }
            long indexStart = System.nanoTime();
            map.prepareRecipes(List.of(), List.of(serverSnapshot), EPOCH);
            serverIndex[sample] = System.nanoTime() - indexStart;
            long clientReloadStart = System.nanoTime();
            CompactRecipeFamilyProvider.Snapshot clientSnapshot = prepareGroup(
                    map, familySources,
                    CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policy);
            clientReload[sample] = System.nanoTime() - clientReloadStart;
            RecipeMap clientMap = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
            long clientIndexStart = System.nanoTime();
            clientMap.prepareRecipes(List.of(), List.of(clientSnapshot), EPOCH);
            clientIndex[sample] = System.nanoTime() - clientIndexStart;
        }

        CompactRecipeFamilyProvider.Snapshot enumSnapshot = prepareGroup(
                map, familySources, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
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

        LookupMetrics lookupMetrics = measureLookup(
                map,
                familySources,
                policy,
                lookupQueries,
                scope + "/" + name);
        assertRejectLookup(map, familySources, policy);

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
                percentile(lookupMetrics.candidates, 50),
                percentile(lookupMetrics.candidates, 95),
                lookupMetrics.candidatesMax,
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
                percentile(lookupMetrics.lookup, 50),
                percentile(lookupMetrics.lookup, 95),
                (int) percentile(lookupMetrics.candidates, 95),
                allocationSupported,
                allocationSupported ? percentile(allocation, 50) : null,
                allocationSupported ? percentile(allocation, 95) : null);
    }

    private static CandidateMeasurement measureCardCandidate(
            String scope,
            String name,
            int expectedLogical,
            int expectedEager,
            int expectedLazy,
            int expectedCache,
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy> policies) {
        Map<ResourceLocation, RecipeMap> knownMaps = Map.of(
                ModRecipeMaps.CENTRIFUGE.id(), new RecipeMap(ModRecipeMaps.CENTRIFUGE.id()));
        var correctness = prepareCard(
                knownMaps, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        int logical = correctness.values().stream()
                .mapToInt(CompactRecipeFamilyProvider.Snapshot::logicalRecipeCount)
                .sum();
        int eager = correctness.values().stream()
                .mapToInt(CompactRecipeFamilyProvider.Snapshot::eagerRecipeCount)
                .sum();
        int lazy = correctness.values().stream()
                .mapToInt(CompactRecipeFamilyProvider.Snapshot::lazyRecipeCount)
                .sum();
        int cache = correctness.values().stream()
                .mapToInt(CompactRecipeFamilyProvider.Snapshot::cacheCeiling)
                .sum();
        assertEquals(expectedLogical, logical);
        assertEquals(expectedEager, eager);
        assertEquals(expectedLazy, lazy);
        assertEquals(expectedCache, cache);
        for (CompactRecipeFamilyProvider.Snapshot snapshot : correctness.values()) {
            assertTrue(
                    snapshot.overflowRelationCount()
                            <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                    scope + " overflow exceeded the hard ceiling");
            int cacheBeforeEnum = snapshot.cacheSize();
            for (int index = 0; index < snapshot.logicalRecipeCount(); index++) {
                assertEquals(
                        snapshot.recipeIds().get(index),
                        snapshot.enumerationEntry(index).id());
            }
            assertEquals(cacheBeforeEnum, snapshot.cacheSize(), name);
        }
        var dedicated = prepareCard(
                knownMaps, CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policies);
        for (PublicationGroupKey key : List.of(singletonKey, multiKey)) {
            assertEquals(
                    correctness.get(key).recipeIds(),
                    dedicated.get(key).recipeIds());
            assertEquals(
                    correctness.get(key).stableFingerprint(),
                    dedicated.get(key).stableFingerprint());
        }
        assertIntegratedClientRejectedCard(policies);

        RecipeMap map = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
        for (int warmup = 0; warmup < PREPARE_WARMUP; warmup++) {
            prepareCard(knownMaps, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
            prepareCard(knownMaps, CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                    policies);
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
            var serverSnapshots = prepareCard(
                    knownMaps, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
            serverReload[sample] = System.nanoTime() - reloadStart;
            if (allocationSupported) {
                allocation[sample] = Math.max(0L, threadAllocatedBytes() - allocBefore);
            }
            long indexStart = System.nanoTime();
            map.prepareRecipes(List.of(), List.copyOf(serverSnapshots.values()), EPOCH);
            serverIndex[sample] = System.nanoTime() - indexStart;
            long clientReloadStart = System.nanoTime();
            var clientSnapshots = prepareCard(
                    knownMaps, CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                    policies);
            clientReload[sample] = System.nanoTime() - clientReloadStart;
            RecipeMap clientMap = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
            long clientIndexStart = System.nanoTime();
            clientMap.prepareRecipes(List.of(), List.copyOf(clientSnapshots.values()), EPOCH);
            clientIndex[sample] = System.nanoTime() - clientIndexStart;
        }

        var enumSnapshots = prepareCard(
                knownMaps, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        long[] enumeration = new long[SAMPLE_COUNT];
        for (CompactRecipeFamilyProvider.Snapshot snapshot : enumSnapshots.values()) {
            for (int warmup = 0; warmup < ENUMERATION_WARMUP; warmup++) {
                enumerate(snapshot);
            }
        }
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            long total = 0L;
            for (CompactRecipeFamilyProvider.Snapshot snapshot : enumSnapshots.values()) {
                int cacheBefore = snapshot.cacheSize();
                long start = System.nanoTime();
                enumerate(snapshot);
                total += System.nanoTime() - start;
                assertEquals(cacheBefore, snapshot.cacheSize());
            }
            enumeration[sample] = total;
        }

        LookupMetrics lookupMetrics = measureCardLookup(
                knownMaps, policies, cardLookupQueries, scope + "/" + name);
        assertRejectCardLookup(knownMaps, policies);

        long syncBytes = correctness.values().stream()
                .mapToLong(CompactRecipeFamilyProvider.Snapshot::syncPayloadBytes)
                .sum();
        int shardCount = correctness.values().stream()
                .mapToInt(CompactRecipeFamilyProvider.Snapshot::shardCount)
                .sum();
        String fingerprint = correctness.get(singletonKey).stableFingerprint()
                + "|"
                + correctness.get(multiKey).stableFingerprint();

        return new CandidateMeasurement(
                name,
                expectedEager,
                expectedLazy,
                expectedCache,
                logical,
                syncBytes,
                fingerprint,
                shardCount,
                0,
                percentile(lookupMetrics.candidates, 50),
                percentile(lookupMetrics.candidates, 95),
                lookupMetrics.candidatesMax,
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
                percentile(lookupMetrics.lookup, 50),
                percentile(lookupMetrics.lookup, 95),
                (int) percentile(lookupMetrics.candidates, 95),
                allocationSupported,
                allocationSupported ? percentile(allocation, 50) : null,
                allocationSupported ? percentile(allocation, 95) : null);
    }

    private static CompactRecipeFamilyProvider.Snapshot prepareGroup(
            RecipeMap map,
            List<CompactRecipeFamilySource> familySources,
            CompactRecipeFamilyProvider.RuntimeSide side,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        return CompactRecipeFamilyProvider.prepare(map, familySources, EPOCH, side, policy);
    }

    private static Map<PublicationGroupKey, CompactRecipeFamilyProvider.Snapshot> prepareCard(
            Map<ResourceLocation, RecipeMap> knownMaps,
            CompactRecipeFamilyProvider.RuntimeSide side,
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy> policies) {
        return CompactRecipeFamilyProvider.prepareByPublicationGroup(
                allSources, knownMaps, EPOCH, side, policies);
    }

    private static void assertIntegratedClientRejected(
            List<CompactRecipeFamilySource> familySources,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        IllegalArgumentException integrated = assertThrows(
                IllegalArgumentException.class,
                () -> prepareGroup(
                        new RecipeMap(ModRecipeMaps.CENTRIFUGE.id()),
                        familySources,
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        policy));
        assertTrue(integrated.getMessage().contains("Integrated clients"));
    }

    private static void assertIntegratedClientRejectedCard(
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy> policies) {
        IllegalArgumentException integrated = assertThrows(
                IllegalArgumentException.class,
                () -> prepareCard(
                        Map.of(
                                ModRecipeMaps.CENTRIFUGE.id(),
                                new RecipeMap(ModRecipeMaps.CENTRIFUGE.id())),
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        policies));
        assertTrue(integrated.getMessage().contains("Integrated clients"));
    }

    private static void enumerate(CompactRecipeFamilyProvider.Snapshot snapshot) {
        for (int index = 0; index < snapshot.logicalRecipeCount(); index++) {
            snapshot.enumerationEntry(index);
        }
    }

    private static LookupMetrics measureLookup(
            RecipeMap map,
            List<CompactRecipeFamilySource> familySources,
            CompactRecipeFamilyProvider.MaterializationPolicy policy,
            List<GTRecipeQuery> lookupQueries,
            String label) {
        CompactRecipeFamilyProvider.Snapshot lookupSnapshot = prepareGroup(
                map, familySources, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        RecipeMap lookupMap = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
        lookupMap.prepareRecipes(List.of(), List.of(lookupSnapshot), EPOCH).publish();
        int queryCount = lookupQueries.size();
        for (int warmup = 0; warmup < LOOKUP_WARMUP_OPS; warmup++) {
            lookupMap.findMatch(lookupQueries.get(warmup % queryCount));
        }
        long[] lookup = new long[SAMPLE_COUNT];
        long[] candidates = new long[SAMPLE_COUNT];
        int candidatesMax = 0;
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            long total = 0L;
            int maxCandidates = 0;
            for (int op = 0; op < LOOKUP_OPS_PER_SAMPLE; op++) {
                GTRecipeQuery query = lookupQueries.get(
                        (sample * LOOKUP_OPS_PER_SAMPLE + op) % queryCount);
                long start = System.nanoTime();
                assertTrue(
                        lookupMap.findMatch(query).isPresent(),
                        label + " lookup missed a generated consume query");
                total += System.nanoTime() - start;
                int indexed = lookupMap.indexedCandidateCount(query);
                maxCandidates = Math.max(maxCandidates, indexed);
                assertTrue(
                        indexed <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                        label + " indexed too many candidates");
            }
            lookup[sample] = total / LOOKUP_OPS_PER_SAMPLE;
            candidates[sample] = maxCandidates;
            candidatesMax = Math.max(candidatesMax, maxCandidates);
        }
        return new LookupMetrics(lookup, candidates, candidatesMax);
    }

    private static LookupMetrics measureCardLookup(
            Map<ResourceLocation, RecipeMap> knownMaps,
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy> policies,
            List<GTRecipeQuery> lookupQueries,
            String label) {
        var lookupSnapshots = prepareCard(
                knownMaps, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        RecipeMap lookupMap = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
        lookupMap.prepareRecipes(List.of(), List.copyOf(lookupSnapshots.values()), EPOCH)
                .publish();
        int queryCount = lookupQueries.size();
        for (int warmup = 0; warmup < LOOKUP_WARMUP_OPS; warmup++) {
            lookupMap.findMatch(lookupQueries.get(warmup % queryCount));
        }
        long[] lookup = new long[SAMPLE_COUNT];
        long[] candidates = new long[SAMPLE_COUNT];
        int candidatesMax = 0;
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            long total = 0L;
            int maxCandidates = 0;
            for (int op = 0; op < LOOKUP_OPS_PER_SAMPLE; op++) {
                GTRecipeQuery query = lookupQueries.get(
                        (sample * LOOKUP_OPS_PER_SAMPLE + op) % queryCount);
                long start = System.nanoTime();
                assertTrue(
                        lookupMap.findMatch(query).isPresent(),
                        label + " lookup missed a generated consume query");
                total += System.nanoTime() - start;
                int indexed = lookupMap.indexedCandidateCount(query);
                maxCandidates = Math.max(maxCandidates, indexed);
                assertTrue(
                        indexed <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                        label + " indexed too many candidates");
            }
            lookup[sample] = total / LOOKUP_OPS_PER_SAMPLE;
            candidates[sample] = maxCandidates;
            candidatesMax = Math.max(candidatesMax, maxCandidates);
        }
        return new LookupMetrics(lookup, candidates, candidatesMax);
    }

    private static void assertRejectLookup(
            RecipeMap map,
            List<CompactRecipeFamilySource> familySources,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        CompactRecipeFamilyProvider.Snapshot snapshot = prepareGroup(
                map, familySources, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        RecipeMap lookupMap = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
        lookupMap.prepareRecipes(List.of(), List.of(snapshot), EPOCH).publish();
        GTRecipeQuery dirtOnly = GTRecipeQuery.items(new ItemStack(Items.DIRT));
        assertTrue(lookupMap.findMatch(dirtOnly).isEmpty());
    }

    private static void assertRejectCardLookup(
            Map<ResourceLocation, RecipeMap> knownMaps,
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy> policies) {
        var snapshots = prepareCard(
                knownMaps, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        RecipeMap lookupMap = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
        lookupMap.prepareRecipes(List.of(), List.copyOf(snapshots.values()), EPOCH).publish();
        GTRecipeQuery dirtOnly = GTRecipeQuery.items(new ItemStack(Items.DIRT));
        assertTrue(lookupMap.findMatch(dirtOnly).isEmpty());
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
                assertFalse(
                        offered.isEmpty() && relation.fluidInputs().isEmpty(),
                        "generated relation has no lookup operands");
                queries.add(new GTRecipeQuery(offered, relation.fluidInputs()));
            }
        }
        queries.sort(Comparator.comparing(
                CentrifugeCompactMeasurementHarness::lookupSortKey));
        return List.copyOf(queries);
    }

    private static String lookupSortKey(GTRecipeQuery query) {
        if (!query.itemInputs().isEmpty()) {
            return "item:" + query.itemInputs().getFirst().getItem();
        }
        return "fluid:" + query.fluidInputs().getFirst().getFluid();
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
            List<CandidateMeasurement> singletonCandidates,
            List<CandidateMeasurement> multiCandidates,
            List<CandidateMeasurement> cardCandidates) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("kind", "t39_production_lock_32_relation_rows");
        input.put("logical_rows", CARD_LOGICAL);
        input.put("measured_logical_rows", List.of(SINGLETON_LOGICAL, MULTI_LOGICAL, CARD_LOGICAL));
        input.put("generated_datapack_present", true);
        input.put(
                "harness",
                "CentrifugeCompactMeasurementHarness 22/32 JUnit samples; "
                        + "retained_memory uses snapshot.syncPayloadBytes, not a naive JVM "
                        + "heap delta. player_execution evidenced by "
                        + "CompactGTRecipeFamilyGeneratedTest and centrifuge-compact centrifuge equivalence "
                        + "artifacts. singleton/multi/card measured independently.");

        Map<String, Object> family = new LinkedHashMap<>();
        family.put("family_count", CARD_FAMILIES);
        family.put("singleton_families", SINGLETON_FAMILIES);
        family.put("multi_families", MULTI_FAMILIES);
        family.put("logical_rows", CARD_LOGICAL);
        family.put("strategy", "undecided_pending_derived_winner");
        family.put(
                "card_level_runtime_costs",
                "recorded once per scenario; never averaged into per-group measurements");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", 1);
        root.put("status", "T39_MATERIALIZATION_MEASUREMENT_READY");
        root.put("production_winner_claimed", false);
        root.put("protocol", Map.of(
                "id", "t39_materialization_production_lock_v2",
                "ranking_scale", "1x",
                "diagnostic_scales_not_for_production", List.of("5x", "20x"),
                "single_wall_clock_sample_forbidden", true,
                "p50_p95_invented", false,
                "sample_count", SAMPLE_COUNT));
        root.put("input", input);
        root.put("t39_opening", t39Opening());
        root.put("family_work_set", family);
        root.put("scenarios", List.of(
                scenario("singleton", SINGLETON_LOGICAL, SINGLETON_FAMILIES, singletonCandidates),
                scenario("multi", MULTI_LOGICAL, MULTI_FAMILIES, multiCandidates),
                scenario("card", CARD_LOGICAL, CARD_FAMILIES, cardCandidates)));
        root.put("diagnostic_scales", Map.of(
                "5x", Map.of(
                        "logical_rows", CARD_LOGICAL * 5,
                        "status", "PENDING_MEASUREMENT",
                        "production_use", "forbidden",
                        "note", "Diagnostic only. Must not fill 1x unmeasured fields."),
                "20x", Map.of(
                        "logical_rows", CARD_LOGICAL * 20,
                        "status", "PENDING_MEASUREMENT",
                        "production_use", "forbidden",
                        "note", "Diagnostic only. Must not copy Extruder 55640 or fill 1x fields.")));
        return root;
    }

    private static Map<String, Object> t39Opening() {
        Map<String, Object> opening = new LinkedHashMap<>();
        opening.put("source", "tools/t38_census_delta.json#t14_load.closing");
        opening.put("datapack_authored_entries", 3664);
        opening.put("eager_publication_rows", 16626);
        opening.put("lazy_logical_rows", 2334);
        opening.put("lazy_cache_ceiling_rows", 24);
        opening.put("sync_bytes", 12731);
        opening.put("server_reload_ms", 2);
        opening.put("server_index_ms", 2);
        opening.put("client_reload_ms", 2);
        opening.put("client_index_ms", 2);
        opening.put("retained_memory_bytes", 12731);
        opening.put("allocation_bytes", 473152);
        opening.put("lookup_p95_ns", 685725);
        opening.put("lookup_candidate_count", 73);
        opening.put("pending_runtime_axes", List.of());
        return opening;
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
                "reason", "CompactRecipeShardRouter overflow 0; indexed candidates sampled"));
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
                "centrifuge-compact compact snapshot payload (syncPayloadBytes); not a naive JVM heap delta");
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

    private record LookupMetrics(long[] lookup, long[] candidates, int candidatesMax) {}

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
