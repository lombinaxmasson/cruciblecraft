package com.masson.cruciblecraft.recipe.gt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.connection.ConnectionType;

import io.netty.buffer.Unpooled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic compact-matrix loads for the bulk capacity gate.
 *
 * <p>Rows stay in test code. They are not registered and not written into the
 * live datapack. Each holder is one {@code matrix_v1} family at the existing
 * per-holder row ceiling. The measurement is encode size, encode time, and
 * one-holder expand time. It is not a dedicated-client EMI timing.
 */
class BulkCapacitySyntheticLoadHarness {
    private static final int[] TIERS = {100_000, 300_000, 500_000};
    private static final Path OUTPUT = Path.of(
            "tools/waves/recipe/gt6-bulk-capacity/measurements.json");
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";

    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        Assumptions.assumeTrue(
                Boolean.getBoolean("cruciblecraft.runBulkCapacityMeasurements")
                        || "1".equals(System.getenv("CRUCIBLECRAFT_BULK_CAPACITY")),
                "bulk capacity synthetic loads stay off the default test run");
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void syntheticMatrixTiersStayInsidePerHolderCeilings() throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Shape shape : Shape.values()) {
            for (int tier : TIERS) {
                rows.add(measure(shape, tier, registries));
            }
        }
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schema_version", 1);
        document.put("kind", "synthetic_compact_matrix");
        document.put("live_datapack", false);
        document.put("per_holder_row_ceiling", CompactRecipeWireLimits.DECODE_RELATIONS_CEILING);
        document.put("shard_relation_ceiling", CompactRecipeShardRouter.HARD_SHARD_CEILING);
        document.put("transport_fragment_ceiling", CompactRecipeWireLimits.MAX_TRANSPORT_FRAGMENTS);
        document.put("note",
                "Encode and one-holder expand only. Dedicated client join and EMI index time are a separate human measurement.");
        document.put("tiers", rows);
        Files.createDirectories(OUTPUT.getParent());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(OUTPUT, gson.toJson(document));
        for (Map<String, Object> row : rows) {
            int holders = (Integer) row.get("holders");
            assertTrue(holders <= CompactRecipeWireLimits.MAX_TRANSPORT_FRAGMENTS, row::toString);
            assertEquals(
                    CompactRecipeWireLimits.DECODE_RELATIONS_CEILING,
                    row.get("expanded_relations"));
        }
    }

    /** Real stream-codec bytes for synthetic extruder matrix rows. Not registered. */
    public static long encodeExtruderRows(RegistryAccess access, int logicalRows) {
        if (logicalRows <= 0) {
            return 0L;
        }
        int ceiling = CompactRecipeWireLimits.DECODE_RELATIONS_CEILING;
        int holders = (logicalRows + ceiling - 1) / ceiling;
        CompactGTRecipeFamilySerializer serializer = new CompactGTRecipeFamilySerializer();
        long encodedBytes = 0L;
        for (int holder = 0; holder < holders; holder++) {
            int start = holder * ceiling;
            int count = Math.min(ceiling, logicalRows - start);
            CompactGTRecipeFamilyEntry entry = new CompactGTRecipeFamilyEntry(
                    Shape.EXTRUDER.definition(holder, start, count));
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), access, ConnectionType.NEOFORGE);
            try {
                serializer.streamCodec().encode(buffer, entry);
                encodedBytes += buffer.readableBytes();
            } finally {
                buffer.release();
            }
        }
        return encodedBytes;
    }

    private static Map<String, Object> measure(
            Shape shape,
            int logicalRows,
            RegistryAccess access) {
        int ceiling = CompactRecipeWireLimits.DECODE_RELATIONS_CEILING;
        int holders = (logicalRows + ceiling - 1) / ceiling;
        CompactGTRecipeFamilySerializer serializer = new CompactGTRecipeFamilySerializer();
        long encodedBytes = 0L;
        long encodeNanos = 0L;
        int expanded = 0;
        long expandNanos = 0L;
        long heapBefore = usedHeap();
        for (int holder = 0; holder < holders; holder++) {
            int start = holder * ceiling;
            int count = Math.min(ceiling, logicalRows - start);
            CompactGTRecipeFamilyDefinition definition = shape.definition(holder, start, count);
            CompactGTRecipeFamilyEntry entry = new CompactGTRecipeFamilyEntry(definition);
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), access, ConnectionType.NEOFORGE);
            long started = System.nanoTime();
            serializer.streamCodec().encode(buffer, entry);
            encodeNanos += System.nanoTime() - started;
            encodedBytes += buffer.readableBytes();
            if (holder == 0) {
                CompactGTRecipeFamilyEntry decoded = serializer.streamCodec().decode(buffer);
                long expandStarted = System.nanoTime();
                expanded = decoded.definition().authoredRelations().size();
                expandNanos = System.nanoTime() - expandStarted;
                assertEquals(count, expanded);
            }
            buffer.release();
        }
        long heapAfter = usedHeap();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("shape", shape.name().toLowerCase(Locale.ROOT));
        row.put("logical_rows", logicalRows);
        row.put("holders", holders);
        row.put("encoded_bytes", encodedBytes);
        row.put("encode_ms", encodeNanos / 1_000_000L);
        row.put("expanded_relations", expanded);
        row.put("expand_one_holder_ms", expandNanos / 1_000_000L);
        row.put("heap_delta_bytes", Math.max(0L, heapAfter - heapBefore));
        return row;
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private enum Shape {
        EXTRUDER(ModRecipeMaps.EXTRUDER.id(), "gt.recipe.extruder"),
        CUTTER(ModRecipeMaps.CUTTER.id(), "gt.recipe.cutter"),
        MIXER(ModRecipeMaps.MIXER.id(), "gt.recipe.mixer");

        private final ResourceLocation target;
        private final String sourceKind;

        Shape(ResourceLocation target, String sourceKind) {
            this.target = target;
            this.sourceKind = sourceKind;
        }

        CompactGTRecipeFamilyDefinition definition(int holder, int start, int count) {
            List<CompactGTRecipeFamilyDefinition.MatrixRow> rows = new ArrayList<>(count);
            for (int offset = 0; offset < count; offset++) {
                int index = start + offset;
                int inputIdx = this == MIXER ? index % 2 : 0;
                rows.add(new CompactGTRecipeFamilyDefinition.MatrixRow(
                        inputIdx,
                        0,
                        0,
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft",
                                "bulk/" + name().toLowerCase(Locale.ROOT) + "/" + index),
                        offset));
            }
            return new CompactGTRecipeFamilyDefinition(
                    sourceKind + "#bulk_" + holder,
                    target,
                    SOURCE_REVISION,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(new CompactGTRecipeFamilyDefinition.AuthoredMatrixV1(
                            shared(),
                            dicts(),
                            rows)));
        }

        private CompactGTRecipeFamilyDefinition.SharedSpec shared() {
            int duration = this == CUTTER ? 64 : 32;
            long eu = this == MIXER ? 32L : 16L;
            return new CompactGTRecipeFamilyDefinition.SharedSpec(
                    duration,
                    eu,
                    0L,
                    true,
                    List.of(1),
                    List.of(ItemInputAction.CONSUME),
                    List.of(GTRecipe.GUARANTEED_CHANCE),
                    "SOURCE_BACKED",
                    sourceKind + "#bulk");
        }

        private CompactGTRecipeFamilyDefinition.MatrixDicts dicts() {
            List<List<Ingredient>> inputs = new ArrayList<>();
            inputs.add(List.of(Ingredient.of(Items.IRON_INGOT)));
            if (this == MIXER) {
                inputs.add(List.of(Ingredient.of(Items.REDSTONE)));
            }
            List<CompactGTRecipeFamilyDefinition.FluidIo> fluids = List.of(
                    this == EXTRUDER
                            ? new CompactGTRecipeFamilyDefinition.FluidIo(List.of(), List.of())
                            : new CompactGTRecipeFamilyDefinition.FluidIo(
                                    List.of(new FluidStack(Fluids.WATER, 1000)),
                                    List.of()));
            return new CompactGTRecipeFamilyDefinition.MatrixDicts(
                    inputs,
                    List.of(List.of(new ItemStack(Items.IRON_NUGGET))),
                    fluids);
        }
    }
}
