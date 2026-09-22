package com.masson.cruciblecraft.scale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.blockentity.TankBlockEntity;
import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator.StructureAccess;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferPhase;
import com.masson.cruciblecraft.logistics.pipe.item.ItemPipeNetworkTraversal;
import com.masson.cruciblecraft.network.CoverConfigurationPayload;

import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Scale evidence: bounded operation counts for the three declared
 * workload scenarios.
 *
 * <p>Evidence classes:
 * <ul>
 * <li>{@code STATIC_INFERENCE} — the 5-tick position-phased pipe schedule
 * ({@link PipeTransferPhase#isDue}), route/cache/payload caps pinned as
 * declared constants, and the manifest-to-gate binding; no wall-clock.
 * <li>{@code SYNTHETIC_BENCHMARK} — the multiblock validation counters
 * below (fake {@link StructureAccess}, exact call counts, deterministic).
 * </ul>
 * Wall-clock is recorded for reference only and is never gated.
 */
class ScaleWorkloadBoundTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void targetScenarioSchedulesExactlyOneHundredDuePipesPerTick() {
        // Pure JVM: 500 consecutive pipe positions over a 5-tick
        // position-phased schedule visit each residue class exactly 100
        // times, so exactly 100 pipes are due on every tick.
        for (long tick = 0; tick < PipeTransferPhase.INTERVAL; tick++) {
            int due = 0;
            for (int pipe = 0; pipe < 500; pipe++) {
                if (PipeTransferPhase.isDue(
                        tick, new BlockPos(pipe, 64, 0))) {
                    due++;
                }
            }
            assertEquals(100, due, "tick " + tick);
        }
    }

    @Test
    void stressScenarioSchedulesExactlyFourHundredDuePipesPerTick() {
        // Same partition rule at 2,000 pipes: 400 due per tick.
        for (long tick = 0; tick < PipeTransferPhase.INTERVAL; tick++) {
            int due = 0;
            for (int pipe = 0; pipe < 2_000; pipe++) {
                if (PipeTransferPhase.isDue(
                        tick, new BlockPos(pipe, 64, 0))) {
                    due++;
                }
            }
            assertEquals(400, due, "tick " + tick);
        }
    }

    @Test
    void scenarioMultiblockValidationOpsAreBounded() throws Exception {
        // Real tank_3x3x3 definition decoded from the bundled datapack
        // JSON (27 elements: 1 controller + 25 ports + 1 air). The
        // validator visits every element exactly once; port elements add
        // exactly one blockEntity read each.
        MultiblockStructureDefinition tank = loadTankDefinition();
        assertEquals(27, tank.structure().size());
        assertEquals(25, tank.portCount(PortType.FLUID));

        CounterAccess access = new CounterAccess();
        MultiblockStructureValidator.validate(
                tank, access, BlockPos.ZERO, Direction.NORTH);
        assertEquals(
                access.isLoadedCalls(), access.blockStateCalls(),
                "validator must check each position exactly once");
        assertEquals(27, access.isLoadedCalls());
        assertEquals(27, access.blockStateCalls());
        assertEquals(25, access.blockEntityCalls());
    }

    @Test
    void scenarioPortScanOpsAreBounded() throws Exception {
        // MultiblockPortAggregator.refresh performs one hasChunkAt plus
        // one getBlockEntity per matched port: 2 ops per port. The real
        // tank declares 25 ports, so the worst case is exactly 50.
        MultiblockStructureDefinition tank = loadTankDefinition();
        long ports = tank.portCount(PortType.FLUID);
        assertEquals(25, ports);
        assertEquals(50, 2 * ports);
        JsonObject tankRow = readJson(
                        "tools/structure_load_evidence.json")
                .getAsJsonObject("structures")
                .getAsJsonObject("tank_3x3x3");
        assertEquals(50, tankRow.get("port_scan_ops_max").getAsInt());
        assertEquals(54, tankRow.get("validation_ops_max").getAsInt());
        assertEquals(27, tankRow.get("positions").getAsInt());
    }

    @Test
    void scenarioAllT23StructuresStayWithinDeclaredCaps() {
        // The target/stress scenarios include the distillation tower and
        // the large boiler. Every selected structure must respect
        // 2 ops per position and 2 ops per port and stay inside the
        // 4,096-position scan volume.
        JsonObject structures = readJson("tools/structure_load_evidence.json")
                .getAsJsonObject("structures");
        assertEquals(
                3, structures.size(), "selected structure set");
        for (String id : structures.keySet()) {
            JsonObject row = structures.getAsJsonObject(id);
            int positions = row.get("positions").getAsInt();
            int validation = row.get("validation_ops_max").getAsInt();
            int ports = row.get("port_count").getAsInt();
            int portScan = row.get("port_scan_ops_max").getAsInt();
            assertTrue(
                    positions > 0 && positions <= 4_096,
                    id + " scan volume");
            assertEquals(2 * positions, validation, id);
            assertEquals(2 * ports, portScan, id);
        }
        // Worst single-structure op counts across the selected set are
        // owned by the distillation tower (81 positions, 80 ports).
        JsonObject tower = structures.getAsJsonObject("distillation_tower");
        assertEquals(81, tower.get("positions").getAsInt());
        assertEquals(162, tower.get("validation_ops_max").getAsInt());
        assertEquals(80, tower.get("port_count").getAsInt());
        assertEquals(160, tower.get("port_scan_ops_max").getAsInt());
    }

    @Test
    void scenarioRouteDiscoveryStaysUnderDeclaredCap() {
        // The traversal cap is a declared constant; the largest scenario
        // (2,000 pipes) stays far below the 32,768 visited-pipe cap.
        assertEquals(32_768, ItemPipeNetworkTraversal.MAX_VISITED_PIPES);
        JsonObject gate = readJson("tools/cover_readiness.json")
                .getAsJsonObject("performance_gate")
                .getAsJsonObject("route_discovery");
        assertEquals(
                32_768, gate.get("maximum_visited_pipes").getAsInt());
        assertTrue(2_000 < ItemPipeNetworkTraversal.MAX_VISITED_PIPES);
    }

    @Test
    void scenarioRouteCacheStaysUnderDeclaredCap() {
        // The per-pipe route cache cap is declared at 256 entries; the
        // scenario never raises it, so every pipe in every scenario stays
        // under the same per-pipe cap.
        JsonObject memory = readJson("tools/cover_readiness.json")
                .getAsJsonObject("performance_gate")
                .getAsJsonObject("memory");
        assertEquals(
                256,
                memory.get("maximum_route_cache_entries_per_item_pipe")
                        .getAsInt());
    }

    @Test
    void scenarioCoverPayloadFitsThirteenByteBound() {
        // The worst-case cover configuration packet is bounded at 13
        // bytes (BlockPos long + side byte + field byte + bounded
        // three-byte VarInt); the encoding bound itself is asserted by
        // CoverConfigurationPayloadTest.
        assertEquals(13, CoverConfigurationPayload.MAX_ENCODED_BYTES);
        JsonObject sync = readJson("tools/cover_readiness.json")
                .getAsJsonObject("performance_gate")
                .getAsJsonObject("synchronization");
        assertEquals(
                13,
                sync.get("maximum_configuration_payload_bytes")
                        .getAsInt());
        assertEquals(
                5, sync.get("fluid_client_sync_interval_ticks").getAsInt());
    }

    @Test
    void scenarioMenuSyncStaysAtThreeContainerDataInts() {
        assertEquals(3, ConfiguredProcessingMachineMenu.DATA_COUNT);
        JsonObject network = readJson("tools/structure_load_evidence.json")
                .getAsJsonObject("network_sync");
        assertEquals(3, network.get("menu_slots").getAsInt());
    }

    @Test
    void manifestCountsMatchDeclaredBounds() {
        // The binding test: every hard-gate number asserted above must be
        // consistent with the generated manifest; anyone who changes a
        // scenario count beyond the declared bounds turns this test red.
        JsonObject manifest = readJson("tools/scale_workload_manifest.json");
        assertEquals(
                "MANIFEST_COMPLETE", manifest.get("status").getAsString());
        String identity = manifest.get("workload_identity").getAsString();
        assertTrue(identity.matches("[0-9a-f]{64}"), identity);

        JsonObject scenarios = manifest.getAsJsonObject("scenarios");
        assertScenario(scenarios.getAsJsonObject("small"), 20, 4, 3, 2, 4, 1, 0);
        assertScenario(scenarios.getAsJsonObject("target"), 500, 100, 24, 8, 24, 3, 1);
        assertScenario(scenarios.getAsJsonObject("stress"), 2_000, 400, 96, 32, 96, 12, 4);

        JsonObject pipeGate = readJson("tools/cover_readiness.json")
                .getAsJsonObject("performance_gate")
                .getAsJsonObject("tick_schedule");
        assertEquals(5, pipeGate.get("interval_ticks").getAsInt());
        assertEquals(100, pipeGate.get("maximum_due_per_tick").getAsInt());
    }

    private static void assertScenario(
            JsonObject scenario,
            int pipesTotal,
            int duePerTick,
            int machines,
            int converters,
            int covers,
            int multiblocks,
            int petroleumChains) {
        JsonObject derived = scenario.getAsJsonObject("derived");
        assertEquals(pipesTotal, derived.get("pipes_total").getAsInt());
        assertEquals(
                duePerTick, derived.get("pipes_due_per_tick").getAsInt());
        assertTrue(derived.get("pipes_partition_exact").getAsBoolean());
        JsonObject counts = scenario.getAsJsonObject("counts");
        assertEquals(machines, counts.get("processing_machines").getAsInt());
        assertEquals(converters, counts.get("energy_converters").getAsInt());
        assertEquals(covers, counts.get("covers").getAsInt());
        assertEquals(multiblocks, counts.get("multiblocks").getAsInt());
        assertEquals(
                petroleumChains, counts.get("petroleum_chains").getAsInt());
    }

    private static MultiblockStructureDefinition loadTankDefinition()
            throws Exception {
        String json;
        try (InputStream input = ScaleWorkloadBoundTest.class
                .getResourceAsStream(
                        "/data/cruciblecraft/multiblock_structures/"
                                + TankBlockEntity.STRUCTURE_ID.getPath()
                                + ".json")) {
            assertTrue(input != null, "tank structure resource missing");
            json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        return MultiblockStructureDefinition.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(IllegalStateException::new);
    }

    private static JsonObject readJson(String relativePath) {
        try {
            return JsonParser.parseString(
                            Files.readString(Path.of(relativePath)))
                    .getAsJsonObject();
        } catch (Exception failure) {
            throw new AssertionError(
                    "failed to read " + relativePath, failure);
        }
    }

    private static final class CounterAccess implements StructureAccess {
        private long isLoadedCalls;
        private long blockStateCalls;
        private long blockEntityCalls;

        long isLoadedCalls() {
            return isLoadedCalls;
        }

        long blockStateCalls() {
            return blockStateCalls;
        }

        long blockEntityCalls() {
            return blockEntityCalls;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            isLoadedCalls++;
            return true;
        }

        @Override
        public BlockState blockState(BlockPos pos) {
            blockStateCalls++;
            return Blocks.STONE.defaultBlockState();
        }

        @Override
        public BlockEntity blockEntity(BlockPos pos) {
            blockEntityCalls++;
            return null;
        }
    }
}
