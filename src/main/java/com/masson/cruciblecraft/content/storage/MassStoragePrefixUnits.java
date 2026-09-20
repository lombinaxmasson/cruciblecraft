package com.masson.cruciblecraft.content.storage;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code MultiTileEntityMassStorage} prefix-unit merge.
 *
 * <p>Same-material members of one family convert into the already stored
 * prefix via leftover {@code mPartialUnits}. Exact item identity still wins.
 */
public final class MassStoragePrefixUnits {
    private static final List<Set<MaterialPrefix>> FAMILIES = List.of(
            Set.of(
                    MaterialPrefixes.DUST,
                    MaterialPrefixes.SMALL_DUST,
                    MaterialPrefixes.TINY_DUST,
                    MaterialPrefixes.DUST_DIV72,
                    MaterialPrefixes.STORAGE_DUST),
            Set.of(
                    MaterialPrefixes.INGOT,
                    MaterialPrefixes.NUGGET,
                    MaterialPrefixes.BLOCK,
                    prefix("chunk"),
                    prefix("billet")),
            Set.of(
                    MaterialPrefixes.WIRE,
                    MaterialPrefixes.DOUBLE_WIRE,
                    MaterialPrefixes.TRIPLE_WIRE,
                    MaterialPrefixes.QUADRUPLE_WIRE,
                    MaterialPrefixes.QUINTUPLE_WIRE,
                    MaterialPrefixes.SEXTUPLE_WIRE,
                    MaterialPrefixes.SEPTUPLE_WIRE,
                    MaterialPrefixes.OCTUPLE_WIRE,
                    MaterialPrefixes.NONUPLE_WIRE,
                    MaterialPrefixes.DECUPLE_WIRE,
                    MaterialPrefixes.UNDECUPLE_WIRE,
                    MaterialPrefixes.DODECUPLE_WIRE,
                    MaterialPrefixes.TREDECUPLE_WIRE,
                    MaterialPrefixes.TETRADECUPLE_WIRE,
                    MaterialPrefixes.PENTADECUPLE_WIRE,
                    MaterialPrefixes.HEXADECUPLE_WIRE),
            Set.of(MaterialPrefixes.GEM, MaterialPrefixes.BLOCK),
            Set.of(MaterialPrefixes.PLATE, MaterialPrefixes.STORAGE_PLATE),
            Set.of(MaterialPrefixes.PLATE_GEM, MaterialPrefixes.TINY_PLATE_GEM),
            Set.of(
                    MaterialPrefixes.CRUSHED_ORE,
                    MaterialPrefixes.TINY_CRUSHED_ORE),
            Set.of(
                    MaterialPrefixes.WASHED_CRUSHED_ORE,
                    MaterialPrefixes.TINY_WASHED_CRUSHED_ORE),
            Set.of(
                    MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                    prefix("tiny_centrifuged_crushed_ore")),
            Set.of(MaterialPrefixes.RAW_ORE, MaterialPrefixes.BLOCK));

    /**
     * GT6 U after CC prefix JSON normalization ({@code dust}/{@code ingot}=144).
     * Used by {@code OM.dust}/{@code OM.ingot} denomination, not by live
     * {@link MaterialPrefix#units()} so tests can run without the catalog.
     */
    static final int U = 144;
    static final int U4 = 36;
    static final int U9 = 16;
    static final int U72 = 2;
    static final int U_BLOCK = 1296;

    private MassStoragePrefixUnits() {}

    public static boolean sameFamily(ItemStack stored, ItemStack incoming) {
        return unitAmount(stored, incoming, 0L) > 0L;
    }

    /**
     * GT6 {@code getUnitAmount}: 0 unless both stacks share a material and
     * prefix family, and leftover partials are still below one stored unit.
     */
    public static long unitAmount(
            ItemStack stored, ItemStack incoming, long partialUnits) {
        if (!MaterialPrefixCatalog.isBootstrapped()
                || stored.isEmpty()
                || incoming.isEmpty()) {
            return 0L;
        }
        Optional<MaterialUnits.Entry> storedEntry = MaterialUnits.resolve(stored);
        Optional<MaterialUnits.Entry> incomingEntry = MaterialUnits.resolve(incoming);
        if (storedEntry.isEmpty() || incomingEntry.isEmpty()) {
            return 0L;
        }
        MaterialUnits.Entry storedData = storedEntry.get();
        MaterialUnits.Entry incomingData = incomingEntry.get();
        if (!storedData.materialId().equals(incomingData.materialId())) {
            return 0L;
        }
        Set<MaterialPrefix> family = sharedFamily(storedData.form(), incomingData.form());
        if (family.isEmpty()) {
            return 0L;
        }
        long storedUnit = storedData.form().units();
        if (partialUnits >= storedUnit) {
            return 0L;
        }
        return incomingData.form().units();
    }

