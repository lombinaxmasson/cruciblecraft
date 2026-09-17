package com.masson.cruciblecraft.energy.converter;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

/** Live vanilla furnace-fuel adapter for GT6 FM.Furnace burning boxes. */
public final class FurnaceFuelAdapter {
    private FurnaceFuelAdapter() {}

    public static int burnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return Math.max(0, stack.getBurnTime(RecipeType.SMELTING));
    }

    public static boolean isFuel(ItemStack stack) {
        return burnTime(stack) > 0;
    }

    public static long heatUnits(ItemStack stack, Integer efficiencyBps) {
        int burn = burnTime(stack);
        if (burn <= 0) {
            return 0L;
        }
        int bps = efficiencyBps == null || efficiencyBps <= 0
                ? 10_000
                : Math.min(10_000, efficiencyBps);
        return Math.max(1L, (long) burn * bps / 10_000L);
    }

    public static ItemStack ashFor(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String materialId = materialId(fuel);
        if (materialId == null) {
            return ItemStack.EMPTY;
        }
        return MaterialLookup.byId(materialId)
                .flatMap(MaterialDefinition::gt6Metadata)
                .map(GT6MaterialMetadata::processingTargets)
                .map(targets -> targets.get("burning"))
                .filter(amount -> amount.ccUnits().orElse(0L) > 0L)
                .map(amount -> stackFor(amount.material(), amount.ccUnits().orElse(0L)))
                .orElse(ItemStack.EMPTY);
    }

    private static String materialId(ItemStack fuel) {
        var resolved = MaterialUnits.resolve(fuel);
        if (resolved.isPresent()) {
            return resolved.orElseThrow().materialId();
        }
        if (fuel.is(Items.COAL) || fuel.is(Items.CHARCOAL)) {
            return "coal";
        }
        if (fuel.is(Items.COAL_BLOCK)) {
            return "coal";
        }
        return null;
    }

    private static ItemStack stackFor(String material, long units) {
        var forms = java.util.List.of(
                MaterialPrefixes.DUST,
                MaterialPrefixes.SMALL_DUST,
                MaterialPrefixes.TINY_DUST,
                MaterialPrefixes.DUST_DIV72);
        for (var form : forms) {
            int formUnits = form.units();
            if (formUnits <= 0 || units % formUnits != 0) {
                continue;
            }
            long count = units / formUnits;
            if (count <= 0L || count > 64L) {
                continue;
            }
            ItemStack stack = MaterialLookup.tryStack(material, form, (int) count)
                    .orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
