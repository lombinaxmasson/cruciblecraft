package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Closeout-integrity integrated mixes. Historical compact groups plus
 * Smelter ordinary-closure, and that mix plus Mixer ordinary-closure.
 *
 * <p>Adds the two tiny-purified gaps: lookup-only ThreadMXBean allocation and
 * controlled-GC class-histogram retained memory. Does not copy opening
 * 687226880 or {@code syncPayloadBytes} as retained.
 */
class OrdinaryCloseoutIntegratedMeasurementHarness {
    private static final int PREPARE_WARMUP = 1;
    private static final int LOOKUP_OPS_PER_SAMPLE = 20;
    private static final int MAX_LOOKUP_QUERIES = 1024;
    private static final long EPOCH = 11L;
    private static final Path V2_MANIFEST = Path.of(
            "tools/compact_recipe_runtime_manifest.v2.json");
    private static final List<String> MIX_HOSTS =
            CompactGTRecipeFamilyGeneratedSupport.ordinaryClosureMixHosts();
    private static final Path SMELTER_OUTPUT = Path.of(
            "tools/waves/ordinary-wave/closeout-integrity-repair/"
                    + "smelter_integrated_measurements.json");
    private static final Path MIXER_OUTPUT = Path.of(
            "tools/waves/ordinary-wave/closeout-integrity-repair/"
                    + "mixer_integrated_measurements.json");
    private static final Pattern HISTOGRAM_ROW = Pattern.compile(
            "(?m)^\\s*\\d+:\\s+(\\d+)\\s+(\\d+)\\s+(.+?)\\s*$");
    private static final Pattern HISTOGRAM_TOTAL = Pattern.compile(
            "(?m)^\\s*Total\\s+(\\d+)\\s+(\\d+)\\s*$");

    private static RegistryAccess registries;
    private static List<CompactRecipeFamilySource> sources;
    private static JsonArray groups;
    private static Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>
            productionPolicies;
    private static Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>
            immediatePolicies;
    private static Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>
            onDemandPolicies;
    private static Map<ResourceLocation, RecipeMap> knownMaps;
    private static List<GTRecipeQuery> lookupQueries;
    private static String selectedMix;

