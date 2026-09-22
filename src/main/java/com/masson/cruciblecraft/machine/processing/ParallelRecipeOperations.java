package com.masson.cruciblecraft.machine.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Pure recipe/resource projection for one atomic parallel machine cycle. */
public final class ParallelRecipeOperations {
    public static GTRecipe scale(GTRecipe recipe, int operations) {
        if (operations <= 0) {
            throw new IllegalArgumentException(
                    "Parallel operations must be positive");
        }
        List<Integer> counts = recipe.itemInputCounts().stream()
                .map(count -> Math.multiplyExact(count, operations))
                .toList();
        List<ItemInputAction> actions = recipe.itemInputActions().stream()
                .map(action -> action.kind()
                                == ItemInputAction.Kind.WEAR
                        ? ItemInputAction.wear(Math.multiplyExact(
                                action.damage(), operations))
                        : action)
                .toList();
        List<FluidStack> fluidInputs = recipe.fluidInputs().stream()
                .map(stack -> stack.copyWithAmount(Math.multiplyExact(
                        stack.getAmount(), operations)))
                .toList();
        List<FluidStack> fluidOutputs = recipe.fluidOutputs().stream()
                .map(stack -> stack.copyWithAmount(Math.multiplyExact(
                        stack.getAmount(), operations)))
                .toList();
        return new GTRecipe(
                recipe.itemInputs(),
                counts,
                actions,
                recipe.itemOutputs(),
                fluidInputs,
                fluidOutputs,
                recipe.outputChances(),
                recipe.duration(),
                recipe.eut(),
                recipe.specialValue(),
                recipe.canBeBuffered(),
                recipe.provenance());
    }

    public static List<ItemStack> maximumItemOutputs(
            GTRecipe recipe, int operations) {
        if (operations <= 0) {
            throw new IllegalArgumentException(
                    "Parallel operations must be positive");
        }
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack output : recipe.itemOutputs()) {
            long remaining = Math.multiplyExact(
                    (long) output.getCount(), operations);
            while (remaining > 0L) {
                int count = (int) Math.min(
                        remaining, output.getMaxStackSize());
                result.add(output.copyWithCount(count));
                remaining -= count;
            }
        }
        return List.copyOf(result);
    }

    public static List<ItemStack> rollItemOutputs(
            GTRecipe recipe,
            int operations,
            IntUnaryOperator randomBelow) {
        List<ItemStack> result = new ArrayList<>();
        for (int operation = 0;
                operation < operations;
                operation++) {
            ChanceOutputs.roll(
                    recipe.itemOutputs(),
                    recipe.outputChances(),
                    randomBelow).forEach(
                            output -> appendCompacted(result, output));
        }
        return List.copyOf(result);
    }

    /**
     * Rolls outputs while retaining the source recipe output index.
     *
     * <p>GT6 writes output {@code i} to output slot {@code i}; equal stacks
     * from different output entries must not be compacted together before the
     * machine transaction is committed.
     */
    public static List<ItemStack> rollItemOutputsIndexed(
            GTRecipe recipe,
            int operations,
            IntUnaryOperator randomBelow) {
        if (operations <= 0) {
            throw new IllegalArgumentException(
                    "Parallel operations must be positive");
        }
        List<ItemStack> result = new ArrayList<>(
                recipe.itemOutputs().size());
        for (int index = 0; index < recipe.itemOutputs().size(); index++) {
            ItemStack output = ItemStack.EMPTY;
            ItemStack template = recipe.itemOutputs().get(index);
            int chance = recipe.outputChances().get(index);
            for (int operation = 0; operation < operations; operation++) {
                ItemStack rolled = ChanceOutputs.roll(
                        template, chance, randomBelow);
                if (rolled.isEmpty()) {
                    continue;
                }
                if (output.isEmpty()) {
                    output = rolled;
                } else {
                    output.grow(rolled.getCount());
                }
            }
            result.add(output);
        }
        return List.copyOf(result);
    }

    /**
     * Returns one capacity probe per source output index.
     *
     * <p>The indexed transaction rejects a stack that does not fit its
     * corresponding output slot, matching GT6's {@code addStackToSlot}
     * routing and its parallel-count reduction.
     */
    public static List<ItemStack> maximumItemOutputsIndexed(
            GTRecipe recipe,
            int operations) {
        if (operations <= 0) {
            throw new IllegalArgumentException(
                    "Parallel operations must be positive");
        }
        List<ItemStack> result = new ArrayList<>(
                recipe.itemOutputs().size());
        for (ItemStack output : recipe.itemOutputs()) {
            long count = Math.multiplyExact(
                    (long) output.getCount(), operations);
            result.add(output.copyWithCount(
                    (int) Math.min(count, Integer.MAX_VALUE)));
        }
        return List.copyOf(result);
    }

    private static void appendCompacted(
            List<ItemStack> outputs, ItemStack offered) {
        ItemStack remaining = offered.copy();
        for (ItemStack current : outputs) {
            if (!ItemStack.isSameItemSameComponents(current, remaining)) {
                continue;
            }
            int moved = Math.min(
                    remaining.getCount(),
                    current.getMaxStackSize() - current.getCount());
            if (moved > 0) {
                current.grow(moved);
                remaining.shrink(moved);
            }
            if (remaining.isEmpty()) {
                return;
            }
        }
        while (!remaining.isEmpty()) {
            int moved = Math.min(
                    remaining.getCount(), remaining.getMaxStackSize());
            outputs.add(remaining.copyWithCount(moved));
            remaining.shrink(moved);
        }
    }

    private ParallelRecipeOperations() {}
}
