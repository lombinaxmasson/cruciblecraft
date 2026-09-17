package com.masson.cruciblecraft.content.storage;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            Set.of(MaterialPrefixes.PLATE, MaterialPrefixes.BLOCK),
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

    private static final List<MaterialPrefix> DROP_PREFIXES = List.of(
            MaterialPrefixes.DUST_DIV72,
            MaterialPrefixes.NUGGET,
            MaterialPrefixes.WIRE,
            MaterialPrefixes.GEM,
            MaterialPrefixes.PLATE,
            MaterialPrefixes.TINY_PLATE_GEM,
            MaterialPrefixes.TINY_CRUSHED_ORE,
            MaterialPrefixes.TINY_WASHED_CRUSHED_ORE,
            prefix("tiny_centrifuged_crushed_ore"),
            MaterialPrefixes.RAW_ORE);

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
        for (MaterialPrefix drop : DROP_PREFIXES) {
            if (!family.contains(drop) || !ModItems.hasMaterialItem(materialId, drop)) {
                continue;
            }
            long unit = drop.units();
            long count = partialUnits / unit;
            if (count <= 0L) {
                continue;
            }
            return new ItemStack(
                    ModItems.materialItem(materialId, drop).get(),
                    (int) Math.min(count, Integer.MAX_VALUE));
        }
        for (MaterialPrefix drop : family) {
            if (!ModItems.hasMaterialItem(materialId, drop)) {
                continue;
            }
            long unit = drop.units();
            long count = partialUnits / unit;
            if (count <= 0L) {
                continue;
            }
            return new ItemStack(
                    ModItems.materialItem(materialId, drop).get(),
                    (int) Math.min(count, Integer.MAX_VALUE));
        }
        return ItemStack.EMPTY;
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
