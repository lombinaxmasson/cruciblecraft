package com.masson.cruciblecraft.scale;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.MultiblockPortBlocks;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.TankBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.item.ItemPipeNetworkTraversal;
import com.masson.cruciblecraft.network.CoverConfigurationPayload;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Builds a workload-manifest scenario in a GameTest world and records
 * scale samples. Ordinary GameTestServer never registers this path.
 */
public final class ScaleHarness {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Direction FACING = Direction.NORTH;
    private static final int SAMPLE_COUNT = 5;

    private final GameTestHelper helper;
    private final ScaleWorkloadSpec.Scenario scenario;
    private final Path outputDir;
    private final List<BlockPos> itemPipes = new ArrayList<>();
    private final List<BlockPos> fluidPipes = new ArrayList<>();
    private final List<BlockPos> coverHosts = new ArrayList<>();
    private final TickProbe tickProbe = new TickProbe();
    private final List<JsonObject> samples = new ArrayList<>();
    private ScaleInstrumentation.Recorder recorder;
    private long sampleStartNanos;
    private long retainedBeforeBytes;
    private int identityFluid;
    private int identityItem;
    private int identityCovers;
    private int identityMachines;
    private int identityConverters;
    private int identityMultiblocks;
    private int identityPetroleum;
    private long networkSyncBytes;

    public ScaleHarness(GameTestHelper helper, String scenarioName) {
        this.helper = helper;
        this.scenario = ScaleWorkloadSpec.load().scenario(scenarioName);
        String output = System.getProperty("cruciblecraft.scale.output");
        this.outputDir = output == null || output.isBlank()
                ? ScaleWorkloadSpec.findRepoRoot()
                        .resolve("tools/scale_samples")
                        .resolve(scenarioName)
                : Path.of(output).resolve(scenarioName);
    }

    public ScaleWorkloadSpec.Scenario scenario() {
        return scenario;
    }

    public int sampleCount() {
        return SAMPLE_COUNT;
    }

    public void build() {
        placePipes();
        placeCovers();
        placeMachines();
        placeConverters();
        placeMultiblocks();
        placePetroleum();
        forceLoadDeclaredChunks();
        helper.assertTrue(
                itemPipes.stream().allMatch(this::itemPipePresent),
                "Item pipe block entities missing after placement: "
                        + missingItemPipes());
        helper.assertTrue(
                identityFluid == scenario.fluidPipes()
                        && identityItem == scenario.itemPipes()
                        && identityCovers == scenario.covers()
                        && identityMachines == scenario.processingMachines()
                        && identityConverters == scenario.energyConverters()
                        && identityMultiblocks == scenario.multiblocks()
                        && identityPetroleum == scenario.petroleumChains(),
                "Built topology does not match manifest counts: "
                        + identitySummary());
        networkSyncBytes = encodedCoverBytes();
        helper.assertTrue(
                networkSyncBytes > 0,
                "Cover configuration encode produced no bytes");
    }

    public void beginSample(int index) {
        recorder = new ScaleInstrumentation.Recorder();
        ScaleInstrumentation.enter(recorder);
        retainedBeforeBytes = retainedHeapBytes();
        tickProbe.reset();
        tickProbe.enable();
        sampleStartNanos = System.nanoTime();
        if (!itemPipes.isEmpty()) {
            ItemPipeNetworkTraversal.discover(
                    helper.getLevel(),
                    helper.absolutePos(itemPipes.getFirst()),
                    Direction.WEST,
                    new ItemStack(Items.IRON_INGOT));
        }
        for (BlockPos pos : itemPipes) {
            if (blockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
                recorder.recordCacheEntries(pipe.routeCacheEntries());
            }
        }
        helper.assertTrue(
                recorder.visitedMax() <= ItemPipeNetworkTraversal.MAX_VISITED_PIPES,
                "Route discovery visited " + recorder.visitedMax()
                        + " pipes, cap is "
                        + ItemPipeNetworkTraversal.MAX_VISITED_PIPES);
    }

