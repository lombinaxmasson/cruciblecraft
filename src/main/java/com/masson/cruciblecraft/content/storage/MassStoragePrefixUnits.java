package com.masson.cruciblecraft.content.storage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.content.item.PrefixMaterialItem;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code MultiTileEntityMassStorage} prefix-unit merge.
 *
 * <p>Same-material members of one family convert into the already stored
 * prefix via leftover {@code mPartialUnits}. Exact item identity still wins.
 *
 * <p>CC folds {@code blockIngot}/{@code blockGem}/{@code blockPlate} into
 * {@code block}. The folded prefix joins one family from the material
 * (ingot, then gem, then plate). It never joins raw-ore; {@code blockRaw}
 * is unmapped.
 */
public final class MassStoragePrefixUnits {
    static final Set<MaterialPrefix> DUST_FAMILY = Set.of(
            MaterialPrefixes.DUST,
            MaterialPrefixes.SMALL_DUST,
            MaterialPrefixes.TINY_DUST,
            MaterialPrefixes.DUST_DIV72,
            MaterialPrefixes.STORAGE_DUST);
    static final Set<MaterialPrefix> INGOT_FAMILY = Set.of(
            MaterialPrefixes.INGOT,
            MaterialPrefixes.NUGGET,
            MaterialPrefixes.CHUNK,
            MaterialPrefixes.BILLET,
            MaterialPrefixes.STORAGE_INGOT);
    static final Set<MaterialPrefix> WIRE_FAMILY = Set.of(
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
            MaterialPrefixes.HEXADECUPLE_WIRE);
    static final Set<MaterialPrefix> GEM_FAMILY = Set.of(MaterialPrefixes.GEM);
    static final Set<MaterialPrefix> PLATE_FAMILY = Set.of(
            MaterialPrefixes.PLATE,
            MaterialPrefixes.STORAGE_PLATE);
    static final Set<MaterialPrefix> PLATE_GEM_FAMILY = Set.of(
            MaterialPrefixes.PLATE_GEM,
            MaterialPrefixes.TINY_PLATE_GEM);
    static final Set<MaterialPrefix> CRUSHED_FAMILY = Set.of(
            MaterialPrefixes.CRUSHED_ORE,
            MaterialPrefixes.TINY_CRUSHED_ORE);
    static final Set<MaterialPrefix> WASHED_FAMILY = Set.of(
            MaterialPrefixes.WASHED_CRUSHED_ORE,
            MaterialPrefixes.TINY_WASHED_CRUSHED_ORE);
    static final Set<MaterialPrefix> CENTRIFUGED_FAMILY = Set.of(
            MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
            prefix("tiny_centrifuged_crushed_ore"));
    static final Set<MaterialPrefix> RAW_ORE_FAMILY = Set.of(MaterialPrefixes.RAW_ORE);

