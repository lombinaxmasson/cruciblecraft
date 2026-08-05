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
        return solveItems(
                requirements, counts, legacyActions(counts), offered).isPresent();
    }

    static boolean matchesItems(
            List<Ingredient> requirements,
            List<Integer> counts,
            List<ItemInputAction> actions,
            List<ItemStack> offered) {
        return solveItems(requirements, counts, actions, offered).isPresent();
    }

    static Optional<long[][]> solveItems(
            List<Ingredient> requirements,
            List<Integer> counts,
            List<ItemStack> offered) {
        return solveItems(requirements, counts, legacyActions(counts), offered);
    }

    static Optional<long[][]> solveItems(
            List<Ingredient> requirements,
            List<Integer> counts,
            List<ItemInputAction> actions,
            List<ItemStack> offered) {
        long[] demands = new long[counts.size()];
        for (int requirement = 0; requirement < counts.size(); requirement++) {
            demands[requirement] =
                    actions.get(requirement).kind() == ItemInputAction.Kind.WEAR
                            ? 1L
                            : counts.get(requirement);
        }
        long[] supplies = offered.stream()
                .mapToLong(stack -> stack.isEmpty() ? 0L : stack.getCount())
                .toArray();
        boolean[][] compatible = new boolean[requirements.size()][offered.size()];
        boolean[] presenceOnly = new boolean[requirements.size()];
        for (int requirement = 0; requirement < requirements.size(); requirement++) {
            presenceOnly[requirement] =
                    actions.get(requirement).kind()
                            == ItemInputAction.Kind.PRESERVE;
            for (int supply = 0; supply < offered.size(); supply++) {
                ItemStack stack = offered.get(supply);
                compatible[requirement][supply] =
                        !stack.isEmpty() && requirements.get(requirement).test(stack);
            }
        }
        return CapacityMatcher.solve(demands, supplies, compatible, presenceOnly);
    }

    private static List<ItemInputAction> legacyActions(List<Integer> counts) {
        return counts.stream()
                .map(count -> count == 0
                        ? ItemInputAction.PRESERVE
                        : ItemInputAction.CONSUME)
                .toList();
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
