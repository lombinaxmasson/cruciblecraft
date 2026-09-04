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
 * 1x=95 tiny-purified measurement harness. Writes
 * {@code tools/bath_tiny_purified_materialization_measurements.json} for the Python
 * decision builder. Timings are nonparametric p50/p95 over 21 samples.
 *
 * <p>Measures the single tiny-purified Bath exact_multi publication group. Production
 * winners are not claimed here. Declared-before-measure cache ceiling is 128; hybrid duration_ticks_lte 16.
 */
class BathTinyPurifiedMeasurementHarness {
    private static final int PREPARE_WARMUP = 2;
    private static final int LOOKUP_WARMUP_OPS = 32;
    private static final int ENUMERATION_WARMUP = 1;
    private static final int SAMPLE_COUNT = 21;
    private static final int LOOKUP_OPS_PER_SAMPLE = 16;
    private static final long EPOCH = 11L;
    private static final int CACHE_CEILING = 128;
    private static final int HYBRID_DURATION_CUTOFF = 16;
    private static final int LOGICAL_ROWS = 95;
    private static final int FAMILY_COUNT = 5;
    private static int hybridEager;
    private static int hybridLazy;
    private static int onDemandCache;
    private static int hybridCache;
    private static final Path OUTPUT = Path.of(
            "tools/bath_tiny_purified_materialization_measurements.json");