    @BeforeAll
    static void bootstrapMinecraft() throws Exception {
        Assumptions.assumeTrue(
                Boolean.getBoolean("cruciblecraft.writeOrdinaryCloseoutMeasurements")
                        || Boolean.getBoolean("cruciblecraft.runOrdinaryCloseoutMeasurements"),
                "ordinary closeout integrated measurements stay on the dedicated Gradle task");
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        selectedMix = System.getProperty("cruciblecraft.ordinaryCloseoutMix", "smelter")
                .trim()
                .toLowerCase(Locale.ROOT);
        boolean deferredRecycling = "deferred_recycling".equals(selectedMix);
        Assumptions.assumeTrue(
                MIX_HOSTS.contains(selectedMix) || deferredRecycling,
                () -> "unknown ordinary closeout mix " + selectedMix);
        List<Path> roots = deferredRecycling
                ? CompactGTRecipeFamilyGeneratedSupport.compactPlusDeferredRecyclingRoots()
                : CompactGTRecipeFamilyGeneratedSupport
                        .compactPlusOrdinaryRootsThrough(selectedMix);
        Assumptions.assumeTrue(
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamiliesRecursive(roots),
                () -> "ordinary-closure generated families are not available at " + roots);
        sources = CompactGTRecipeFamilyGeneratedSupport
                .loadGeneratedSourcesRecursive(roots, registries);
        groups = composeGroups(deferredRecycling ? "deferred_recycling" : selectedMix);
        if (deferredRecycling) {
            assertTrue(
                    groups.size() > 40,
                    () -> "deferred-recycling mix must exceed Mixer 28 groups but saw "
                            + groups.size());
        } else if ("smelter".equals(selectedMix) || "mixer".equals(selectedMix)) {
            int expected = "mixer".equals(selectedMix) ? 28 : 25;
            assertTrue(
                    groups.size() == expected,
                    () -> selectedMix + " mix expected " + expected
                            + " groups (19 compact + 6 Smelter"
                            + ("mixer".equals(selectedMix) ? " + 3 Mixer" : "")
                            + ") but saw " + groups.size());
        } else {
            assertTrue(
                    groups.size() > 28,
                    () -> selectedMix + " mix must exceed Mixer 28 groups but saw "
                            + groups.size());
        }
        productionPolicies = new LinkedHashMap<>();
        immediatePolicies = new LinkedHashMap<>();
        onDemandPolicies = new LinkedHashMap<>();
        knownMaps = new HashMap<>();
        for (int index = 0; index < groups.size(); index++) {
            JsonObject group = groups.get(index).getAsJsonObject();
            ResourceLocation mapId = ResourceLocation.parse(group.get("target_map").getAsString());
            ResourceLocation publicationGroup = ResourceLocation.parse(
                    group.get("publication_group").getAsString());
            PublicationGroupKey key = new PublicationGroupKey(mapId, publicationGroup);
            int cache = group.get("cache_ceiling").getAsInt();
            Set<ResourceLocation> eager = new HashSet<>();
            JsonArray eagerIds = group.getAsJsonArray("eager_stable_ids");
            if (eagerIds != null) {
                eagerIds.forEach(element ->
                        eager.add(ResourceLocation.parse(remapStableId(element.getAsString()))));
            }
            String policyType = group.get("policy_type").getAsString();
            CompactRecipeFamilyProvider.MaterializationPolicy production =
                    switch (policyType) {
                        case "immediate" ->
                                CompactRecipeFamilyProvider.MaterializationPolicy.immediate();
                        case "on_demand" ->
                                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(cache);
                        case "hybrid" ->
                                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                        cache,
                                        (ignored, relation) -> eager.contains(relation.stableId()));
                        default -> throw new IllegalStateException(policyType);
                    };
            productionPolicies.put(key, production);
            immediatePolicies.put(
                    key, CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
            onDemandPolicies.put(
                    key, CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(cache));
            knownMaps.computeIfAbsent(mapId, RecipeMap::new);
        }
        for (CompactRecipeFamilySource source : sources) {
            PublicationGroupKey key = new PublicationGroupKey(
                    source.definition().targetMap(),
                    source.definition().resolvedPublicationGroup());
            Assumptions.assumeTrue(
                    productionPolicies.containsKey(key),
                    () -> "generated publication group missing from composed mix: "
                            + key.publicationGroup());
        }
        lookupQueries = buildLookupQueries(sources);
    }

    @Test
    void measureSelectedOrdinaryCloseoutMixAndWriteArtifact() throws Exception {
        CandidateMeasurement immediate = measureCandidate("immediate", immediatePolicies);
        CandidateMeasurement onDemand = measureCandidate("on_demand", onDemandPolicies);
        CandidateMeasurement hybrid = measureCandidate("hybrid", productionPolicies);
        assertTrue(hybrid.logical > 50_666, "integrated mix must exceed the 19-group compact row count");
        assertTrue(hybrid.candidatesP95() > 0, "integrated hybrid lookup produced no candidates");
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String encoded = gson.toJson(artifact(List.of(immediate, onDemand, hybrid))) + "\n";
        if (Boolean.getBoolean("cruciblecraft.writeOrdinaryCloseoutMeasurements")) {
            Path output = outputPath(selectedMix);
            Files.createDirectories(output.getParent());
            Files.writeString(output, encoded, StandardCharsets.UTF_8);
        }
        assertTrue(encoded.contains("\"lookup_allocation\""));
        assertTrue(encoded.contains("\"controlled_gc_histogram\""));
        assertFalse(encoded.contains("\"sync_payload_proxy\""));
    }

