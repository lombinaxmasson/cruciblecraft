package com.masson.cruciblecraft.recipe.crafting;

import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.storage.MassStoragePrefixUnits;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code OP.rockGt} crushing and furnace yields.
 *
 * <p>Crusher / mortar {@code RecipeMapHandlerPrefix} with pulverized remains is
 * {@code OM.pulverize(material, 9*U4)}. Anvil shredding is 9 {@code dustSmall}
 * of {@code mTargetPulver}. Every live rock material currently has pulver
 * {@code cc_units = U}, so both are 9 small dust of the pulver target.
 *
 * <p>Furnace is {@code Listener_Furnace_Smelting(-1, T)}: smelting target
 * scaled by the rock prefix, then {@code OM.ingot}. Netherrack rock is the
 * {@code Loader_Recipes_Other} special that becomes nether-brick rock.
 */
public final class RockGtProcessing {
    public static final MaterialPrefix ROCK =
            new MaterialPrefix("cruciblecraft:rock");
    private static final long U = MaterialPrefixes.INGOT.units();
    private static final long ROCK_UNITS = 9L * MaterialPrefixes.SMALL_DUST.units();

    private RockGtProcessing() {}

    public static long rockUnits() {
        return ROCK_UNITS;
    }

    /** GT6 {@code UT.Code.units}. */
    public static long units(
            long amount, long originalUnit, long targetUnit, boolean roundUp) {
        if (targetUnit == 0L) {
            return 0L;
        }
        if (originalUnit == targetUnit || originalUnit == 0L) {
            return amount;
        }
        long original = originalUnit;
        long target = targetUnit;
        if (original % target == 0L) {
            original /= target;
            target = 1L;
        } else if (target % original == 0L) {
            target /= original;
            original = 1L;
        }
        long product = amount * target;
        long result = product / original;
        if (roundUp && product % original > 0L) {
            result += 1L;
        }
        return Math.max(0L, result);
    }

    public static Optional<PulverPlan> pulverPlan(MaterialDefinition material) {
        if (material.hasMaterialTag("ATOMIC.ANTIMATTER")
                || material.hasMaterialTag("PROPERTIES.INVALID_MATERIAL")) {
            return Optional.empty();
        }
        GT6MaterialMetadata.MaterialAmount pulver = target(material, "pulver")
                .orElse(null);
        if (pulver == null || pulver.ccUnits().isEmpty() || pulver.ccUnits().get() <= 0L) {
            return Optional.empty();
        }
        long dustUnits = units(ROCK_UNITS, U, pulver.ccUnits().get(), false);
        MaterialPrefix prefix = MassStoragePrefixUnits.dustDenomination(dustUnits);
        if (prefix == null) {
            return Optional.empty();
        }
        int count = dustCount(prefix, dustUnits);
        if (count <= 0) {
            return Optional.empty();
        }
        return Optional.of(new PulverPlan(pulver.material(), prefix, count, dustUnits));
    }

    public static Optional<ItemStack> pulverStack(MaterialDefinition material) {
        return pulverPlan(material).flatMap(plan -> MaterialLookup.tryStack(
                plan.materialId(), plan.prefix(), plan.count()));
    }

    /**
     * Crusher / mortar / anvil duration when the handler duration is 0:
     * {@code units(rockUnits, U, 16 * (1 + toolQuality), true)}.
     */
    public static int machineDuration(MaterialDefinition material) {
        int quality = material.gt6Metadata()
                .map(metadata -> metadata.tool().quality())
                .orElse(0);
        return (int) units(ROCK_UNITS, U, 16L * (1L + quality), true);
    }

