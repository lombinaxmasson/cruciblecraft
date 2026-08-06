package com.masson.cruciblecraft.benchmark.recipe;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRecipe;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRelation;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.LookupRequest;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ProviderDiagnostics;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.Publication;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.RecipeEnumeration;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.RuntimeSide;

/**
 * Isolated T14c benchmark orchestrator. It parses the actual T14a compact
 * relation artifact, compares candidates, and writes raw production evidence.
 */
public final class RecipeLoadBenchmarkHarness {
    private static final List<String> CANDIDATES =
            List.of("immediate", "on_demand", "hybrid");
    private static volatile long blackhole;

    private RecipeLoadBenchmarkHarness() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Expected raw output, policy, repository root and JFR directory");
        }
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        Path policyPath = Path.of(args[1]).toAbsolutePath().normalize();
        Path repositoryRoot = Path.of(args[2]).toAbsolutePath().normalize();
        Path jfrDirectory = Path.of(args[3]).toAbsolutePath().normalize();
        BenchmarkPolicy policy = BenchmarkPolicy.load(policyPath);
        Path compactPath = repositoryRoot.resolve(policy.compactPath)
                .toAbsolutePath().normalize();

        // Fail immediately if the dependency-free contract itself has drifted.
        RecipeFamilyProviderContractTest.main(new String[0]);

        List<Map<String, Object>> scenarios = new ArrayList<>();
        boolean hasSkips = false;
        for (BenchmarkPolicy.Scale scale : policy.scales) {
            System.out.println(
                    "T14c benchmark scale " + scale.id() + " ("
                            + scale.relationCount() + " relations)");
            ExtruderBenchmarkModel.LoadedRelations loaded =
                    ExtruderBenchmarkModel.load(
                    compactPath,
                    scale.relationCount(),
                    policy.hotModulo);
            if (!loaded.compactSha256().equals(policy.compactSha256)
                    || !loaded.compactFingerprint().equals(
                            policy.compactFingerprint)) {
                throw new IllegalArgumentException(
                        "T14a compact artifact does not match the pinned "
                                + "production measurement policy");
            }
            List<ExtruderRelation> relations = loaded.relations();
            List<Map<String, Object>> candidateRows = new ArrayList<>();
            for (String candidate : CANDIDATES) {
                System.out.println("  measuring " + candidate);
                Map<String, Object> row = measureCandidate(
                        policy,
                        repositoryRoot,
                        jfrDirectory,
                        scale,
                        relations,
                        candidate);
                candidateRows.add(row);
                hasSkips |= containsSkip(row);
            }
            Map<String, Object> scenario = new LinkedHashMap<>();
            scenario.put("scale", scale.id());
            scenario.put("purpose", scale.purpose());
            scenario.put("relation_count", scale.relationCount());
            scenario.put("actual_distribution_copies",
                    loaded.distributionCopies());
            scenario.put("candidates", candidateRows);
            scenarios.add(scenario);
        }

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schema_version", 2);
        document.put("protocol", Map.of(
                "id", "t14_materialization_lookup_cache_v2",
                "supersedes", "t14_materialization_publication_enumeration_v1",
                "supersession_reason",
                        "v1 used unbounded candidate caches and mixed "
                                + "enumeration allocation into lookup evidence"));
        document.put("status", hasSkips
                ? "T14C_PRODUCTION_MEASUREMENTS_WITH_SKIPS"
                : "T14C_PRODUCTION_MEASUREMENTS_COMPLETE");
        document.put("benchmark_only", false);
        document.put("production_winner_claimed", false);
        document.put("generated_at_utc", Instant.now().toString());
        document.put("harness", RecipeLoadBenchmarkHarness.class.getName());
        document.put("input", Map.of(
                "kind", policy.inputKind,
                "base_relation_count", policy.prototypeRelationCount,
                "t14a_compact_data_present", true,
                "compact_path", policy.compactPath,
                "compact_sha256", policy.compactSha256,
                "compact_fingerprint", policy.compactFingerprint,
                "distribution",
                        "complete ordered compact relation replay; lookup "
                                + "traces derive from actual lazyByItem groups"));
        document.put("policy", Map.of(
                "path", relative(repositoryRoot, policy.path),
                "sha256", policy.sha256,
                "schema_version", policy.schemaVersion));
        document.put("java_sources", sourceHashes(repositoryRoot));
        document.put("runtime", runtimeMetadata());
        document.put("method", methodMetadata(policy));
        document.put("contract", contractMetadata(policy));
        document.put("scenarios", scenarios);
        document.put("blackhole", blackhole);

        Files.createDirectories(output.getParent());
        Files.writeString(
                output,
                JsonEncoder.encode(document),
                StandardCharsets.UTF_8);
        System.out.println("Wrote T14b raw measurements to " + output);
    }

    private static Map<String, Object> measureCandidate(
            BenchmarkPolicy policy,
            Path repositoryRoot,
            Path jfrDirectory,
            BenchmarkPolicy.Scale scale,
            List<ExtruderRelation> relations,
            String candidate) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("candidate", candidate);
        row.put("gates", evaluateGates(candidate, relations));
        row.put("server", measureReload(
                policy, candidate, relations, RuntimeSide.SERVER));
        row.put("dedicated_client", measureReload(
                policy, candidate, relations, RuntimeSide.DEDICATED_CLIENT));
        Map<String, Object> integratedClient = new LinkedHashMap<>();
        integratedClient.put("status", "SKIP");
        integratedClient.put(
                "reason", "Integrated client shares the server publication");
        integratedClient.put("independent_reexpansion", false);
        integratedClient.put("reexpansion_ns", null);
        row.put("integrated_client", integratedClient);

        RecipeFamilyProvider live =
                RecipeFamilyProviderContractTest.newProvider(candidate);
        Publication publication =
                live.publish(relations, 800L, RuntimeSide.SERVER);
        row.put("lookup", measureLookup(policy, candidate, relations));
        row.put("enumeration", measureEnumeration(policy, live));
        row.put("sync", Map.of(
                "status", "PASS",
                "payload", candidate.equals("immediate")
                        ? "concrete_recipe_rows" : "compact_relation_rows",
                "bytes", live.syncPayloadBytes()));
        row.put("publication_shape", Map.of(
                "relations", publication.relationCount(),
                "eager_recipes", publication.eagerlyMaterializedRecipes(),
                "long_tail_relations", publication.relationCount()
                        - publication.eagerlyMaterializedRecipes(),
                "cache_policy", live.diagnostics().cachePolicy().name(),
                "cache_ceiling", live.diagnostics().cacheCeiling()));

        AtomicLong allocationEpoch = new AtomicLong(20_000L);
        int allocationOperations = policy.lookupSamples
                * policy.lookupOperationsPerSample;
        LookupWorkloads.Trace allocationTrace = LookupWorkloads.build(
                        relations, allocationOperations)
                .traces().getFirst();
        java.util.function.Supplier<Runnable> allocationWorkload = () -> {
            RecipeFamilyProvider provider =
                    RecipeFamilyProviderContractTest.newProvider(candidate);
            provider.publish(
                    relations,
                    allocationEpoch.incrementAndGet(),
                    RuntimeSide.SERVER);
            return () -> exerciseTrace(
                    provider, allocationTrace.operations(), 0,
                    allocationTrace.operations().size());
        };
        row.put("jfr_allocation", JfrAllocationProbe.measure(
                policy.jfrSamples,
                jfrDirectory,
                repositoryRoot,
                candidate,
                scale.id(),
                allocationWorkload));
        row.put("retained_memory", RetainedMemoryProbe.measure(
                policy, repositoryRoot, candidate, scale));
        return row;
    }

    private static Map<String, Object> measureReload(
            BenchmarkPolicy policy,
            String candidate,
            List<ExtruderRelation> relations,
            RuntimeSide side) {
        AtomicLong epoch = new AtomicLong(100L);
        for (int warmup = 0; warmup < policy.reloadWarmup; warmup++) {
            RecipeFamilyProvider provider =
                    RecipeFamilyProviderContractTest.newProvider(candidate);
            provider.publish(relations, epoch.incrementAndGet(), side);
            blackhole += provider.diagnostics().indexedInputKeys();
        }
        long[] expansion = new long[policy.reloadSamples];
        long[] index = new long[policy.reloadSamples];
        long[] reload = new long[policy.reloadSamples];
        int eagerRecipes = -1;
        String stableFingerprint = null;
        for (int sample = 0; sample < policy.reloadSamples; sample++) {
            RecipeFamilyProvider provider =
                    RecipeFamilyProviderContractTest.newProvider(candidate);
            Publication publication = provider.publish(
                    relations, epoch.incrementAndGet(), side);
            expansion[sample] = publication.expansionNanos();
            index[sample] = publication.indexNanos();
            reload[sample] = publication.reloadNanos();
            eagerRecipes = publication.eagerlyMaterializedRecipes();
            blackhole += provider.syncPayloadBytes();
            String sampleFingerprint =
                    stableViewFingerprint(provider.enumerationView());
            if (stableFingerprint != null
                    && !stableFingerprint.equals(sampleFingerprint)) {
                throw new IllegalStateException(
                        "Stable enumeration fingerprint changed between samples");
            }
            stableFingerprint = sampleFingerprint;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PASS");
        result.put("side", side.name());
        result.put("relation_count", relations.size());
        result.put("eager_recipes", eagerRecipes);
        result.put("stable_fingerprint", stableFingerprint);
        result.put("expansion", BenchmarkStatistics.timing(
                expansion, policy.reloadWarmup));
        result.put("index", BenchmarkStatistics.timing(
                index, policy.reloadWarmup));
        result.put("reload", BenchmarkStatistics.timing(
                reload, policy.reloadWarmup));
        return result;
    }

    private static Map<String, Object> measureLookup(
            BenchmarkPolicy policy,
            String candidate,
            List<ExtruderRelation> relations) {
        int measuredOperations = policy.lookupSamples
                * policy.lookupOperationsPerSample;
        int operationsPerTrace = policy.lookupWarmupOperations
                + measuredOperations;
        LookupWorkloads.Workload workload = LookupWorkloads.build(
                relations, operationsPerTrace);
        List<Map<String, Object>> traces = new ArrayList<>();
        Map<String, Object> hardLimitTrace = null;
        long hardLimitP95 = Long.MIN_VALUE;
        long epoch = 900L;
        for (LookupWorkloads.Trace trace : workload.traces()) {
            RecipeFamilyProvider provider =
                    RecipeFamilyProviderContractTest.newProvider(candidate);
            provider.publish(relations, epoch++, RuntimeSide.SERVER);
            exerciseTrace(
                    provider,
                    trace.operations(),
                    0,
                    policy.lookupWarmupOperations);
            ProviderDiagnostics before = provider.diagnostics();
            long[] samples = new long[policy.lookupSamples];
            int[] candidateCounts = new int[measuredOperations];
            int countIndex = 0;
            for (int sample = 0; sample < policy.lookupSamples; sample++) {
                long started = System.nanoTime();
                for (int operation = 0;
                        operation < policy.lookupOperationsPerSample;
                        operation++) {
                    int traceIndex = policy.lookupWarmupOperations
                            + sample * policy.lookupOperationsPerSample
                            + operation;
                    ExtruderRelation relation =
                            trace.operations().get(traceIndex);
                    LookupRequest request =
                            ExtruderBenchmarkModel.request(relation);
                    candidateCounts[countIndex++] =
                            provider.indexedCandidateCount(request);
                    ExtruderRecipe recipe =
                            provider.lookup(request).orElseThrow();
                    blackhole += recipe.stableId().hashCode();
                }
                samples[sample] = (System.nanoTime() - started)
                        / policy.lookupOperationsPerSample;
            }
            ProviderDiagnostics after = provider.diagnostics();
            Map<String, Object> traceResult = new LinkedHashMap<>(
                    BenchmarkStatistics.timing(
                            samples, policy.lookupWarmupOperations));
            traceResult.put("trace", trace.id());
            traceResult.put("description", trace.description());
            traceResult.put("operations", measuredOperations);
            traceResult.put("operations_per_sample",
                    policy.lookupOperationsPerSample);
            traceResult.put(
                    "candidates",
                    BenchmarkStatistics.counts(candidateCounts));
            traceResult.put("cache", cacheDelta(before, after));
            traceResult.put("indexed_relation_lookup", true);
            traces.add(traceResult);
            long p95 = (Long) traceResult.get("p95_ns");
            if (p95 > hardLimitP95) {
                hardLimitP95 = p95;
                hardLimitTrace = traceResult;
            }
        }
        if (hardLimitTrace == null) {
            throw new IllegalStateException("No lookup trace was measured");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (String field : List.of(
                "status",
                "warmup_iterations",
                "samples",
                "raw_samples_ns",
                "p50_ns",
                "p95_ns",
                "max_ns",
                "error_bar",
                "p95_error_bar")) {
            result.put(field, hardLimitTrace.get(field));
        }
        result.put("hard_limit_aggregation", "maximum trace p95");
        result.put("hard_limit_trace", hardLimitTrace.get("trace"));
        result.put("operations_per_sample",
                policy.lookupOperationsPerSample);
        result.put("actual_lazy_by_item", Map.of(
                "lazy_relations", workload.lazyRelations(),
                "input_groups", workload.lazyInputGroups(),
                "max_candidates_per_input",
                        workload.maxCandidatesPerInput()));
        result.put("traces", traces);
        result.put("indexed_relation_lookup", true);
        return result;
    }

    private static Map<String, Object> cacheDelta(
            ProviderDiagnostics before,
            ProviderDiagnostics after) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policy", after.cachePolicy().name());
        result.put("ceiling", after.cacheCeiling());
        result.put("size_before", before.cachedRecipes());
        result.put("size_after", after.cachedRecipes());
        result.put("hits", after.cacheHits() - before.cacheHits());
        result.put("misses", after.cacheMisses() - before.cacheMisses());
        result.put("materializations",
                after.materializations() - before.materializations());
        result.put("evictions", after.evictions() - before.evictions());
        return result;
    }

    private static Map<String, Object> measureEnumeration(
            BenchmarkPolicy policy,
            RecipeFamilyProvider provider) {
        for (int warmup = 0;
                warmup < policy.enumerationWarmup;
                warmup++) {
            blackhole += enumerateChecksum(provider.enumerationView());
        }
        long[] samples = new long[policy.enumerationSamples];
        long checksum = 0L;
        for (int sample = 0;
                sample < policy.enumerationSamples;
                sample++) {
            long started = System.nanoTime();
            checksum += enumerateChecksum(provider.enumerationView());
            samples[sample] = System.nanoTime() - started;
        }
        blackhole += checksum;
        Map<String, Object> result = new LinkedHashMap<>(
                BenchmarkStatistics.timing(
                        samples, policy.enumerationWarmup));
        result.put("recipes_per_enumeration",
                provider.enumerationView().size());
        result.put("complete_view", true);
        result.put("checksum", checksum);
        return result;
    }

    private static Map<String, Object> evaluateGates(
            String candidate,
            List<ExtruderRelation> relations) {
        Map<String, Object> gates = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        boolean correctness = false;
        boolean epochInvalidation = false;
        boolean dedicatedEnumeration = false;
        boolean integratedSkip = false;
        try {
            RecipeFamilyProvider server =
                    RecipeFamilyProviderContractTest.newProvider(candidate);
            server.publish(relations, 1L, RuntimeSide.SERVER);
            correctness = verifyCompleteStableView(server, relations);
            RecipeEnumeration stale = server.enumerationView();
            ExtruderRecipe before = server.lookup(
                    ExtruderBenchmarkModel.request(relations.get(
                            Math.min(7, relations.size() - 1)))).orElseThrow();
            server.publish(relations, 2L, RuntimeSide.SERVER);
            boolean staleRejected = false;
            try {
                stale.size();
            } catch (IllegalStateException expected) {
                staleRejected = true;
            }
            ExtruderRecipe after = server.lookup(
                    ExtruderBenchmarkModel.request(relations.get(
                            Math.min(7, relations.size() - 1)))).orElseThrow();
            epochInvalidation = staleRejected
                    && server.diagnostics().cacheEpoch() == 2L
                    && before.stableId().equals(after.stableId());
        } catch (Throwable failure) {
            errors.add("server/epoch: " + failure);
        }
        try {
            RecipeFamilyProvider client =
                    RecipeFamilyProviderContractTest.newProvider(candidate);
            client.publish(
                    relations, 1L, RuntimeSide.DEDICATED_CLIENT);
            dedicatedEnumeration = verifyCompleteStableView(client, relations);
        } catch (Throwable failure) {
            errors.add("dedicated client: " + failure);
        }
        try {
            RecipeFamilyProvider integrated =
                    RecipeFamilyProviderContractTest.newProvider(candidate);
            boolean rejected = false;
            try {
                integrated.publish(
                        relations, 1L, RuntimeSide.INTEGRATED_CLIENT);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            integratedSkip = rejected
                    && !RuntimeSide.INTEGRATED_CLIENT
                            .requiresIndependentExpansion();
        } catch (Throwable failure) {
            errors.add("integrated client: " + failure);
        }
        gates.put("correctness", correctness);
        gates.put("epoch_invalidation", epochInvalidation);
        gates.put("dedicated_client_enumeration", dedicatedEnumeration);
        gates.put("integrated_client_skip", integratedSkip);
        gates.put("all_pass", correctness
                && epochInvalidation
                && dedicatedEnumeration
                && integratedSkip);
        gates.put("errors", errors);
        return gates;
    }

    private static boolean verifyCompleteStableView(
            RecipeFamilyProvider provider,
            List<ExtruderRelation> relations) {
        RecipeEnumeration view = provider.enumerationView();
        if (view.size() != relations.size()) {
            return false;
        }
        for (int index = 0; index < relations.size(); index++) {
            ExtruderRelation relation = relations.get(index);
            ExtruderRecipe recipe = view.get(index);
            if (!recipe.stableId().equals(relation.relationId())
                    || !provider.lookup(
                            ExtruderBenchmarkModel.request(relation))
                            .map(ExtruderRecipe::stableId)
                            .filter(recipe.stableId()::equals)
                            .isPresent()) {
                return false;
            }
        }
        return true;
    }

    private static void exerciseTrace(
            RecipeFamilyProvider provider,
            List<ExtruderRelation> trace,
            int start,
            int count) {
        for (int operation = start; operation < start + count; operation++) {
            ExtruderRelation relation = trace.get(operation);
            blackhole += provider.lookup(
                    ExtruderBenchmarkModel.request(relation))
                    .orElseThrow()
                    .stableId()
                    .hashCode();
        }
    }

    private static long enumerateChecksum(RecipeEnumeration view) {
        long result = view.size();
        for (ExtruderRecipe recipe : view) {
            result += recipe.stableId().hashCode();
        }
        return result;
    }

    private static String stableViewFingerprint(RecipeEnumeration view) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (ExtruderRecipe recipe : view) {
                digest.update(recipe.stableId().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static Map<String, Object> runtimeMetadata() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("java_version", System.getProperty("java.version"));
        result.put("java_vendor", System.getProperty("java.vendor"));
        result.put("java_vm", System.getProperty("java.vm.name"));
        result.put("java_vm_version", System.getProperty("java.vm.version"));
        result.put("jvm_arguments", ManagementFactory.getRuntimeMXBean()
                .getInputArguments());
        result.put("gc_names", ManagementFactory.getGarbageCollectorMXBeans()
                .stream().map(bean -> bean.getName()).sorted().toList());
        result.put("heap_max_bytes", Runtime.getRuntime().maxMemory());
        result.put("loaded_classes",
                ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
        result.put("os_name", System.getProperty("os.name"));
        result.put("os_version", System.getProperty("os.version"));
        result.put("os_arch", System.getProperty("os.arch"));
        result.put("processors", Runtime.getRuntime().availableProcessors());
        return result;
    }

    private static Map<String, Object> methodMetadata(BenchmarkPolicy policy) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clock", "System.nanoTime");
        result.put("reload", Map.of(
                "warmup_iterations", policy.reloadWarmup,
                "samples", policy.reloadSamples));
        result.put("lookup", Map.of(
                "warmup_operations", policy.lookupWarmupOperations,
                "samples", policy.lookupSamples,
                "operations_per_sample", policy.lookupOperationsPerSample,
                "trace_source", "actual compact lazyByItem groups",
                "traces", LookupWorkloads.TRACE_IDS,
                "hard_limit_aggregation", "maximum trace p95"));
        result.put("enumeration", Map.of(
                "warmup_iterations", policy.enumerationWarmup,
                "samples", policy.enumerationSamples));
        result.put("retained_memory", Map.of(
                "child_samples", policy.retainedSamples,
                "child_heap", policy.retainedHeap,
                "lookup_warmup_operations",
                        policy.retainedLookupWarmup,
                "controlled_gc", "jcmd GC.run",
                "measurement", "jcmd GC.class_histogram",
                "retained_scope",
                        "published snapshot plus production lookup cache; "
                                + "enumeration never executed",
                "naive_heap_delta", false));
        result.put("allocation", Map.of(
                "samples", policy.jfrSamples,
                "source", "JDK Flight Recorder allocation events",
                "scope", "lookup-only after publication; enumeration excluded"));
        result.put("timing_error_bar",
                "nonparametric 95% median order statistic");
        return result;
    }

    private static Map<String, Object> contractMetadata(
            BenchmarkPolicy policy) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("interface", RecipeFamilyProvider.class.getName());
        result.put("stable_id_prefix",
                "T14a compact stable_id");
        result.put("input", "parsed tools/t14_extruder_compact.json");
        result.put("on_demand", Map.of(
                "stable_ids", true,
                "indexed_relation_lookup", true,
                "complete_enumeration_view", true,
                "epoch_cache_invalidation", true,
                "cache_policy", "NO_CACHE",
                "cache_ceiling", 0));
        result.put("hybrid_boundary", Map.of(
                "rule", "relation ordinal modulo hot_modulo equals zero",
                "hot_modulo", policy.hotModulo,
                "eager", "eligible relations",
                "long_tail", "all non-eligible relations",
                "cache_policy", "synchronized access-order LinkedHashMap LRU",
                "cache_ceiling", HybridExtruderProvider.CACHE_CEILING));
        result.put("lookup_traces", Map.of(
                "source", "actual compact lazyByItem groups",
                "ids", LookupWorkloads.TRACE_IDS));
        result.put("client_gate", Map.of(
                "dedicated_client", "independent relation re-expansion",
                "integrated_client", "server publication shared; skip"));
        result.put("production_integration", true);
        return result;
    }

    private static Map<String, Object> sourceHashes(Path repositoryRoot)
            throws IOException {
        Path root = repositoryRoot.resolve("src/t14Benchmark/java");
        Map<String, String> files = new java.util.TreeMap<>();
        try (var paths = Files.walk(root)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> files.put(
                            relative(repositoryRoot, path),
                            BenchmarkPolicy.sha256(path)));
        }
        if (files.isEmpty()) {
            throw new IllegalStateException(
                    "T14b benchmark source digest is empty");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithm", "sha256");
        result.put("files", files);
        return result;
    }

    private static boolean containsSkip(Object value) {
        if (value instanceof Map<?, ?> map) {
            if ("SKIP".equals(map.get("status"))
                    && map.containsKey("samples")) {
                return true;
            }
            return map.values().stream()
                    .anyMatch(RecipeLoadBenchmarkHarness::containsSkip);
        }
        if (value instanceof Iterable<?> values) {
            for (Object item : values) {
                if (containsSkip(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String relative(Path root, Path path) {
        return root.toAbsolutePath().normalize()
                .relativize(path.toAbsolutePath().normalize())
                .toString().replace('\\', '/');
    }

}