    public static ItemStack partialStack(ItemStack stored, long partialUnits) {
        if (!MaterialPrefixCatalog.isBootstrapped()
                || stored.isEmpty()
                || partialUnits <= 0L) {
            return ItemStack.EMPTY;
        }
        Optional<MaterialUnits.Entry> entry = MaterialUnits.resolve(stored);
        if (entry.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Set<MaterialPrefix> family = familyOf(entry.get().form());
        if (family.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String materialId = entry.get().materialId();
        if (family.contains(MaterialPrefixes.DUST)
                || family.contains(MaterialPrefixes.STORAGE_DUST)) {
            return omDust(materialId, partialUnits);
        }
        if (family.contains(MaterialPrefixes.INGOT)
                || family.contains(MaterialPrefixes.NUGGET)) {
            return omIngot(materialId, partialUnits);
        }
        if (family.contains(MaterialPrefixes.WIRE)) {
            return tryMaterial(
                    materialId,
                    MaterialPrefixes.WIRE,
                    partialUnits / Math.max(1, MaterialPrefixes.WIRE.units()));
        }
        if (family.contains(MaterialPrefixes.GEM)) {
            return tryMaterial(
                    materialId,
                    MaterialPrefixes.GEM,
                    partialUnits / Math.max(1, MaterialPrefixes.GEM.units()));
        }
        if (family.contains(MaterialPrefixes.PLATE)
                || family.contains(MaterialPrefixes.STORAGE_PLATE)) {
            return tryMaterial(
                    materialId,
                    MaterialPrefixes.PLATE,
                    partialUnits / Math.max(1, MaterialPrefixes.PLATE.units()));
        }
        if (family.contains(MaterialPrefixes.TINY_PLATE_GEM)
                || family.contains(MaterialPrefixes.PLATE_GEM)) {
            return tryMaterial(
                    materialId,
                    MaterialPrefixes.PLATE_GEM,
                    partialUnits / Math.max(1, MaterialPrefixes.PLATE_GEM.units()));
        }
        if (family.contains(MaterialPrefixes.TINY_CRUSHED_ORE)
                || family.contains(MaterialPrefixes.CRUSHED_ORE)) {
            return tryMaterial(
                    materialId,
                    MaterialPrefixes.TINY_CRUSHED_ORE,
                    partialUnits / Math.max(1, MaterialPrefixes.TINY_CRUSHED_ORE.units()));
        }
        if (family.contains(MaterialPrefixes.TINY_WASHED_CRUSHED_ORE)
                || family.contains(MaterialPrefixes.WASHED_CRUSHED_ORE)) {
            return tryMaterial(
                    materialId,
                    MaterialPrefixes.TINY_WASHED_CRUSHED_ORE,
                    partialUnits
                            / Math.max(1, MaterialPrefixes.TINY_WASHED_CRUSHED_ORE.units()));
        }
        if (family.contains(prefix("tiny_centrifuged_crushed_ore"))
                || family.contains(MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE)) {
            MaterialPrefix tiny = prefix("tiny_centrifuged_crushed_ore");
            return tryMaterial(materialId, tiny, partialUnits / Math.max(1, tiny.units()));
        }
        if (family.contains(MaterialPrefixes.RAW_ORE)) {
            return tryMaterial(
                    materialId,
                    MaterialPrefixes.RAW_ORE,
                    partialUnits / Math.max(1, MaterialPrefixes.RAW_ORE.units()));
        }
        return ItemStack.EMPTY;
    }

    /**
     * GT6 {@code OM.dust} denomination assuming every dust prefix exists.
     * Largest unit first, with the original remainder heuristics.
     */
    public static MaterialPrefix dustDenomination(long amount) {
        if (amount < U72) {
            return null;
        }
        if (amount >= (long) U * 72) {
            return MaterialPrefixes.STORAGE_DUST;
        }
        if (amount >= U && (amount >= (long) U * 16 || amount % U == 0)) {
            return MaterialPrefixes.DUST;
        }
        if (amount >= U4 && (amount >= (long) U * 8 || amount % U4 <= amount % U9)) {
            return MaterialPrefixes.SMALL_DUST;
        }
        if (amount >= U9 && (amount >= U || amount % U9 <= amount % U72)) {
            return MaterialPrefixes.TINY_DUST;
        }
        return MaterialPrefixes.DUST_DIV72;
    }

    /**
     * GT6 {@code OM.ingot} denomination. CC has no {@code chunkGt} at U/4, so
     * that step falls through to nuggets.
     */
    public static MaterialPrefix ingotDenomination(long amount) {
        if (amount < U9) {
            return null;
        }
        if (amount >= (long) U * 72) {
            return MaterialPrefixes.BLOCK;
        }
        if (amount >= U && (amount >= (long) U * 16 || amount % U == 0)) {
            return MaterialPrefixes.INGOT;
        }
        return MaterialPrefixes.NUGGET;
    }

    private static ItemStack omDust(String materialId, long amount) {
        if (amount < U72) {
            return ItemStack.EMPTY;
        }
        if (amount >= (long) U * 72) {
            ItemStack block = tryMaterial(materialId, MaterialPrefixes.STORAGE_DUST, amount / U_BLOCK);
            if (!block.isEmpty()) {
                return block;
            }
        }
        if (amount >= U && (amount >= (long) U * 16 || amount % U == 0)) {
            ItemStack dust = tryMaterial(materialId, MaterialPrefixes.DUST, amount / U);
            if (!dust.isEmpty()) {
                return dust;
            }
        }
        if (amount >= U4 && (amount >= (long) U * 8 || amount % U4 <= amount % U9)) {
            ItemStack small = tryMaterial(materialId, MaterialPrefixes.SMALL_DUST, amount / U4);
            if (!small.isEmpty()) {
                return small;
            }
        }
        if (amount >= U9 && (amount >= U || amount % U9 <= amount % U72)) {
            ItemStack tiny = tryMaterial(materialId, MaterialPrefixes.TINY_DUST, amount / U9);
            if (!tiny.isEmpty()) {
                return tiny;
            }
        }
        return tryMaterial(materialId, MaterialPrefixes.DUST_DIV72, amount / U72);
    }

    private static ItemStack omIngot(String materialId, long amount) {
        if (amount < U9) {
            return ItemStack.EMPTY;
        }
        if (amount >= (long) U * 72) {
            ItemStack block = tryMaterial(materialId, MaterialPrefixes.BLOCK, amount / U_BLOCK);
            if (!block.isEmpty()) {
                return block;
            }
        }
        if (amount >= U && (amount >= (long) U * 16 || amount % U == 0)) {
            ItemStack ingot = tryMaterial(materialId, MaterialPrefixes.INGOT, amount / U);
            if (!ingot.isEmpty()) {
                return ingot;
            }
        }
        return tryMaterial(materialId, MaterialPrefixes.NUGGET, amount / U9);
    }

    private static ItemStack tryMaterial(
            String materialId, MaterialPrefix prefix, long count) {
        if (count <= 0L || !ModItems.hasMaterialItem(materialId, prefix)) {
            return ItemStack.EMPTY;
        }
        return MaterialLookup.tryStack(
                        materialId, prefix, (int) Math.min(count, Integer.MAX_VALUE))
                .orElse(ItemStack.EMPTY);
    }

    private static MaterialPrefix prefix(String path) {
        return new MaterialPrefix("cruciblecraft:" + path);
    }

    static Set<MaterialPrefix> familyOf(MaterialPrefix prefix) {
        for (Set<MaterialPrefix> family : FAMILIES) {
            if (family.contains(prefix)) {
                return family;
            }
        }
        return Set.of();
    }

    private static Set<MaterialPrefix> sharedFamily(
            MaterialPrefix stored, MaterialPrefix incoming) {
        for (Set<MaterialPrefix> family : FAMILIES) {
            if (family.contains(stored) && family.contains(incoming)) {
                return family;
            }
        }
        return Set.of();
    }
}