    private static CandidateMeasurement measureCandidate(
            String name,
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>
                    policies) {
        Map<PublicationGroupKey, CompactRecipeFamilyProvider.Snapshot> correctness =
                prepare(CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        int logical = 0;
        int eager = 0;
        int lazy = 0;
        int cache = 0;
        long sync = 0L;
        for (CompactRecipeFamilyProvider.Snapshot snapshot : correctness.values()) {
            logical += snapshot.logicalRecipeCount();
            eager += snapshot.eagerRecipeCount();
            lazy += snapshot.lazyRecipeCount();
            cache += snapshot.cacheCeiling();
            sync += snapshot.syncPayloadBytes();
        }
        IllegalArgumentException integrated = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        policies));
        assertTrue(integrated.getMessage().contains("Integrated clients"));
        for (int warmup = 0; warmup < PREPARE_WARMUP; warmup++) {
            prepare(CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
            prepare(CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policies);
        }
        long[] serverReload = new long[sampleCount()];
        long[] serverIndex = new long[sampleCount()];
        long[] clientReload = new long[sampleCount()];
        long[] clientIndex = new long[sampleCount()];
        long[] allocation = new long[sampleCount()];
        boolean allocationSupported = threadAllocatedBytesSupported();
        for (int sample = 0; sample < sampleCount(); sample++) {
            long allocBefore = allocationSupported ? threadAllocatedBytes() : 0L;
            long reloadStart = System.nanoTime();
            var serverSnapshots = prepare(
                    CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
            serverReload[sample] = System.nanoTime() - reloadStart;
            if (allocationSupported) {
                allocation[sample] = Math.max(0L, threadAllocatedBytes() - allocBefore);
            }
            Map<ResourceLocation, RecipeMap> maps = freshMaps();
            long indexStart = System.nanoTime();
            publish(maps, serverSnapshots);
            serverIndex[sample] = System.nanoTime() - indexStart;
            long clientReloadStart = System.nanoTime();
            var clientSnapshots = prepare(
                    CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT, policies);
            clientReload[sample] = System.nanoTime() - clientReloadStart;
            Map<ResourceLocation, RecipeMap> clientMaps = freshMaps();
            long clientIndexStart = System.nanoTime();
            publish(clientMaps, clientSnapshots);
            clientIndex[sample] = System.nanoTime() - clientIndexStart;
        }
        var lookupSnapshots = prepare(
                CompactRecipeFamilyProvider.RuntimeSide.SERVER, policies);
        Map<ResourceLocation, RecipeMap> lookupMaps = freshMaps();
        publish(lookupMaps, lookupSnapshots);
        long[] lookup = new long[sampleCount()];
        long[] candidates = new long[sampleCount()];
        long[] lookupAlloc = new long[sampleCount()];
        int queryCount = lookupQueries.size();
        for (int sample = 0; sample < sampleCount(); sample++) {
            long total = 0L;
            int maxCandidates = 0;
            long allocBefore = allocationSupported ? threadAllocatedBytes() : 0L;
            for (int op = 0; op < LOOKUP_OPS_PER_SAMPLE; op++) {
                GTRecipeQuery query = lookupQueries.get(
                        (sample * LOOKUP_OPS_PER_SAMPLE + op) % queryCount);
                long start = System.nanoTime();
                boolean hit = false;
                int indexed = 0;
                for (RecipeMap map : lookupMaps.values()) {
                    if (map.findMatch(query).isPresent()) {
                        hit = true;
                    }
                    indexed = Math.max(indexed, map.indexedCandidateCount(query));
                }
                assertTrue(hit, name + " integrated lookup missed a generated query");
                total += System.nanoTime() - start;
                maxCandidates = Math.max(maxCandidates, indexed);
            }
            lookup[sample] = total / LOOKUP_OPS_PER_SAMPLE;
            candidates[sample] = maxCandidates;
            if (allocationSupported) {
                long allocated = Math.max(0L, threadAllocatedBytes() - allocBefore);
                lookupAlloc[sample] = allocated / LOOKUP_OPS_PER_SAMPLE;
            }
        }
        RetainedSample retained = measureRetainedAfterControlledGc();
        return new CandidateMeasurement(
                name, eager, lazy, cache, logical, sync,
                percentile(serverReload, 50), percentile(serverReload, 95),
                percentile(serverIndex, 50), percentile(serverIndex, 95),
                percentile(clientReload, 50), percentile(clientReload, 95),
                percentile(clientIndex, 50), percentile(clientIndex, 95),
                percentile(lookup, 50), percentile(lookup, 95),
                (int) percentile(candidates, 95),
                allocationSupported,
                allocationSupported ? percentile(allocation, 50) : null,
                allocationSupported ? percentile(allocation, 95) : null,
                allocationSupported ? percentile(lookupAlloc, 50) : null,
                allocationSupported ? percentile(lookupAlloc, 95) : null,
                retained);
    }

    private static Map<PublicationGroupKey, CompactRecipeFamilyProvider.Snapshot> prepare(
            CompactRecipeFamilyProvider.RuntimeSide side,
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>
                    policies) {
        return CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources, knownMaps, EPOCH, side, policies);
    }

