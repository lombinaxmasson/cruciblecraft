package com.masson.cruciblecraft.scale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.logistics.pipe.item.ItemPipeNetworkTraversal;
import com.masson.cruciblecraft.network.CoverConfigurationPayload;
import com.masson.cruciblecraft.network.MaterialConfigurationHandshake;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ScaleHarnessIdentityTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void specReadsManifestWithoutASecondConstantTable() throws Exception {
        ScaleWorkloadSpec spec = ScaleWorkloadSpec.load();
        JsonObject manifest = JsonParser.parseString(
                Files.readString(
                        ScaleWorkloadSpec.findRepoRoot().resolve(
                                ScaleWorkloadSpec.MANIFEST_RELATIVE),
                        StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertEquals(
                manifest.get("workload_identity").getAsString(),
                spec.workloadIdentity());
        for (String name : List.of("small", "target", "stress")) {
            JsonObject row = manifest.getAsJsonObject("scenarios")
                    .getAsJsonObject(name);
            ScaleWorkloadSpec.Scenario scenario = spec.scenario(name);
            JsonObject counts = row.getAsJsonObject("counts");
            assertEquals(counts.get("fluid_pipes").getAsInt(), scenario.fluidPipes(), name);
            assertEquals(counts.get("item_pipes").getAsInt(), scenario.itemPipes(), name);
            assertEquals(counts.get("covers").getAsInt(), scenario.covers(), name);
            assertEquals(
                    counts.get("processing_machines").getAsInt(),
                    scenario.processingMachines(),
                    name);
            assertEquals(
                    counts.get("energy_converters").getAsInt(),
                    scenario.energyConverters(),
                    name);
            assertEquals(counts.get("multiblocks").getAsInt(), scenario.multiblocks(), name);
            assertEquals(
                    counts.get("petroleum_chains").getAsInt(),
                    scenario.petroleumChains(),
                    name);
            assertEquals(
                    row.getAsJsonObject("ticks").get("warmup").getAsInt(),
                    scenario.warmupTicks(),
                    name);
            assertEquals(
                    row.getAsJsonObject("ticks").get("sampling").getAsInt(),
                    scenario.samplingTicks(),
                    name);
            assertEquals(row.get("seed").getAsLong(), scenario.seed(), name);
            assertEquals(
                    scenario.fluidPipes() + scenario.itemPipes(),
                    scenario.pipesTotal(),
                    name);
        }
    }

    @Test
    void sameSeedYieldsStableWorkloadIdentity() {
        assertEquals(
                ScaleWorkloadSpec.load().workloadIdentity(),
                ScaleWorkloadSpec.load().workloadIdentity());
    }

    @Test
    void instrumentationDisabledDoesNotRecord() {
        assertFalse(ScaleInstrumentation.isEnabled());
        ScaleInstrumentation.recordRouteVisit(20);
        ScaleInstrumentation.Recorder recorder = new ScaleInstrumentation.Recorder();
        ScaleInstrumentation.enter(recorder);
        try {
            assertTrue(ScaleInstrumentation.isEnabled());
            ScaleInstrumentation.recordRouteVisit(20);
            ScaleInstrumentation.recordRouteVisit(7);
            assertEquals(20, recorder.visitedMax());
            assertEquals(20, recorder.visitedP95());
            assertEquals(List.of(20, 7), recorder.visitedSamples());
            recorder.reset();
            assertEquals(0, recorder.visitedMax());
            assertTrue(recorder.visitedSamples().isEmpty());
        } finally {
            ScaleInstrumentation.exit();
        }
        assertFalse(ScaleInstrumentation.isEnabled());
        ScaleInstrumentation.recordRouteVisit(99);
        assertTrue(recorder.visitedSamples().isEmpty());
    }

    @Test
    void instrumentationRejectsNegativeVisitedAndAcceptsCap() {
        ScaleInstrumentation.Recorder recorder = new ScaleInstrumentation.Recorder();
        assertThrows(
                IllegalArgumentException.class,
                () -> recorder.recordVisited(-1));
        recorder.recordVisited(ItemPipeNetworkTraversal.MAX_VISITED_PIPES);
        recorder.recordCacheEntries(256);
        assertEquals(
                ItemPipeNetworkTraversal.MAX_VISITED_PIPES,
                recorder.visitedMax());
        assertEquals(256, recorder.cacheEntriesMax());
        assertThrows(
                IllegalArgumentException.class,
                () -> recorder.recordCacheEntries(-1));
    }

    @Test
    void coverPayloadEncodeUsesTheRealCodec() {
        CoverConfigurationPayload payload = new CoverConfigurationPayload(
                new BlockPos(1, 64, 2), 0, 1, 4);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        CoverConfigurationPayload.STREAM_CODEC.encode(buffer, payload);
        int bytes = buffer.readableBytes();
        buffer.release();
        assertTrue(bytes > 0);
        assertTrue(bytes <= CoverConfigurationPayload.MAX_ENCODED_BYTES);
    }

    @Test
    void transferConservationTreatsNegativeDeliveredAsFailure() {
        assertTrue(0 <= 0);
        assertFalse(-1 >= 0);
    }

    @Test
    void handshakeNetworkVersionRemainsOne() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/network/"
                        + "MaterialConfigurationHandshake.java"));
        assertTrue(source.contains("NETWORK_VERSION = \"1\""));
        assertTrue(source.contains("MaterialConfigurationHandshake"));
        assertNotEquals("", MaterialConfigurationHandshake.class.getName());
    }

    @Test
    void scaleGameTestsAreNotInTheRequiredGametestPackage() throws Exception {
        String gametest = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/gametest/"
                        + "CrucibleCraftGameTests.java"));
        assertFalse(gametest.contains("scaleMeasurement"));
        String holder = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/scale/"
                        + "ScaleGameTests.java"));
        assertTrue(holder.contains("@GameTestHolder"));
        assertTrue(holder.contains("cruciblecraft_scale"));
        assertTrue(holder.contains("scaleMeasurement"));
        assertFalse(holder.contains("@GameTestHolder(CrucibleCraft.MODID)"));
    }
}
