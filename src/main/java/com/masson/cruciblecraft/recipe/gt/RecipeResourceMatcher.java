package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/** Exact, order-independent capacity matching for overlapping recipe inputs. */
final class RecipeResourceMatcher {
    private RecipeResourceMatcher() {}

    static boolean matchesItems(
            List<Ingredient> requirements,
            List<Integer> counts,
            List<ItemStack> offered) {
        return solveItems(requirements, counts, offered).isPresent();
    }

    static Optional<long[][]> solveItems(
            List<Ingredient> requirements,
            List<Integer> counts,
            List<ItemStack> offered) {
        long[] demands = counts.stream().mapToLong(Integer::longValue).toArray();
        long[] supplies = offered.stream()
                .mapToLong(stack -> stack.isEmpty() ? 0L : stack.getCount())
                .toArray();
        boolean[][] compatible = new boolean[requirements.size()][offered.size()];
        boolean[] presenceOnly = new boolean[requirements.size()];
        for (int requirement = 0; requirement < requirements.size(); requirement++) {
            int count = counts.get(requirement);
            presenceOnly[requirement] = count == 0;
            for (int supply = 0; supply < offered.size(); supply++) {
                ItemStack stack = offered.get(supply);
                compatible[requirement][supply] =
                        !stack.isEmpty() && requirements.get(requirement).test(stack);
            }
        }
        return CapacityMatcher.solve(demands, supplies, compatible, presenceOnly);
    }

    static boolean matchesFluids(
            List<FluidStack> requirements,
            List<FluidStack> offered) {
        long[] demands = requirements.stream()
                .mapToLong(FluidStack::getAmount)
                .toArray();
        long[] supplies = offered.stream()
                .mapToLong(stack -> stack.isEmpty() ? 0L : stack.getAmount())
                .toArray();
        boolean[][] compatible = new boolean[requirements.size()][offered.size()];
        for (int requirement = 0; requirement < requirements.size(); requirement++) {
            for (int supply = 0; supply < offered.size(); supply++) {
                FluidStack stack = offered.get(supply);
                compatible[requirement][supply] =
                        !stack.isEmpty()
                                && FluidStack.isSameFluidSameComponents(
                                        requirements.get(requirement),
                                        stack);
            }
        }
        return CapacityMatcher.canSatisfy(demands, supplies, compatible);
    }
}