    private static Map<ResourceLocation, RecipeMap> freshMaps() {
        Map<ResourceLocation, RecipeMap> maps = new HashMap<>();
        knownMaps.keySet().forEach(id -> maps.put(id, new RecipeMap(id)));
        return maps;
    }

    private static void publish(
            Map<ResourceLocation, RecipeMap> maps,
            Map<PublicationGroupKey, CompactRecipeFamilyProvider.Snapshot> snapshots) {
        Map<ResourceLocation, List<CompactRecipeFamilyProvider.Snapshot>> byMap =
                new HashMap<>();
        snapshots.forEach((key, snapshot) ->
                byMap.computeIfAbsent(key.targetMap(), ignored -> new ArrayList<>())
                        .add(snapshot));
        byMap.forEach((mapId, groupSnapshots) ->
                maps.get(mapId).prepareRecipes(
                        List.of(),
                        List.copyOf(groupSnapshots),
                        EPOCH).publish());
    }

    private static List<GTRecipeQuery> buildLookupQueries(
            List<CompactRecipeFamilySource> familySources) {
        List<GTRecipeQuery> collected = new ArrayList<>();
        for (CompactRecipeFamilySource source : familySources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.authoredRelations()) {
                List<ItemStack> offered = new ArrayList<>();
                var inputs = relation.itemInputs();
                var counts = relation.itemInputCounts();
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
                if (!offered.isEmpty()) {
                    collected.add(new GTRecipeQuery(offered, relation.fluidInputs()));
                }
            }
        }
        assertFalse(collected.isEmpty());
        if (collected.size() <= MAX_LOOKUP_QUERIES) {
            return List.copyOf(collected);
        }
        List<GTRecipeQuery> sampled = new ArrayList<>();
        double stride = collected.size() / (double) MAX_LOOKUP_QUERIES;
        for (int index = 0; index < MAX_LOOKUP_QUERIES; index++) {
            sampled.add(collected.get(Math.min(collected.size() - 1, (int) (index * stride))));
        }
        return List.copyOf(sampled);
    }

    private static int sampleCount() {
        return Boolean.getBoolean("cruciblecraft.writeOrdinaryCloseoutMeasurements") ? 21 : 3;
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
        return ((com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean())
                .getThreadAllocatedBytes(Thread.currentThread().threadId());
    }

    private static RetainedSample measureRetainedAfterControlledGc() {
        try {
            System.gc();
            sleepQuietly(200L);
            System.gc();
            String histogram = invokeClassHistogram();
            Matcher total = HISTOGRAM_TOTAL.matcher(histogram);
            if (!total.find()) {
                throw new IllegalStateException("class histogram omitted its Total row");
            }
            long totalBytes = Long.parseLong(total.group(2));
            long recipeBytes = 0L;
            Matcher row = HISTOGRAM_ROW.matcher(histogram);
            while (row.find()) {
                if (isRecipeRetainedClass(row.group(3))) {
                    recipeBytes += Long.parseLong(row.group(2));
                }
            }
            long retained = recipeBytes > 0L ? recipeBytes : totalBytes;
            return new RetainedSample(
                    retained,
                    "controlled_gc_histogram",
                    "DiagnosticCommand.gcClassHistogram",
                    totalBytes,
                    recipeBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("controlled-GC histogram failed", exception);
        }
    }

    private static String invokeClassHistogram() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("com.sun.management:type=DiagnosticCommand");
        Object result = server.invoke(
                name,
                "gcClassHistogram",
                new Object[] {new String[0]},
                new String[] {String[].class.getName()});
        if (result instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new IllegalStateException("DiagnosticCommand.gcClassHistogram returned no text");
    }

    private static boolean isRecipeRetainedClass(String className) {
        String name = className == null ? "" : className;
        return name.contains("com.masson.cruciblecraft.recipe")
                || name.contains("CompactRecipe")
                || name.contains("GTRecipe")
                || name.contains("RecipeMap");
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static String remapPublicationGroup(String group) {
        return SemanticIdMap.remapPublicationGroup(group);
    }

    private static String remapStableId(String stableId) {
        return SemanticIdMap.remapStableId(stableId);
    }

    private static Path outputPath(String mix) {
        return switch (mix) {
            case "smelter" -> SMELTER_OUTPUT;
            case "mixer" -> MIXER_OUTPUT;
            case "deferred_recycling" -> Path.of(
                    "tools/waves/smelter/deferred-recycling/"
                            + "smelter_deferred_recycling_integrated_measurements.json");
            default -> Path.of(
                    "tools/waves/" + mix + "/ordinary-closure/"
                            + mix + "_integrated_measurements.json");
        };
    }

    private static JsonArray composeGroups(String mix) throws IOException {
        JsonArray composed = new JsonArray();
        JsonObject v2 = JsonParser.parseString(Files.readString(V2_MANIFEST)).getAsJsonObject();
        for (JsonElement element : v2.getAsJsonArray("groups")) {
            JsonObject group = element.getAsJsonObject().deepCopy();
            group.addProperty(
                    "publication_group",
                    remapPublicationGroup(group.get("publication_group").getAsString()));
            composed.add(group);
        }
        boolean deferredRecycling = "deferred_recycling".equals(mix);
        for (String host : MIX_HOSTS) {
            Path delta = Path.of(
                    "tools/waves/" + host + "/ordinary-closure/runtime_manifest_delta.json");
            appendDelta(composed, delta);
            if (!deferredRecycling && host.equals(mix)) {
                break;
            }
        }
        if (deferredRecycling) {
            appendDelta(
                    composed,
                    Path.of(
                            "tools/waves/smelter/deferred-recycling/"
                                    + "runtime_manifest_delta.json"));
        }
        return composed;
    }

    private static void appendDelta(JsonArray composed, Path deltaPath) throws IOException {
        JsonObject delta = JsonParser.parseString(Files.readString(deltaPath)).getAsJsonObject();
        for (JsonElement element : delta.getAsJsonArray("groups")) {
            composed.add(element.getAsJsonObject().deepCopy());
        }
    }

    private static Map<String, Object> artifact(List<CandidateMeasurement> candidates) {
        List<Map<String, Object>> rows = new ArrayList<>();
        candidates.forEach(candidate -> rows.add(candidateRow(candidate)));
        List<String> unverified = new ArrayList<>();
        CandidateMeasurement hybrid = candidates.stream()
                .filter(candidate -> "hybrid".equals(candidate.name))
                .findFirst()
                .orElseThrow();
        if (hybrid.eager > 21_000) {
            unverified.add("UNVERIFIED_SCALE:eager_publication_rows:" + hybrid.eager + ">21000");
        }
        if (hybrid.lazy > 500_000) {
            unverified.add("UNVERIFIED_SCALE:lazy_logical_rows:" + hybrid.lazy + ">500000");
        }
        if (hybrid.cache > 4_096) {
            unverified.add("UNVERIFIED_SCALE:lazy_cache_ceiling_rows:" + hybrid.cache + ">4096");
        }
        if (hybrid.candidatesP95() > CompactRecipeShardRouter.HARD_SHARD_CEILING) {
            unverified.add(
                    "UNVERIFIED_SCALE:lookup_candidates_p95:"
                            + hybrid.candidatesP95()
                            + ">"
                            + CompactRecipeShardRouter.HARD_SHARD_CEILING);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", 1);
        root.put("status", selectedMix.toUpperCase(Locale.ROOT)
                + "_ORDINARY_CLOSEOUT_INTEGRATED_MEASUREMENT_READY");
        root.put("id", "integrated");
        root.put("scale", "1x");
        root.put("mix", selectedMix);
        root.put("group_count", groups.size());
        root.put("compact_group_count", 19);
        root.put("smelter_group_count", 6);
        if (!"smelter".equals(selectedMix)) {
            root.put("mixer_group_count", 3);
        }
        root.put("measured_from", "OrdinaryCloseoutIntegratedMeasurementHarness");
        root.put("candidates", rows);
        root.put("unverified_scale", unverified);
        root.put("note",
                "Measured historical compact plus ordinary-closure groups. "
                        + "lookup_allocation is a stable-epoch lookup-only ThreadMXBean window. "
                        + "retained_memory is controlled-GC class histogram, not syncPayloadBytes. "
                        + "Closing census uses this snapshot, not opening+delta arithmetic.");
        return root;
    }

    private static Map<String, Object> candidateRow(CandidateMeasurement candidate) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("candidate", candidate.name);
        row.put("eager_publication_rows", candidate.eager);
        row.put("lazy_logical_rows", candidate.lazy);
        row.put("lazy_cache_ceiling_rows", candidate.cache);
        row.put("logical_rows", candidate.logical);
        row.put("status", "PASS");
        row.put("sync", Map.of("bytes", candidate.syncBytes));
        row.put("server", side(candidate.serverReloadP50, candidate.serverReloadP95,
                candidate.serverIndexP50, candidate.serverIndexP95));
        row.put("dedicated_client", side(candidate.clientReloadP50, candidate.clientReloadP95,
                candidate.clientIndexP50, candidate.clientIndexP95));
        row.put("lookup", Map.of(
                "p50_ns", candidate.lookupP50,
                "p95_ns", candidate.lookupP95,
                "candidates_p95", candidate.candidatesP95,
                "window", CompactLoadMetricWindows.LOOKUP));
        Map<String, Object> retained = new LinkedHashMap<>();
        retained.put("p50_bytes", candidate.retained.bytes());
        retained.put("method", candidate.retained.method());
        retained.put("window", CompactLoadMetricWindows.RETAINED_AFTER_GC);
        if (candidate.retained.jcmd() != null) {
            retained.put("jcmd", candidate.retained.jcmd());
        }
        retained.put("histogram_total_bytes", candidate.retained.histogramTotalBytes());
        retained.put("recipe_class_bytes", candidate.retained.recipeClassBytes());
        row.put("retained_memory", retained);
        row.put("allocation", candidate.allocationSupported
                ? Map.of(
                        "p50_bytes", candidate.allocationP50,
                        "p95_bytes", candidate.allocationP95,
                        "window", CompactLoadMetricWindows.RELOAD_TRANSIENT)
                : Map.of("status", "UNSUPPORTED"));
        row.put("lookup_allocation", candidate.allocationSupported
                ? Map.of(
                        "p50_bytes", candidate.lookupAllocationP50,
                        "p95_bytes", candidate.lookupAllocationP95,
                        "window", CompactLoadMetricWindows.LOOKUP,
                        "operations", LOOKUP_OPS_PER_SAMPLE)
                : Map.of("status", "UNSUPPORTED"));
        return row;
    }

    private static Map<String, Object> side(long reloadP50, long reloadP95, long indexP50, long indexP95) {
        return Map.of(
                "reload", Map.of("p50_ns", reloadP50, "p95_ns", reloadP95),
                "index", Map.of("p50_ns", indexP50, "p95_ns", indexP95));
    }

    private record RetainedSample(
            long bytes,
            String method,
            String jcmd,
            long histogramTotalBytes,
            long recipeClassBytes) {
    }

    private record CandidateMeasurement(
            String name,
            int eager,
            int lazy,
            int cache,
            int logical,
            long syncBytes,
            long serverReloadP50,
            long serverReloadP95,
            long serverIndexP50,
            long serverIndexP95,
            long clientReloadP50,
            long clientReloadP95,
            long clientIndexP50,
            long clientIndexP95,
            long lookupP50,
            long lookupP95,
            int candidatesP95,
            boolean allocationSupported,
            Long allocationP50,
            Long allocationP95,
            Long lookupAllocationP50,
            Long lookupAllocationP95,
            RetainedSample retained) {
    }
}
