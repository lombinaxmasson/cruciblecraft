package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integrated assembler-through-identity co-load measurement. Writes
 * {@code tools/bath_identity_integrated_measurements.json}. Card-only bath-identity samples
 * stay in {@link BathIdentityMeasurementHarness}.
 *
 * <p>group_count is 13 closed groups plus bath-identity groups once the v2 manifest
 * includes bath-identity. Skips when bath-identity generated families are missing.
 */
class BathIdentityIntegratedMeasurementHarness {
    private static final int PREPARE_WARMUP = 1;
    private static final int LOOKUP_OPS_PER_SAMPLE = 8;
    private static final long EPOCH = 11L;
    private static final Path MANIFEST = Path.of(
            "tools/compact_recipe_runtime_manifest.v2.json");
    private static final Path OUTPUT = Path.of(
            "tools/bath_identity_integrated_measurements.json");

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

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path waveRoot = CompactGTRecipeFamilyGeneratedSupport.bathIdentityGeneratedRoot();
        Assumptions.assumeTrue(
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamiliesRecursive(waveRoot),
                () -> "bath-identity generated compact families are not available at " + waveRoot);
        sources = new ArrayList<>();
        for (Path root : CompactGTRecipeFamilyGeneratedSupport.generatedRootsThroughBathIdentity()) {
            sources.addAll(CompactGTRecipeFamilyGeneratedSupport
                    .loadGeneratedSourcesRecursive(root, registries));
        }
        JsonObject manifest = JsonParser.parseString(Files.readString(MANIFEST))
                .getAsJsonObject();
        groups = manifest.getAsJsonArray("groups");
        assertTrue(
                groups.size() >= 13,
                "assembler-through-identity integrated load expects the 13 closed groups plus bath-identity");
        boolean hasBathExact = false;
        boolean hasBathExactMulti = false;
        boolean hasBathToolHead = false;
        boolean hasRemainderExact = false;
        boolean hasRemainderExactMulti = false;
        for (int index = 0; index < groups.size(); index++) {
            String publicationGroup = groups.get(index).getAsJsonObject()
                    .get("publication_group").getAsString();
            if ("cruciblecraft:bath/identity/exact".equals(publicationGroup)) {
                hasBathExact = true;
            }
            if ("cruciblecraft:bath/identity/exact_multi".equals(publicationGroup)) {
                hasBathExactMulti = true;
            }
            if ("cruciblecraft:bath/identity/tool_head".equals(publicationGroup)) {
                hasBathToolHead = true;
            }
            if ("cruciblecraft:bath/remainder/exact".equals(publicationGroup)) {
                hasRemainderExact = true;
            }
            if ("cruciblecraft:bath/remainder/exact_multi".equals(publicationGroup)) {
                hasRemainderExactMulti = true;
            }
        }
        assertTrue(hasBathExact, "composed v2 must include cruciblecraft:bath/identity/exact");
        assertTrue(
                hasBathExactMulti,
                "composed v2 must include cruciblecraft:bath/identity/exact_multi");
        assertTrue(
                hasBathToolHead,
                "composed v2 must include cruciblecraft:bath/identity/tool_head");
        assertTrue(hasRemainderExact, "composed v2 must keep cruciblecraft:bath/remainder/exact");
        assertTrue(
                hasRemainderExactMulti,
                "composed v2 must keep cruciblecraft:bath/remainder/exact_multi");
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
                        eager.add(ResourceLocation.parse(element.getAsString())));
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
                    () -> "bath-identity publication group not yet in runtime manifest v2: "
                            + key.publicationGroup());
        }
        lookupQueries = buildLookupQueries(sources);
    }

    @Test
    void measureIntegratedAssemblerThroughIdentityAndWriteArtifact() throws Exception {
        assertTrue(
                productionPolicies.size() >= 13,
                "assembler-through-identity group_count should be 13 closed groups plus bath-identity");
        CandidateMeasurement immediate = measureCandidate("immediate", immediatePolicies);
        CandidateMeasurement onDemand = measureCandidate("on_demand", onDemandPolicies);
        CandidateMeasurement hybrid = measureCandidate("hybrid", productionPolicies);
        assertEquals(immediate.logical, onDemand.logical);
        assertEquals(immediate.logical, hybrid.logical);
        assertTrue(hybrid.logical > 1517);
        PublicationGroupKey t46 = new PublicationGroupKey(
                ModRecipeMaps.BATH.id(),
                ResourceLocation.parse("cruciblecraft:bath/mte"));
        var production = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources, knownMaps, EPOCH,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                productionPolicies);
        assertEquals(0, production.get(t46).eagerRecipeCount());
        assertEquals(1517, production.get(t46).lazyRecipeCount());
        assertEquals(24, production.get(t46).cacheCeiling());
        assertTrue(
                hybrid.candidatesP95() <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                () -> "integrated hybrid lookup scanned " + hybrid.candidatesP95());
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String encoded = gson.toJson(artifact(List.of(immediate, onDemand, hybrid))) + "\n";
        if (Boolean.getBoolean("cruciblecraft.writeBathIdentityMeasurements")) {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(OUTPUT, encoded, StandardCharsets.UTF_8);
        }
        assertTrue(encoded.contains("\"group_count\""));
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
        int queryCount = lookupQueries.size();
        for (int sample = 0; sample < sampleCount(); sample++) {
            long total = 0L;
            int maxCandidates = 0;
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
        }
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
                allocationSupported ? percentile(allocation, 95) : null);
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
        List<GTRecipeQuery> queries = new ArrayList<>();
        for (CompactRecipeFamilySource source : familySources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.definition().relations()) {
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
                    queries.add(new GTRecipeQuery(offered, relation.fluidInputs()));
                }
            }
        }
        assertFalse(queries.isEmpty());
        return List.copyOf(queries);
    }

    private static int sampleCount() {
        return Boolean.getBoolean("cruciblecraft.writeBathIdentityMeasurements") ? 21 : 3;
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

    private static Map<String, Object> artifact(List<CandidateMeasurement> candidates) {
        List<Map<String, Object>> rows = new ArrayList<>();
        candidates.forEach(candidate -> rows.add(candidateRow(candidate)));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", 1);
        root.put("status", "T48_INTEGRATED_MEASUREMENT_READY");
        root.put("id", "integrated");
        root.put("scale", "1x");
        root.put("group_count", groups.size());
        root.put("load_budget_policy_v2_sha256", hashV2());
        root.put("candidates", rows);
        root.put("note",
                "Measured assembler-through-identity compact families. group_count is 13 closed groups "
                        + "plus bath-identity groups once runtime manifest v2 includes bath-identity. "
                        + "Closing census uses this snapshot, not opening+delta arithmetic.");
        return root;
    }

    private static String hashV2() {
        try {
            byte[] bytes = Files.readAllBytes(Path.of("tools/t14_load_budget_policy.v2.json"));
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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
                "candidates_p95", candidate.candidatesP95));
        row.put("retained_memory", Map.of("p50_bytes", candidate.syncBytes));
        row.put("allocation", candidate.allocationSupported
                ? Map.of("p50_bytes", candidate.allocationP50, "p95_bytes", candidate.allocationP95)
                : Map.of("status", "UNSUPPORTED"));
        return row;
    }

    private static Map<String, Object> side(long reloadP50, long reloadP95, long indexP50, long indexP95) {
        return Map.of(
                "reload", Map.of("p50_ns", reloadP50, "p95_ns", reloadP95),
                "index", Map.of("p50_ns", indexP50, "p95_ns", indexP95));
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
            Long allocationP95) {
    }
}