    private static final List<Set<MaterialPrefix>> FAMILIES = List.of(
            DUST_FAMILY,
            INGOT_FAMILY,
            WIRE_FAMILY,
            GEM_FAMILY,
            PLATE_FAMILY,
            PLATE_GEM_FAMILY,
            CRUSHED_FAMILY,
            WASHED_FAMILY,
            CENTRIFUGED_FAMILY,
            RAW_ORE_FAMILY);

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
     * prefix family, leftover partials are still below one stored unit, and
     * the stored item is not a blacklisted vanilla glass stack.
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
        if (prefixMergeBlocked(stored, storedData)) {
            return 0L;
        }
        MaterialDefinition material = MaterialCatalog.contains(storedData.materialId())
                ? MaterialCatalog.require(storedData.materialId())
                : null;
        Set<MaterialPrefix> family = sharedFamily(
                storedData.form(), incomingData.form(), material);
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
        MaterialDefinition material = MaterialCatalog.contains(entry.get().materialId())
                ? MaterialCatalog.require(entry.get().materialId())
                : null;
        Set<MaterialPrefix> family = familyOf(entry.get().form(), material);
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
     * GT6 {@code OM.ingot} denomination, including {@code chunkGt} at U/4.
     * Billet is input-only; GT6 does not emit it here.
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
        if (amount >= U4 && (amount >= (long) U * 8 || amount % U4 <= amount % U9)) {
            return MaterialPrefixes.CHUNK;
        }
        return MaterialPrefixes.NUGGET;
    }

    /**
     * GT6 {@code !mBlackListed || (material != Glass && MD.MC.owns(stored))}.
     * CC has no unification blacklist table; vanilla glass is the stored-item
     * case that hits it. CC glass dust/gem still convert. Ordinary metals do not.
     */
    static boolean prefixMergeBlocked(ItemStack stored, MaterialUnits.Entry storedData) {
        if (!"glass".equals(storedData.materialId())) {
            return false;
        }
        Item item = stored.getItem();
        if (item instanceof MaterialFormItem || item instanceof PrefixMaterialItem) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && "minecraft".equals(id.getNamespace());
    }

    static Set<MaterialPrefix> blockFamily(MaterialDefinition material) {
        if (material == null) {
            return Set.of();
        }
        return blockFamily(
                material.generationFlagIds(),
                material.gt6Metadata()
                        .map(GT6MaterialMetadata::generationTags)
                        .orElse(List.of()),
                material.forms());
    }

    static Set<MaterialPrefix> blockFamily(
            Collection<String> generationFlagIds,
            Collection<String> generationTags,
            Collection<MaterialPrefix> forms) {
        if (ingotBased(generationFlagIds, generationTags, forms)) {
            return INGOT_FAMILY;
        }
        if (gemBased(generationFlagIds, generationTags, forms)) {
            return GEM_FAMILY;
        }
        if (plateBased(generationFlagIds, generationTags, forms)) {
            return PLATE_FAMILY;
        }
        return Set.of();
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
        if (amount >= U4 && (amount >= (long) U * 8 || amount % U4 <= amount % U9)) {
            ItemStack chunk = tryMaterial(
                    materialId, MaterialPrefixes.CHUNK, (amount * 4L) / U);
            if (!chunk.isEmpty()) {
                return chunk;
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

    static Set<MaterialPrefix> familyOf(
            MaterialPrefix prefix, MaterialDefinition material) {
        Set<MaterialPrefix> family = familyOf(prefix);
        if (!family.isEmpty()) {
            return family;
        }
        if (MaterialPrefixes.BLOCK.equals(prefix)) {
            return blockFamily(material);
        }
        return Set.of();
    }

    private static Set<MaterialPrefix> sharedFamily(
            MaterialPrefix stored,
            MaterialPrefix incoming,
            MaterialDefinition material) {
        for (Set<MaterialPrefix> family : FAMILIES) {
            if (family.contains(stored) && family.contains(incoming)) {
                return family;
            }
        }
        Set<MaterialPrefix> blockFamily = blockFamily(material);
        if (blockFamily.isEmpty()) {
            return Set.of();
        }
        boolean storedBlock = MaterialPrefixes.BLOCK.equals(stored);
        boolean incomingBlock = MaterialPrefixes.BLOCK.equals(incoming);
        if (storedBlock && incomingBlock) {
            return blockFamily;
        }
        if (storedBlock && blockFamily.contains(incoming)) {
            return blockFamily;
        }
        if (incomingBlock && blockFamily.contains(stored)) {
            return blockFamily;
        }
        return Set.of();
    }

    private static boolean ingotBased(
            Collection<String> generationFlagIds,
            Collection<String> generationTags,
            Collection<MaterialPrefix> forms) {
        return generationFlagIds.contains("cruciblecraft:generates_ingot")
                || generationTags.contains("ITEMGENERATOR.INGOTS")
                || forms.contains(MaterialPrefixes.INGOT);
    }

    private static boolean gemBased(
            Collection<String> generationFlagIds,
            Collection<String> generationTags,
            Collection<MaterialPrefix> forms) {
        return generationFlagIds.contains("gt6:itemgenerator/gems")
                || generationFlagIds.contains("cruciblecraft:generates_gem")
                || generationTags.contains("ITEMGENERATOR.GEMS")
                || forms.contains(MaterialPrefixes.GEM);
    }

    private static boolean plateBased(
            Collection<String> generationFlagIds,
            Collection<String> generationTags,
            Collection<MaterialPrefix> forms) {
        return generationFlagIds.contains("cruciblecraft:generates_plate")
                || generationTags.contains("ITEMGENERATOR.PLATES")
                || forms.contains(MaterialPrefixes.PLATE);
    }
}
