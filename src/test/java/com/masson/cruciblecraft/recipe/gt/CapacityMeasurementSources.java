package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Temporary lazy rows for the capacity cold start. They are appended only
 * while {@code cruciblecraft.capacityMeasurementRows} is set, stay out of
 * the live datapack, and are not registered as player recipes.
 *
 * <p>Sluice has no live compact group, so these rows do not force a
 * cross-group signature pass over an existing map. Each shard holds
 * {@link CompactRecipeShardRouter#HARD_SHARD_CEILING} rows of one simple
 * item, and the water amount inside that group keeps signatures distinct.
 */
public final class CapacityMeasurementSources {
    public static final int DEFAULT_TARGET_LAZY_ROWS = 600_000;
    /**
     * Live compact families include eager rows. Subtracting this cushion
     * before the target makes the added lazy count land above 600,000
     * without a second publication.
     */
    private static final int ASSUMED_EAGER_COMPACT_ROWS = 80_000;
    private static final int SHARD_GROUP = CompactRecipeShardRouter.HARD_SHARD_CEILING;
    private static final int FLUID_AMOUNT_BASE = 3_873;
    private static final ResourceLocation GROUP = ResourceLocation.fromNamespaceAndPath(
            CrucibleCraft.MODID, "capacity_measurement");

    private CapacityMeasurementSources() {}

    public static List<CompactRecipeFamilySource> append(
            List<CompactRecipeFamilySource> sources) {
        int existing = 0;
        for (CompactRecipeFamilySource source : sources) {
            existing += logicalRows(source);
        }
        int estimatedLazy = Math.max(0, existing - ASSUMED_EAGER_COMPACT_ROWS);
        int toAdd = Math.max(0, targetLazyRows() - estimatedLazy);
        if (toAdd == 0) {
            CrucibleCraft.LOGGER.info(
                    "Capacity measurement rows: existingCompact={} estimatedLazy={} added=0",
                    existing,
                    estimatedLazy);
            return sources;
        }
        List<CompactRecipeFamilySource> appended = new ArrayList<>(
                sources.size() + holderCount(toAdd));
        appended.addAll(sources);
        appended.addAll(holders(toAdd));
        CrucibleCraft.LOGGER.info(
                "Capacity measurement rows: existingCompact={} estimatedLazy={} added={}",
                existing,
                estimatedLazy,
                toAdd);
        return appended;
    }

    @SuppressWarnings("unchecked")
    public static void putPolicy(Map<?, ?> policies) {
        ((Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>)
                        policies)
                .put(
                        new PublicationGroupKey(ModRecipeMaps.SLUICE.id(), GROUP),
                        CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(0));
    }

    static int targetLazyRows() {
        String value = System.getProperty("cruciblecraft.capacityMeasurementRows");
        if (value == null || value.isBlank() || "false".equalsIgnoreCase(value)) {
            return 0;
        }
        if ("true".equalsIgnoreCase(value)) {
            return DEFAULT_TARGET_LAZY_ROWS;
        }
        return Integer.parseInt(value);
    }

    private static int logicalRows(CompactRecipeFamilySource source) {
        return source.definition().matrix()
                .map(matrix -> matrix.rows().size())
                .orElseGet(() -> source.definition().relations().size());
    }

    private static int holderCount(int rows) {
        int ceiling = CompactRecipeWireLimits.DECODE_RELATIONS_CEILING;
        return (rows + ceiling - 1) / ceiling;
    }

    private static List<CompactRecipeFamilySource> holders(int rows) {
        int buckets = (rows + SHARD_GROUP - 1) / SHARD_GROUP;
        List<Item> items = routeItems(buckets);
        int ceiling = CompactRecipeWireLimits.DECODE_RELATIONS_CEILING;
        int holders = holderCount(rows);
        List<CompactRecipeFamilySource> sources = new ArrayList<>(holders);
        for (int holder = 0; holder < holders; holder++) {
            int start = holder * ceiling;
            int count = Math.min(ceiling, rows - start);
            sources.add(new CompactRecipeFamilySource(
                    ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            "capacity_measure/sluice/" + holder),
                    definition(holder, start, count, items)));
        }
        return sources;
    }

    private static List<Item> routeItems(int buckets) {
        List<Item> items = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item != Items.AIR) {
                items.add(item);
            }
        });
        items.sort(Comparator.comparing(item -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            return id == null ? "" : id.toString();
        }));
        if (items.size() < buckets) {
            throw new IllegalStateException(
                    "Capacity measurement needs " + buckets
                            + " distinct items but the registry has " + items.size());
        }
        return items.subList(0, buckets);
    }

    private static CompactGTRecipeFamilyDefinition definition(
            int holder, int start, int count, List<Item> items) {
        int firstBucket = start / SHARD_GROUP;
        int lastBucket = (start + count - 1) / SHARD_GROUP;
        List<List<Ingredient>> inputs = new ArrayList<>(lastBucket - firstBucket + 1);
        for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
            inputs.add(List.of(Ingredient.of(items.get(bucket))));
        }
        List<CompactGTRecipeFamilyDefinition.FluidIo> fluids = new ArrayList<>(SHARD_GROUP);
        for (int offset = 0; offset < SHARD_GROUP; offset++) {
            fluids.add(new CompactGTRecipeFamilyDefinition.FluidIo(
                    List.of(new FluidStack(Fluids.WATER, FLUID_AMOUNT_BASE + offset)),
                    List.of()));
        }
        List<CompactGTRecipeFamilyDefinition.MatrixRow> matrixRows = new ArrayList<>(count);
        for (int offset = 0; offset < count; offset++) {
            int index = start + offset;
            matrixRows.add(new CompactGTRecipeFamilyDefinition.MatrixRow(
                    index / SHARD_GROUP - firstBucket,
                    0,
                    index % SHARD_GROUP,
                    ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            "capacity_measure/row/" + index),
                    index));
        }
        CompactGTRecipeFamilyDefinition.SharedSpec shared =
                new CompactGTRecipeFamilyDefinition.SharedSpec(
                        32,
                        16L,
                        0L,
                        true,
                        List.of(1),
                        List.of(ItemInputAction.CONSUME),
                        List.of(GTRecipe.GUARANTEED_CHANCE),
                        "capacity_measurement",
                        "capacity_measurement#sluice");
        CompactGTRecipeFamilyDefinition.MatrixDicts dicts =
                new CompactGTRecipeFamilyDefinition.MatrixDicts(
                        inputs,
                        List.of(List.of(new ItemStack(Items.IRON_NUGGET))),
                        fluids);
        return new CompactGTRecipeFamilyDefinition(
                "capacity_measurement#sluice_" + holder,
                ModRecipeMaps.SLUICE.id(),
                "capacity-measurement",
                List.of(),
                Optional.empty(),
                Optional.of(GROUP),
                Optional.of(new CompactGTRecipeFamilyDefinition.AuthoredMatrixV1(
                        shared, dicts, matrixRows)));
    }
}