    public static Optional<FurnacePlan> furnacePlan(
            MaterialDefinition material,
            Map<String, MaterialDefinition> byId) {
        if ("netherrack".equals(material.id())) {
            return Optional.of(new FurnacePlan(
                    "nether_brick",
                    MaterialPrefixCatalog.require("rock"),
                    1,
                    ROCK_UNITS,
                    true));
        }
        if (!material.furnaceSmeltable()
                || material.hasMaterialTag("PROPERTIES.UNUSED_MATERIAL")
                || material.hasMaterialTag("ATOMIC.ANTIMATTER")
                || material.hasMaterialTag("PROPERTIES.INVALID_MATERIAL")) {
            return Optional.empty();
        }
        GT6MaterialMetadata.MaterialAmount smelting = target(material, "smelting")
                .orElse(null);
        if (smelting == null
                || smelting.ccUnits().isEmpty()
                || smelting.ccUnits().get() <= 0L) {
            return Optional.empty();
        }
        MaterialDefinition smelted = byId.get(smelting.material());
        if (smelted == null) {
            return Optional.empty();
        }
        GT6MaterialMetadata.MaterialAmount solidifying = target(smelted, "solidifying")
                .orElse(null);
        String outputMaterial = solidifying == null
                ? smelted.id()
                : solidifying.material();
        long solidUnits = solidifying == null
                ? 0L
                : solidifying.ccUnits().orElse(0L);
        long inner = units(smelting.ccUnits().get(), U, solidUnits, false);
        long ingotUnits = units(inner, U, ROCK_UNITS, false);
        MaterialPrefix prefix = MassStoragePrefixUnits.ingotDenomination(ingotUnits);
        if (prefix == null) {
            return Optional.empty();
        }
        int count = ingotCount(prefix, ingotUnits);
        if (count <= 0) {
            return Optional.empty();
        }
        return Optional.of(new FurnacePlan(
                outputMaterial, prefix, count, ingotUnits, false));
    }

    public static Optional<ItemStack> furnaceStack(
            MaterialDefinition material,
            Map<String, MaterialDefinition> byId) {
        return furnacePlan(material, byId).flatMap(plan -> {
            if (plan.netherrackSpecial()) {
                return MaterialLookup.tryStack(
                        plan.materialId(), plan.prefix(), plan.count());
            }
            ItemStack stack = MassStoragePrefixUnits.ingot(
                    plan.materialId(), plan.units());
            return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
        });
    }

    /**
     * GT6 {@code Listener_Furnace_Smelting(-1, T)} XP:
     * {@code units(outputAmount, U, toolQuality+1, true)}. Netherrack special
     * is {@code add_smelting(..., F, F, T)} with no XP.
     */
    public static float furnaceExperience(
            MaterialDefinition material, FurnacePlan plan) {
        if (plan.netherrackSpecial()) {
            return 0.0F;
        }
        int quality = material.gt6Metadata()
                .map(metadata -> metadata.tool().quality())
                .orElse(0);
        return (float) units(plan.units(), U, 1L + quality, true);
    }

    private static Optional<GT6MaterialMetadata.MaterialAmount> target(
            MaterialDefinition material, String key) {
        return material.gt6Metadata()
                .map(GT6MaterialMetadata::processingTargets)
                .map(targets -> targets.get(key));
    }

    private static int dustCount(MaterialPrefix prefix, long amount) {
        if (prefix.equals(MaterialPrefixes.STORAGE_DUST)) {
            return (int) (amount / (U * 9L));
        }
        if (prefix.equals(MaterialPrefixes.DUST)) {
            return (int) (amount / U);
        }
        if (prefix.equals(MaterialPrefixes.SMALL_DUST)) {
            return (int) (amount / MaterialPrefixes.SMALL_DUST.units());
        }
        if (prefix.equals(MaterialPrefixes.TINY_DUST)) {
            return (int) (amount / MaterialPrefixes.TINY_DUST.units());
        }
        if (prefix.equals(MaterialPrefixes.DUST_DIV72)) {
            return (int) (amount / MaterialPrefixes.DUST_DIV72.units());
        }
        return 0;
    }

    private static int ingotCount(MaterialPrefix prefix, long amount) {
        if (prefix.equals(MaterialPrefixes.BLOCK)) {
            return (int) (amount / (U * 9L));
        }
        if (prefix.equals(MaterialPrefixes.INGOT)) {
            return (int) (amount / U);
        }
        if (prefix.equals(MaterialPrefixes.CHUNK)) {
            return (int) ((amount * 4L) / U);
        }
        if (prefix.equals(MaterialPrefixes.NUGGET)) {
            return (int) ((amount * 9L) / U);
        }
        return 0;
    }

    public record PulverPlan(
            String materialId, MaterialPrefix prefix, int count, long units) {}

    public record FurnacePlan(
            String materialId,
            MaterialPrefix prefix,
            int count,
            long units,
            boolean netherrackSpecial) {}
}
