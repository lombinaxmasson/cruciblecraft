package com.masson.cruciblecraft.material;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPackCache;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.network.MaterialConfigurationHandshake;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Manual forked-JVM stress harness. Not part of the default unit-test task. */
public final class MaterialRegistryStressHarness {
    private static final int SYNTHETIC_MATERIALS = 20_000;

    private MaterialRegistryStressHarness() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1
                || !(args[0].equals("metadata_only") || args[0].equals("single_dust"))) {
            throw new IllegalArgumentException(
                    "Expected one scenario: metadata_only or single_dust");
        }
        String scenario = args[0];
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        long enqueueStarted = System.nanoTime();
        for (int index = 0; index < SYNTHETIC_MATERIALS; index++) {
            MaterialCatalog.addStartupMaterial(definition(scenario, index));
        }
        long enqueueNanos = System.nanoTime() - enqueueStarted;

        Path config = Files.createTempDirectory("cruciblecraft-material-stress-config");
        MaterialPrefixCatalog.bootstrap(config.resolve("prefixes"));
        long bootstrapStarted = System.nanoTime();
        MaterialCatalog.bootstrap(config.resolve("materials"));
        long bootstrapNanos = System.nanoTime() - bootstrapStarted;

        long fingerprintStarted = System.nanoTime();
        String fingerprint = MaterialFingerprint.structure(MaterialCatalog.values());
        Map<String, String> entries =
                MaterialFingerprint.structureEntries(MaterialCatalog.values());
        long fingerprintNanos = System.nanoTime() - fingerprintStarted;

        long deferredStarted = System.nanoTime();
        DeferredRegister.Items deferred = DeferredRegister.createItems(
                "cruciblecraft_stress_" + scenario);
        int queuedItems = 0;
        for (MaterialDefinition material : MaterialCatalog.values()) {
            if (!material.id().startsWith("stress_")) continue;
            for (var form : MaterialCatalog.registeredForms(material)) {
                deferred.register(
                        material.registryName(form),
                        () -> new Item(new Item.Properties()));
                queuedItems++;
            }
        }
        long deferredNanos = System.nanoTime() - deferredStarted;

        Method serverFilesMethod = GeneratedMaterialPack.class.getDeclaredMethod("serverFiles");
        Method clientFilesMethod = GeneratedMaterialPack.class.getDeclaredMethod("clientFiles");
        serverFilesMethod.setAccessible(true);
        clientFilesMethod.setAccessible(true);
        long packStarted = System.nanoTime();
        @SuppressWarnings("unchecked")
        Map<String, String> serverFiles =
                (Map<String, String>) serverFilesMethod.invoke(null);
        @SuppressWarnings("unchecked")
        Map<String, String> clientFiles =
                (Map<String, String>) clientFilesMethod.invoke(null);
        long packNanos = System.nanoTime() - packStarted;

        Path coldRoot = Files.createTempDirectory("cruciblecraft-material-stress-pack");
        LinkedHashMap<String, String> allPackFiles = new LinkedHashMap<>();
        serverFiles.forEach((path, content) -> allPackFiles.put("server/" + path, content));
        clientFiles.forEach((path, content) -> allPackFiles.put("client/" + path, content));
        AtomicInteger coldPlanCalls = new AtomicInteger();
        long coldCacheStarted = System.nanoTime();
        boolean coldChanged = GeneratedMaterialPackCache.ensure(
                coldRoot, fingerprint, () -> {
                    coldPlanCalls.incrementAndGet();
                    return allPackFiles;
                });
        long coldCacheNanos = System.nanoTime() - coldCacheStarted;
        AtomicInteger hotPlanCalls = new AtomicInteger();
        long hotCacheStarted = System.nanoTime();
        boolean hotChanged = GeneratedMaterialPackCache.ensure(
                coldRoot, fingerprint, () -> {
                    hotPlanCalls.incrementAndGet();
                    return allPackFiles;
                });
        long hotCacheNanos = System.nanoTime() - hotCacheStarted;

        var payload = new MaterialConfigurationHandshake.StructurePayload(
                fingerprint, entries);
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        long encodeStarted = System.nanoTime();
        String expectedFailure = null;
        try {
            MaterialConfigurationHandshake.StructurePayload.STREAM_CODEC.encode(
                    encoded, payload);
        } catch (IllegalArgumentException exception) {
            expectedFailure = exception.getMessage();
        }
        long encodeNanos = System.nanoTime() - encodeStarted;
        int encodedBytes = encoded.readableBytes();

        LinkedHashMap<String, String> withinCap = new LinkedHashMap<>();
        entries.entrySet().stream().limit(4_000).forEach(entry ->
                withinCap.put(entry.getKey(), entry.getValue()));
        var withinCapPayload = new MaterialConfigurationHandshake.StructurePayload(
                fingerprint, withinCap);
        FriendlyByteBuf roundTripBuffer = new FriendlyByteBuf(Unpooled.buffer());
        long roundTripStarted = System.nanoTime();
        MaterialConfigurationHandshake.StructurePayload.STREAM_CODEC.encode(
                roundTripBuffer, withinCapPayload);
        var decoded =
                MaterialConfigurationHandshake.StructurePayload.STREAM_CODEC.decode(
                        roundTripBuffer);
        long roundTripNanos = System.nanoTime() - roundTripStarted;

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("scenario", scenario);
        result.put("synthetic_materials", SYNTHETIC_MATERIALS);
        result.put("catalog_materials", MaterialCatalog.values().size());
        result.put("registered_synthetic_items", queuedItems);
        result.put("catalog_enqueue_ms", millis(enqueueNanos));
        result.put("catalog_bootstrap_ms", millis(bootstrapNanos));
        result.put("fingerprint_ms", millis(fingerprintNanos));
        result.put("deferred_register_queue_ms", millis(deferredNanos));
        result.put("generated_pack_ms", millis(packNanos));
        result.put("generated_server_files", serverFiles.size());
        result.put("generated_client_models", clientFiles.size() - 1);
        result.put("generated_pack_bytes", allPackFiles.values().stream()
                .mapToLong(value -> value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
                .sum());
        result.put("cold_cache_ms", millis(coldCacheNanos));
        result.put("hot_cache_ms", millis(hotCacheNanos));
        result.put("cold_cache_changed", coldChanged);
        result.put("hot_cache_changed", hotChanged);
        result.put("cold_cache_plan_calls", coldPlanCalls.get());
        result.put("hot_cache_plan_calls", hotPlanCalls.get());
        result.put("handshake_entries", entries.size());
        result.put("handshake_encode_ms", millis(encodeNanos));
        result.put("handshake_encoded_bytes", encodedBytes);
        result.put("handshake_20k_expected_failure", expectedFailure);
        result.put("handshake_4000_round_trip_ms", millis(roundTripNanos));
        result.put("handshake_4000_round_trip_entries", decoded.materials().size());
        result.put("max_heap_bytes", Runtime.getRuntime().maxMemory());
        result.put("used_heap_bytes", Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory());
        System.out.println("STRESS_RESULT=" + new Gson().toJson(result));
    }

    private static MaterialDefinition definition(String scenario, int index) {
        String id = "stress_" + String.format("%05d", index);
        if (scenario.equals("metadata_only")) {
            return MaterialDefinition.metadataOnly(
                    id,
                    id,
                    Optional.empty(),
                    0,
                    "#808080",
                    "matte",
                    new ThermalProperties(1.0),
                    false,
                    Map.of(),
                    true);
        }
        return new MaterialDefinition(
                id,
                id,
                Optional.empty(),
                0,
                "#808080",
                "matte",
                List.of(MaterialPrefixes.DUST),
                Map.of(),
                new ThermalProperties(1.0),
                false,
                Map.of(),
                true);
    }

    private static double millis(long nanos) {
        return Math.round(nanos / 100_000.0) / 10.0;
    }
}
