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

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1x=73 roaster-compact measurement harness. Writes
 * {@code tools/roaster_compact_materialization_measurements.json} for the Python
 * decision builder. Timings are nonparametric p50/p95 over 21 samples.
 *
 * <p>Measures roaster-compact Roaster sources only. assembler-compact assembler policy remains loaded
 * elsewhere in the repo; multi-target cumulative cost is not sampled here.
 */
class RoasterCompactMeasurementHarness {
    private static final int PREPARE_WARMUP = 2;
    private static final int LOOKUP_WARMUP_OPS = 32;
    private static final int ENUMERATION_WARMUP = 1;
    private static final int SAMPLE_COUNT = 21;
    private static final int LOOKUP_OPS_PER_SAMPLE = 16;
    private static final long EPOCH = 11L;
    private static final int ON_DEMAND_CACHE = 16;
    private static final int HYBRID_DURATION_CUTOFF = 16;
    private static final int LOGICAL_ROWS = 73;
    private static final int FAMILY_COUNT = 29;
    private static final int HYBRID_EAGER = 38;
    private static final int HYBRID_LAZY = 35;
    private static final Path OUTPUT = Path.of(
            "tools/roaster_compact_materialization_measurements.json");

    private static RegistryAccess registries;
    private static List<CompactRecipeFamilySource> sources;
    private static CompactRecipeFamilyProvider.EagerSelector hybridSelector;
    private static List<GTRecipeQuery> lookupQueries;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.roasterGeneratedRoot();
        List<JsonObject> documents =
                CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(root);
        sources = documents.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(root, document, registries))
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
        assertEquals(HYBRID_EAGER, eagerStableIds.size());
        hybridSelector = (index, relation) -> eagerStableIds.contains(relation.stableId());
        lookupQueries = buildLookupQueries(sources);
    }

    @Test
    void measureOneXSeventyThreeAndWriteArtifact() throws Exception {
        assertEquals(FAMILY_COUNT, sources.size());
        assertEquals(LOGICAL_ROWS, countLogicalRows(sources));
        assertFalse(
                lookupQueries.stream().anyMatch(
                        RoasterCompactMeasurementHarness::isIronIngotOnly),
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
                ON_DEMAND_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(ON_DEMAND_CACHE));
        CandidateMeasurement hybrid = measureCandidate(
                "hybrid",
                HYBRID_EAGER,
                HYBRID_LAZY,
                ON_DEMAND_CACHE,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                        ON_DEMAND_CACHE, hybridSelector));

        assertEquals(immediate.fingerprint, onDemand.fingerprint);
        assertEquals(immediate.fingerprint, hybrid.fingerprint);
        assertEquals(LOGICAL_ROWS, immediate.logical);
        assertEquals(LOGICAL_ROWS, immediate.eager);
        assertEquals(0, onDemand.eager);
        assertEquals(HYBRID_EAGER, hybrid.eager);
        assertTrue(immediate.syncBytes > 0L);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        String encoded = gson.toJson(artifact(List.of(immediate, onDemand, hybrid))) + "\n";
        assertTrue(encoded.length() > 1);
        if (Boolean.getBoolean("cruciblecraft.writeRoasterCompactMeasurements")) {
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
        RecipeMap map = new RecipeMap(ModRecipeMaps.ROASTER.id());
        CompactRecipeFamilyProvider.Snapshot correctness = prepare(
                map, CompactRecipeFamilyProvider.RuntimeSide.SERVER, policy);
        assertEquals(LOGICAL_ROWS, correctness.logicalRecipeCount());
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
            RecipeMap clientMap = new RecipeMap(ModRecipeMaps.ROASTER.id());
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
        RecipeMap lookupMap = new RecipeMap(ModRecipeMaps.ROASTER.id());
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
        Map<String, Object> scenario = new LinkedHashMap<>();
        scenario.put("scale", "1x");
        scenario.put("logical_rows", LOGICAL_ROWS);
        scenario.put("production_scale", true);
        scenario.put("candidates", rows);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("kind", "roaster_73_relation_rows");
        input.put("logical_rows", LOGICAL_ROWS);
        input.put("measured_logical_rows", List.of(LOGICAL_ROWS));
        input.put("generated_datapack_present", true);
        input.put(
                "harness",
                "RoasterCompactMeasurementHarness 1x=73 JUnit samples; "
                        + "retained_memory uses snapshot.syncPayloadBytes (roaster-compact-added compact payload), "
                        + "not a naive JVM heap delta. player_execution evidenced by "
                        + "CompactGTRecipeFamilyGeneratedTest and roaster-compact roaster equivalence artifacts. "
                        + "assembler-compact assembler policy remains loaded in-repo but is not co-measured here.");

        Map<String, Object> family = new LinkedHashMap<>();
        family.put("family_count", FAMILY_COUNT);
        family.put("authored_entries_per_family", 1);
        family.put("logical_rows_per_family", "variable_1_to_38");
        family.put("strategy", "undecided_pending_derived_winner");
        family.put(
                "card_level_runtime_costs",
                "recorded once; never copied 73 times or averaged into per-family measurements");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", 1);
        root.put("status", "ROASTER_COMPACT_MATERIALIZATION_MEASUREMENT_READY");
        root.put("production_winner_claimed", false);
        root.put("protocol", Map.of(
                "id", "roaster_materialization_73_row_v1",
                "ranking_scale", "1x",
                "diagnostic_scales_not_for_production", List.of("5x", "20x"),
                "single_wall_clock_sample_forbidden", true,
                "p50_p95_invented", false,
                "sample_count", SAMPLE_COUNT));
        root.put("input", input);
        Map<String, Object> assemblerOpening = new LinkedHashMap<>();
        assemblerOpening.put("source", "tools/assembler_compact_census_delta.json#recipe_load.closing");
        assemblerOpening.put("datapack_authored_entries", 3616);
        assemblerOpening.put("eager_publication_rows", 16611);
        assemblerOpening.put("lazy_logical_rows", 2261);
        assemblerOpening.put("lazy_cache_ceiling_rows", 8);
        assemblerOpening.put("sync_bytes", 5150);
        assemblerOpening.put("server_reload_ms", 1);
        assemblerOpening.put("server_index_ms", 1);
        assemblerOpening.put("client_reload_ms", 1);
        assemblerOpening.put("client_index_ms", 1);
        assemblerOpening.put("retained_memory_bytes", 5150);
        assemblerOpening.put("allocation_bytes", 236168);
        assemblerOpening.put("lookup_p95_ns", 87368);
        assemblerOpening.put("lookup_candidate_count", 42);
        assemblerOpening.put("pending_runtime_axes", List.of());
        root.put("assembler_compact_opening", assemblerOpening);
        root.put("family_work_set", family);
        root.put("scenarios", List.of(scenario));
        root.put("diagnostic_scales", Map.of(
                "5x", Map.of(
                        "logical_rows", 365,
                        "status", "PENDING_MEASUREMENT",
                        "production_use", "forbidden",
                        "note", "Diagnostic only. Must not fill 1x unmeasured fields."),
                "20x", Map.of(
                        "logical_rows", 1460,
                        "status", "PENDING_MEASUREMENT",
                        "production_use", "forbidden",
                        "note", "Diagnostic only. Must not copy Extruder 20x=55640 or fill 1x unmeasured fields.")));
        return root;
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
                "roaster-compact-added compact snapshot payload (syncPayloadBytes); not a naive JVM heap delta");
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