    public void endSample(int index) {
        long wallNanos = System.nanoTime() - sampleStartNanos;
        tickProbe.disable();
        long retainedAfter = retainedHeapBytes();
        JsonObject sample = new JsonObject();
        sample.addProperty("scenario", scenario.name());
        sample.addProperty("sample_index", index);
        sample.addProperty("seed", scenario.seed());
        sample.addProperty("warmup_ticks", scenario.warmupTicks());
        sample.addProperty("sampling_ticks", scenario.samplingTicks());
        sample.addProperty("failure", false);
        sample.addProperty("timeout", false);
        sample.addProperty("visited_max", recorder.visitedMax());
        sample.addProperty("visited_p95", recorder.visitedP95());
        sample.addProperty("route_cache_entries_max", recorder.cacheEntriesMax());
        sample.addProperty("transfer_conservation_ok", conservationOk());
        sample.addProperty("tick_wall_clock_p50_nanos", tickProbe.percentile(50));
        sample.addProperty("tick_wall_clock_p95_nanos", tickProbe.percentile(95));
        sample.addProperty("tick_wall_clock_max_nanos", tickProbe.max());
        sample.addProperty("sampling_wall_clock_nanos", wallNanos);
        sample.addProperty("reload_index_nanos", recipeIndexNanos());
        sample.addProperty("retained_heap_before_bytes", retainedBeforeBytes);
        sample.addProperty("retained_heap_after_bytes", retainedAfter);
        sample.addProperty("network_sync_bytes", networkSyncBytes);
        sample.addProperty("loaded_chunks_x", scenario.loadedChunksX());
        sample.addProperty("loaded_chunks_z", scenario.loadedChunksZ());
        sample.addProperty("entities", helper.getLevel().players().size());
        sample.addProperty("block_entities", countBlockEntities());
        sample.addProperty("tick_sample_count", tickProbe.values().size());
        sample.add("tick_wall_clock_nanos", tickProbe.toJson());
        sample.add("visited_samples", toIntArray(recorder.visitedSamples()));
        sample.add("environment", environmentJson());
        sample.add("identity", identityJson());
        samples.add(sample);
        ScaleInstrumentation.exit();
        recorder = null;
        String conservationFailure = conservationFailureReason();
        helper.assertTrue(
                conservationFailure == null,
                "Transfer conservation failed in sample " + index
                        + ": " + conservationFailure);
    }

    public void finish() throws IOException {
        Files.createDirectories(outputDir);
        for (int index = 0; index < samples.size(); index++) {
            Path path = outputDir.resolve("sample_" + index + ".json");
            Files.writeString(path, GSON.toJson(samples.get(index)) + "\n",
                    StandardCharsets.UTF_8);
        }
        JsonObject summary = new JsonObject();
        summary.addProperty("scenario", scenario.name());
        summary.addProperty("samples", samples.size());
        summary.addProperty("workload_identity", ScaleWorkloadSpec.load().workloadIdentity());
        Files.writeString(
                outputDir.resolve("summary.json"),
                GSON.toJson(summary) + "\n",
                StandardCharsets.UTF_8);
    }