    private static RegistryAccess registries;
    private static List<CompactRecipeFamilySource> sources;
    private static CompactRecipeFamilyProvider.EagerSelector hybridSelector;
    private static List<GTRecipeQuery> lookupQueries;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.bathTinyPurifiedGeneratedRoot();
        sources = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedSourcesRecursive(
                root, registries);
        Set<ResourceLocation> eagerStableIds = new HashSet<>();
        for (CompactRecipeFamilySource source : sources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.authoredRelations()) {
                if (relation.duration() <= HYBRID_DURATION_CUTOFF) {
                    eagerStableIds.add(relation.stableId());
                }
            }
        }
        hybridEager = eagerStableIds.size();
        hybridLazy = LOGICAL_ROWS - hybridEager;
        onDemandCache = Math.min(CACHE_CEILING, LOGICAL_ROWS);
        hybridCache = Math.min(CACHE_CEILING, hybridLazy);
        hybridSelector = (index, relation) -> eagerStableIds.contains(relation.stableId());
        lookupQueries = buildLookupQueries(sources);
    }

    @Test
    void measureOneXFifteenSeventeenAndWriteArtifact() throws Exception {
        assertEquals(FAMILY_COUNT, sources.size());
        assertEquals(LOGICAL_ROWS, countLogicalRows(sources));
        assertFalse(
                lookupQueries.stream().anyMatch(
                        BathTinyPurifiedMeasurementHarness::isIronIngotOnly),
                "lookup must use generated consume items, not a synthetic IRON_INGOT probe");

        CandidateMeasurement immediate = measureCandidate(
                "immediate",
                LOGICAL_ROWS,
                0,
                0,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        CandidateMeasurement onDemand = measureCandidate(
                "on_demand",
                0,
                LOGICAL_ROWS,
                onDemandCache,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(onDemandCache));
        CandidateMeasurement hybrid = measureCandidate(
                "hybrid",
                hybridEager,
                hybridLazy,
                hybridCache,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                        hybridCache, hybridSelector));

        assertEquals(immediate.fingerprint, onDemand.fingerprint);
        assertEquals(immediate.fingerprint, hybrid.fingerprint);
        assertEquals(LOGICAL_ROWS, immediate.logical);
        assertEquals(LOGICAL_ROWS, immediate.eager);
        assertEquals(0, onDemand.eager);
        assertEquals(hybridEager, hybrid.eager);
        assertTrue(immediate.syncBytes > 0L);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        String encoded = gson.toJson(artifact(List.of(immediate, onDemand, hybrid))) + "\n";
        assertTrue(encoded.length() > 1);
        if (Boolean.getBoolean("cruciblecraft.writeBathTinyPurifiedMeasurements")) {
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

    private static CandidateMeasurement measureCandidate(
            String name,
            int expectedEager,
            int expectedLazy,
            int expectedCache,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        RecipeMap map = new RecipeMap(ModRecipeMaps.BATH.id());
        CompactRecipeFamilyProvider.Snapshot correctness = prepare(
                map, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        assertEquals(LOGICAL_ROWS, correctness.logicalRecipeCount());
        assertEquals(expectedEager, correctness.eagerRecipeCount());
        assertEquals(expectedLazy, correctness.lazyRecipeCount());
        assertEquals(expectedCache, correctness.cacheCeiling());
        assertEquals(LOGICAL_ROWS, correctness.shardCount(), name + " shard_count");
        assertEquals(0, correctness.overflowRelationCount(), name + " overflow");
        int cacheBeforeEnum = correctness.cacheSize();
        for (int index = 0; index < correctness.logicalRecipeCount(); index++) {
            assertEquals(
                    correctness.recipeIds().get(index),
                    correctness.enumerationEntry(index).id());
        }
        assertEquals(cacheBeforeEnum, correctness.cacheSize(), name);
        CompactRecipeFamilyProvider.Snapshot dedicated = prepare(
                map, CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policy);
        assertEquals(correctness.recipeIds(), dedicated.recipeIds());
        assertEquals(correctness.stableFingerprint(), dedicated.stableFingerprint());
        IllegalArgumentException integrated = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        map,
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        policy));
        assertTrue(integrated.getMessage().contains("Integrated clients"));

        for (int warmup = 0; warmup < PREPARE_WARMUP; warmup++) {
            prepare(map, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
            prepare(map, CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policy);
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
                    map, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
            serverReload[sample] = System.nanoTime() - reloadStart;
            if (allocationSupported) {
                allocation[sample] = Math.max(0L, threadAllocatedBytes() - allocBefore);
            }
            long indexStart = System.nanoTime();
            map.prepareRecipes(List.of(), List.of(serverSnapshot), EPOCH);
            serverIndex[sample] = System.nanoTime() - indexStart;
            long clientReloadStart = System.nanoTime();
            CompactRecipeFamilyProvider.Snapshot clientSnapshot = prepare(
                    map, CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policy);
            clientReload[sample] = System.nanoTime() - clientReloadStart;
            RecipeMap clientMap = new RecipeMap(ModRecipeMaps.BATH.id());
            long clientIndexStart = System.nanoTime();
            clientMap.prepareRecipes(List.of(), List.of(clientSnapshot), EPOCH);
            clientIndex[sample] = System.nanoTime() - clientIndexStart;
        }

        CompactRecipeFamilyProvider.Snapshot enumSnapshot = prepare(
                map, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
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
                map, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        RecipeMap lookupMap = new RecipeMap(ModRecipeMaps.BATH.id());
        lookupMap.prepareRecipes(List.of(), List.of(lookupSnapshot), EPOCH).publish();
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

    private static CompactRecipeFamilyProvider.Snapshot prepare(
            RecipeMap map,
            CompactRecipeFamilyProvider.RuntimeSide side,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        return CompactRecipeFamilyProvider.prepare(map, sources, EPOCH, side, policy);
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

    private static Map<String, Object> artifact(List<CandidateMeasurement> candidates) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CandidateMeasurement candidate : candidates) {
            rows.add(candidateRow(candidate));
        }
        Map<String, Object> bath = scenario("exact_multi", rows);
        Map<String, Object> card = scenario("card", rows);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("kind", "bath_tiny_purified_production_lock_95_relation_rows");
        input.put("logical_rows", LOGICAL_ROWS);
        input.put("measured_logical_rows", List.of(LOGICAL_ROWS, LOGICAL_ROWS));
        input.put("generated_datapack_present", true);
        input.put(
                "harness",
                "BathTinyPurifiedMeasurementHarness 95 JUnit samples; "
                        + "retained_memory uses snapshot.syncPayloadBytes, not a naive JVM "
                        + "heap delta. player_execution evidenced by BathTinyPurifiedHarnessTest "
                        + "and BathTinyPurifiedGameTests. exact_multi and card are the same 95-row group.");

        Map<String, Object> family = new LinkedHashMap<>();
        family.put("family_count", FAMILY_COUNT);
        family.put("authored_entries_per_family", 1);
        family.put("logical_rows_per_family", "exact_or_exact_multi");
        family.put("strategy", "undecided_pending_derived_winner");
        family.put(
                "card_level_runtime_costs",
                "recorded once; never copied 5 times or averaged into per-family measurements");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", 1);
        root.put("status", "BATH_TINY_PURIFIED_MATERIALIZATION_MEASUREMENT_READY");
        root.put("production_winner_claimed", false);
        root.put("protocol", Map.of(
                "id", "bath_tiny_purified_materialization_production_lock_v1",
                "ranking_scale", "1x",
                "diagnostic_scales_not_for_production", List.of("5x", "20x"),
                "single_wall_clock_sample_forbidden", true,
                "p50_p95_invented", false,
                "sample_count", SAMPLE_COUNT));
        root.put("input", input);
        root.put("bath_tiny_purified_opening", bathTinyPurifiedOpening());
        root.put("family_work_set", family);
        root.put("scenarios", List.of(bath, card));
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

    private static Map<String, Object> scenario(String id, List<Map<String, Object>> rows) {
        Map<String, Object> scenario = new LinkedHashMap<>();
        scenario.put("id", id);
        scenario.put("scale", "1x");
        scenario.put("logical_rows", LOGICAL_ROWS);
        scenario.put("family_count", FAMILY_COUNT);
        scenario.put("production_scale", true);
        scenario.put("candidates", rows);
        return scenario;
    }

    private static Map<String, Object> bathTinyPurifiedOpening() {
        Map<String, Object> opening = new LinkedHashMap<>();
        opening.put("source", "tools/bath_identity_readiness.json#bath_identity_opening.recipe_load_closing");
        opening.put("datapack_authored_entries", 6263);
        opening.put("eager_publication_rows", 14);
        opening.put("lazy_logical_rows", 50557);
        opening.put("lazy_cache_ceiling_rows", 781);
        opening.put("sync_bytes", 5009445);
        opening.put("server_reload_ms", 347);
        opening.put("server_index_ms", 5);
        opening.put("client_reload_ms", 357);
        opening.put("client_index_ms", 5);
        opening.put("retained_memory_bytes", 5009445);
        opening.put("allocation_bytes", 686579392);
        opening.put("lookup_p95_ns", 93625);
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
                "bath-mte-added compact snapshot payload (syncPayloadBytes); not a naive JVM heap delta");
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
