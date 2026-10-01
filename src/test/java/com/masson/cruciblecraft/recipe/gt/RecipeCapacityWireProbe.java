package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * Encodes the live {@code update_recipes} packet body and a synthetic
 * add-on that brings logical rows to {@link #TARGET_LOGICAL_ROWS}.
 * Stub payload estimates are recorded beside the real encode and are not
 * the sync proof.
 */
public final class RecipeCapacityWireProbe {
    public static final int TARGET_LOGICAL_ROWS = 600_000;
    private static final Path REPORT = Path.of(
            "tools/waves/recipe/gt6-recipe-capacity-expansion/live_wire.json");

    private RecipeCapacityWireProbe() {}

    public static Report measure(MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        Collection<RecipeHolder<?>> holders = manager.getRecipes();
        List<RecipeHolder<?>> recipes = new ArrayList<>(holders);
        RegistryAccess registries = server.registryAccess();
        Map<String, long[]> bySerializer = new LinkedHashMap<>();
        for (RecipeHolder<?> holder : recipes) {
            String serializer = serializerId(holder);
            long[] row = bySerializer.computeIfAbsent(serializer, ignored -> new long[2]);
            row[0]++;
            row[1] += encodeRecipe(holder, registries);
        }
        long livePacketBytes = encodePacket(recipes, registries);
        CrucibleCraft.LOGGER.info(
                "Recipe capacity live packet bytes={} holders={}",
                livePacketBytes,
                recipes.size());
        GTRecipeMapLoader.PublicationMetrics metrics =
                GTRecipeMapLoader.lastPublicationMetrics();
        int lazy = metrics.lazyLogicalRecipes();
        int gap = Math.max(0, TARGET_LOGICAL_ROWS - lazy);
        long syntheticBytes = BulkCapacitySyntheticLoadHarness.encodeExtruderRows(
                registries, gap);
        long linear = lazy <= 0 ? 0L : livePacketBytes * TARGET_LOGICAL_ROWS / lazy;
        GTRecipeMapLoader.CompactLoadLookupMetrics lookup =
                GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        return new Report(
                lazy,
                metrics.eagerPublishedRecipes(),
                metrics.reloadMillis(),
                metrics.compactLoadExtruderSyncBytes() + metrics.compactFamilySyncBytes(),
                recipes.size(),
                livePacketBytes,
                gap,
                syntheticBytes,
                livePacketBytes,
                linear,
                lookup.p95Nanos(),
                lookup.p95Candidates(),
                bySerializer);
    }

    public static Path write(Report report) throws IOException {
        Path cwd = Path.of("").toAbsolutePath();
        Path repo = cwd.getFileName().toString().startsWith("run")
                ? cwd.getParent()
                : cwd;
        Path output = repo.resolve(REPORT);
        Files.createDirectories(output.getParent());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(output, gson.toJson(report.document()));
        CrucibleCraft.LOGGER.info(
                "Recipe capacity wire: lazy={} livePacketBytes={} syntheticBytes={} "
                        + "projected={} linear600k={} reloadMs={} lookupP95Ns={} report={}",
                report.lazyLogicalRecipes(),
                report.livePacketBytes(),
                report.syntheticExtruderBytes(),
                report.projectedPacketPlusSyntheticBytes(),
                report.linear600kBytes(),
                report.reloadMillis(),
                report.lookupP95Nanos(),
                output);
        return output;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static long encodeRecipe(RecipeHolder<?> holder, RegistryAccess registries) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        try {
            RecipeSerializer serializer = holder.value().getSerializer();
            serializer.streamCodec().encode(buffer, holder.value());
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }

    private static long encodePacket(
            List<RecipeHolder<?>> recipes,
            RegistryAccess registries) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        try {
            ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(
                    buffer, new ClientboundUpdateRecipesPacket(recipes));
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }

    private static String serializerId(RecipeHolder<?> holder) {
        ResourceLocation id = BuiltInRegistries.RECIPE_SERIALIZER.getKey(
                holder.value().getSerializer());
        return id == null ? holder.value().getSerializer().getClass().getName() : id.toString();
    }

    public record Report(
            int lazyLogicalRecipes,
            int eagerPublishedRecipes,
            long reloadMillis,
            long stubSyncBytes,
            int recipeHolders,
            long livePacketBytes,
            int syntheticExtruderRows,
            long syntheticExtruderBytes,
            long projectedPacketPlusSyntheticBytes,
            long linear600kBytes,
            long lookupP95Nanos,
            long lookupP95Candidates,
            Map<String, long[]> bytesBySerializer) {
        private Map<String, Object> document() {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("schema_version", 1);
            root.put("kind", "live_update_recipes_packet");
            root.put("target_logical_rows", TARGET_LOGICAL_ROWS);
            root.put("sync_budget_bytes", ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES);
            root.put("lazy_logical_recipes", lazyLogicalRecipes);
            root.put("eager_published_recipes", eagerPublishedRecipes);
            root.put("reload_ms", reloadMillis);
            root.put("stub_sync_bytes", stubSyncBytes);
            root.put("recipe_holders", recipeHolders);
            root.put("live_packet_bytes", livePacketBytes);
            root.put("synthetic_extruder_rows", syntheticExtruderRows);
            root.put("synthetic_extruder_bytes", syntheticExtruderBytes);
            root.put(
                    "projected_packet_plus_synthetic_bytes",
                    projectedPacketPlusSyntheticBytes);
            root.put("linear_600k_bytes", linear600kBytes);
            root.put("lookup_p95_nanos", lookupP95Nanos);
            root.put("lookup_p95_candidates", lookupP95Candidates);
            root.put(
                    "lookup_p95_within_verification_budget",
                    lookupP95Nanos
                            <= ModProcessingMachines
                                    .VERIFICATION_RECIPE_LOOKUP_P95_BUDGET_NS);
            root.put(
                    "live_packet_within_budget",
                    livePacketBytes <= ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES);
            root.put(
                    "projection_within_budget",
                    projectedPacketPlusSyntheticBytes
                            <= ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES);
            root.put(
                    "linear_600k_within_budget",
                    linear600kBytes <= ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES);
            List<Map<String, Object>> serializers = new ArrayList<>();
            bytesBySerializer.entrySet().stream()
                    .sorted(Comparator.comparingLong(
                            (Map.Entry<String, long[]> entry) -> entry.getValue()[1])
                            .reversed())
                    .forEach(entry -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("serializer", entry.getKey());
                        row.put("holders", entry.getValue()[0]);
                        row.put("body_bytes", entry.getValue()[1]);
                        serializers.add(row);
                    });
            root.put("by_serializer", serializers);
            root.put(
                    "note",
                    "live_packet_bytes is the login ClientboundUpdateRecipesPacket after "
                            + "compact family holders are omitted. Those bodies are on-demand "
                            + "and are not part of the login sync budget. stub_sync_bytes still "
                            + "includes the family stub and is not the sync proof. "
                            + "projected_packet_plus_synthetic_bytes is the login packet; "
                            + "synthetic_extruder_bytes is an on-demand size, not added to login. "
                            + "Dedicated client cold start is still required to close the card.");
            return root;
        }
    }
}