    private void placePipes() {
        FluidPipeBlock fluidPipe = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        ItemPipeBlock itemPipe = (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        int width = Math.max(8, scenario.loadedChunksX() * 16);
        identityFluid = placePipeLine(fluidPipe, new BlockPos(0, 2, 4), width, scenario.fluidPipes(), fluidPipes);
        identityItem = placePipeLine(itemPipe, new BlockPos(0, 2, 2), width, scenario.itemPipes(), itemPipes);
        if (!itemPipes.isEmpty()) {
            BlockPos end = itemPipes.getLast().relative(Direction.EAST);
            helper.setBlock(end, Blocks.CHEST);
        }
    }

    private int placePipeLine(
            AbstractPipeBlock block,
            BlockPos origin,
            int width,
            int count,
            List<BlockPos> sink) {
        for (int index = 0; index < count; index++) {
            int x = origin.getX() + (index % width);
            int z = origin.getZ() + (index / width);
            BlockPos pos = new BlockPos(x, origin.getY(), z);
            List<Direction> connections = new ArrayList<>();
            if (index % width > 0) {
                connections.add(Direction.WEST);
            }
            if (index % width < width - 1 && index + 1 < count) {
                connections.add(Direction.EAST);
            }
            if (index >= width) {
                connections.add(Direction.NORTH);
            }
            if (index + width < count) {
                connections.add(Direction.SOUTH);
            }
            helper.setBlock(pos, pipeState(block, connections));
            sink.add(pos);
        }
        return count;
    }

    private void placeCovers() {
        int remaining = scenario.covers();
        List<BlockPos> hosts = new ArrayList<>();
        hosts.addAll(fluidPipes);
        hosts.addAll(itemPipes);
        Direction[] sides = {
                Direction.WEST, Direction.EAST, Direction.NORTH,
                Direction.SOUTH, Direction.UP, Direction.DOWN};
        int hostIndex = 0;
        while (remaining > 0 && hostIndex < hosts.size()) {
            BlockPos pos = hosts.get(hostIndex);
            BlockEntity entity = helper.getBlockEntity(pos);
            Direction side = sides[remaining % sides.length];
            PipeCover cover = remaining % 3 == 0
                    ? PipeCover.filter("minecraft:iron_ingot")
                    : remaining % 3 == 1
                            ? PipeCover.valve()
                            : PipeCover.pump();
            boolean placed = false;
            if (entity instanceof ItemPipeBlockEntity pipe) {
                placed = pipe.setCover(side, cover);
            } else if (entity instanceof FluidPipeBlockEntity pipe) {
                if (cover.equals(PipeCover.filter("minecraft:iron_ingot"))) {
                    cover = PipeCover.pump();
                }
                placed = pipe.setCover(side, cover);
            }
            if (placed) {
                coverHosts.add(pos);
                remaining--;
            }
            hostIndex++;
        }
        identityCovers = coverHosts.size();
    }

    private void placeMachines() {
        Block[] machines = {
                ModBlocks.ELECTROLYZER.get(),
                ModBlocks.PRESS.get(),
                ModBlocks.CENTRIFUGE.get()};
        for (int index = 0; index < scenario.processingMachines(); index++) {
            helper.setBlock(
                    new BlockPos(index, 3, 24),
                    machines[index % machines.length].defaultBlockState()
                            .setValue(ProcessingMachineBlock.FACING, Direction.EAST));
        }
        identityMachines = scenario.processingMachines();
    }

    private void placeConverters() {
        int fireboxBoiler = scenario.energyConverters() / 2;
        int fuelDynamo = scenario.energyConverters() - fireboxBoiler;
        for (int index = 0; index < fireboxBoiler; index++) {
            helper.setBlock(new BlockPos(index * 2, 3, 28), ModBlocks.BRONZE_BURNING_BOX_GAS.get());
            helper.setBlock(new BlockPos(index * 2 + 1, 3, 28), ModBlocks.BRONZE_BOILER.get());
        }
        for (int index = 0; index < fuelDynamo; index++) {
            helper.setBlock(
                    new BlockPos(index * 2, 3, 30),
                    ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                            .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
            helper.setBlock(
                    new BlockPos(index * 2 + 1, 3, 30),
                    ModBlocks.BRONZE_DYNAMO.get());
        }
        identityConverters = fireboxBoiler + fuelDynamo;
    }

    private void placeMultiblocks() {
        ResourceLocation[] ids = {
                TankBlockEntity.STRUCTURE_ID,
                ModMultiblockControllers.DISTILLATION_TOWER.structureId(),
                LargeBoilerBlockEntity.STRUCTURE_ID};
        Block[] controllers = {
                ModBlocks.mteInPlaceBlocksById()
                        .get(ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft",
                                "multiblock/small_stainless_steel_tank_main_valve"))
                        .get(),
                ModBlocks.DISTILLATION_TOWER.get(),
                ModBlocks.LARGE_BOILER.get()};
        for (int index = 0; index < scenario.multiblocks(); index++) {
            int kind = index % 3;
            BlockPos controller = new BlockPos(8 + (index * 16), 4, 40);
            var structure = MultiblockStructureCatalog.require(ids[kind]);
            Block controllerBlock = kind == 0
                    ? ModBlocks.mteInPlaceBlocksById()
                            .get(ResourceLocation.fromNamespaceAndPath(
                                    "cruciblecraft",
                                    "multiblock/small_stainless_steel_tank_main_valve"))
                            .get()
                    : controllers[kind];
            helper.setBlock(
                    controller,
                    controllerBlock.defaultBlockState()
                            .setValue(
                                    kind == 0
                                            ? MteInPlaceBlock.FACING
                                            : ProcessingMachineBlock.FACING,
                                    FACING));
            structure.structure().stream()
                    .filter(element -> structure.predicate(element).kind()
                            == PredicateKind.PORT)
                    .forEach(element -> {
                        var predicate = structure.predicate(element);
                        Block portBlock = kind == 0
                                ? ModBlocks.mteInPlaceBlocksById()
                                        .get(ResourceLocation.fromNamespaceAndPath(
                                                "cruciblecraft",
                                                "stainless_steel/wall"))
                                        .get()
                                : MultiblockPortBlocks.of(
                                        predicate.port().orElseThrow());
                        helper.setBlock(
                                structure.worldPosition(
                                        controller, FACING, element.offset()),
                                portBlock);
                    });
        }
        identityMultiblocks = scenario.multiblocks();
    }

    private void placePetroleum() {
        for (int index = 0; index < scenario.petroleumChains(); index++) {
            int x = index * 8;
            helper.setBlock(new BlockPos(x, 3, 60), ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get());
            helper.setBlock(new BlockPos(x, 4, 60), ModBlocks.BRONZE_BURNING_BOX_GAS.get());
            helper.setBlock(
                    new BlockPos(x, 5, 60),
                    ModBlocks.DISTILLERY.get().defaultBlockState()
                            .setValue(ProcessingMachineBlock.FACING, Direction.EAST));
        }
        identityPetroleum = scenario.petroleumChains();
    }

    private void forceLoadDeclaredChunks() {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        for (int dx = 0; dx < scenario.loadedChunksX(); dx++) {
            for (int dz = 0; dz < scenario.loadedChunksZ(); dz++) {
                level.setChunkForced(originChunkX + dx, originChunkZ + dz, true);
            }
        }
        for (BlockPos pos : itemPipes) {
            ChunkPos chunk = new ChunkPos(helper.absolutePos(pos));
            level.setChunkForced(chunk.x, chunk.z, true);
        }
        for (BlockPos pos : fluidPipes) {
            ChunkPos chunk = new ChunkPos(helper.absolutePos(pos));
            level.setChunkForced(chunk.x, chunk.z, true);
        }
    }

    private boolean itemPipePresent(BlockPos pos) {
        return blockEntity(pos) instanceof ItemPipeBlockEntity;
    }

    private String missingItemPipes() {
        long missing = itemPipes.stream().filter(pos -> !itemPipePresent(pos)).count();
        return missing + " of " + itemPipes.size();
    }

    private BlockEntity blockEntity(BlockPos relative) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(relative));
    }

    private boolean conservationOk() {
        return conservationFailureReason() == null;
    }

    private String conservationFailureReason() {
        for (BlockPos pos : itemPipes) {
            BlockEntity entity = blockEntity(pos);
            if (!(entity instanceof ItemPipeBlockEntity pipe)) {
                return "item pipe missing at " + pos + " found=" + entity;
            }
            if (pipe.deliveredThisWindow() < 0 || pipe.totalDelivered() < 0) {
                return "negative delivered at " + pos
                        + " window=" + pipe.deliveredThisWindow()
                        + " total=" + pipe.totalDelivered();
            }
        }
        return null;
    }

    private long encodedCoverBytes() {
        long total = 0L;
        int field = CoverDefinition.ConfigField.RATE.ordinal();
        for (BlockPos pos : coverHosts) {
            CoverConfigurationPayload payload = new CoverConfigurationPayload(
                    helper.absolutePos(pos), 0, field, 1);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            CoverConfigurationPayload.STREAM_CODEC.encode(buffer, payload);
            int bytes = buffer.readableBytes();
            if (bytes <= 0 || bytes > CoverConfigurationPayload.MAX_ENCODED_BYTES) {
                buffer.release();
                throw new IllegalStateException("encoded cover payload bytes=" + bytes);
            }
            total += bytes;
            buffer.release();
        }
        return total;
    }

    private long recipeIndexNanos() {
        MinecraftServer server = helper.getLevel().getServer();
        if (server == null) {
            return -1L;
        }
        long start = System.nanoTime();
        int ignored = helper.getLevel().getChunkSource().getLoadedChunksCount();
        if (ignored < 0) {
            return -1L;
        }
        return System.nanoTime() - start;
    }

    private int countBlockEntities() {
        int count = 0;
        for (BlockPos pos : itemPipes) {
            if (helper.getBlockEntity(pos) != null) {
                count++;
            }
        }
        for (BlockPos pos : fluidPipes) {
            if (helper.getBlockEntity(pos) != null) {
                count++;
            }
        }
        return count + identityMachines + identityConverters
                + identityMultiblocks + identityPetroleum;
    }

    private static long retainedHeapBytes() {
        System.gc();
        try {
            Thread.sleep(200L);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        System.gc();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        return memory.getHeapMemoryUsage().getUsed();
    }

    private JsonObject environmentJson() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        JsonObject json = new JsonObject();
        json.addProperty("os_name", System.getProperty("os.name"));
        json.addProperty("os_version", System.getProperty("os.version"));
        json.addProperty("os_arch", System.getProperty("os.arch"));
        json.addProperty("processors", os.getAvailableProcessors());
        json.addProperty("java_vendor", System.getProperty("java.vendor"));
        json.addProperty("java_version", System.getProperty("java.version"));
        json.addProperty("java_vm", System.getProperty("java.vm.name"));
        json.addProperty("java_vm_version", System.getProperty("java.vm.version"));
        json.addProperty("heap_max_bytes", Runtime.getRuntime().maxMemory());
        JsonArray args = new JsonArray();
        for (String argument : runtime.getInputArguments()) {
            args.add(argument);
        }
        json.add("jvm_arguments", args);
        json.addProperty("mod_version_expected", "0.1.0-beta.1");
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            json.addProperty("physical_memory_bytes", sunOs.getTotalMemorySize());
        }
        return json;
    }

    private JsonObject identityJson() {
        JsonObject json = new JsonObject();
        json.addProperty("fluid_pipes", identityFluid);
        json.addProperty("item_pipes", identityItem);
        json.addProperty("covers", identityCovers);
        json.addProperty("processing_machines", identityMachines);
        json.addProperty("energy_converters", identityConverters);
        json.addProperty("multiblocks", identityMultiblocks);
        json.addProperty("petroleum_chains", identityPetroleum);
        json.addProperty("pipes_total", identityFluid + identityItem);
        return json;
    }

    private String identitySummary() {
        return identityJson().toString();
    }

    private static JsonArray toIntArray(List<Integer> values) {
        JsonArray array = new JsonArray();
        for (int value : values) {
            array.add(value);
        }
        return array;
    }

    private static BlockState pipeState(
            AbstractPipeBlock block, List<Direction> connections) {
        BlockState state = block.defaultBlockState();
        for (Direction direction : connections) {
            state = state.setValue(
                    AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                    true);
        }
        return state;
    }

    private static final class TickProbe {
        private final List<Long> values = new ArrayList<>();
        private long lastNanos;
        private boolean enabled;
        private boolean registered;

        void enable() {
            if (!registered) {
                NeoForge.EVENT_BUS.addListener(this::onTick);
                registered = true;
            }
            enabled = true;
            lastNanos = System.nanoTime();
        }

        void disable() {
            enabled = false;
        }

        void reset() {
            values.clear();
            lastNanos = 0L;
        }

        void onTick(ServerTickEvent.Post event) {
            if (!enabled) {
                return;
            }
            long now = System.nanoTime();
            if (lastNanos > 0L) {
                values.add(now - lastNanos);
            }
            lastNanos = now;
        }

        List<Long> values() {
            return values;
        }

        long max() {
            long max = 0L;
            for (long value : values) {
                if (value > max) {
                    max = value;
                }
            }
            return max;
        }

        long percentile(int percentile) {
            if (values.isEmpty()) {
                return 0L;
            }
            ArrayList<Long> sorted = new ArrayList<>(values);
            sorted.sort(Long::compareTo);
            int index = (int) Math.ceil(percentile / 100.0d * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
        }

        JsonArray toJson() {
            JsonArray array = new JsonArray();
            for (long value : values) {
                array.add(value);
            }
            return array;
        }
    }
}
