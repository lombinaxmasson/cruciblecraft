package com.masson.cruciblecraft.machine.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.material.MaterialComponentPolicy;
import com.masson.cruciblecraft.recipe.gt.CapacityMatcher;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Immutable before/after resource snapshot. Capacity and allocation are solved
 * before commit; commit first revalidates every observed slot and tank.
 */
public record MachineTransaction(
        List<ItemStack> beforeItems,
        List<ItemStack> afterItems,
        List<FluidStack> beforeFluids,
        List<FluidStack> afterFluids) {

    public MachineTransaction {
        beforeItems = copyItems(beforeItems);
        afterItems = copyItems(afterItems);
        beforeFluids = copyFluids(beforeFluids);
        afterFluids = copyFluids(afterFluids);
        if (beforeItems.size() != afterItems.size()
                || beforeFluids.size() != afterFluids.size()) {
            throw new IllegalArgumentException("Transaction snapshots have different shapes");
        }
    }

    public static Optional<MachineTransaction> prepare(
            GTRecipe recipe,
            List<ItemStack> items,
            List<Integer> inputSlots,
            List<Integer> outputSlots,
            List<FluidStack> fluids,
            List<ProcessingMachineSpec.TankSpec> inputTanks,
            List<ProcessingMachineSpec.TankSpec> outputTanks,
            List<ItemStack> rolledOutputs) {
        return prepare(
                recipe,
                items,
                inputSlots,
                outputSlots,
                fluids,
                inputTanks,
                outputTanks,
                rolledOutputs,
                List.of());
    }

    public static Optional<MachineTransaction> prepareIndexed(
            GTRecipe recipe,
            List<ItemStack> items,
            List<Integer> inputSlots,
            List<Integer> outputSlots,
            List<FluidStack> fluids,
            List<ProcessingMachineSpec.TankSpec> inputTanks,
            List<ProcessingMachineSpec.TankSpec> outputTanks,
            List<ItemStack> indexedOutputs) {
        return prepareIndexed(
                recipe,
                items,
                inputSlots,
                outputSlots,
                fluids,
                inputTanks,
                outputTanks,
                indexedOutputs,
                List.of());
    }

    public static Optional<MachineTransaction> prepareIndexed(
            GTRecipe recipe,
            List<ItemStack> items,
            List<Integer> inputSlots,
            List<Integer> outputSlots,
            List<FluidStack> fluids,
            List<ProcessingMachineSpec.TankSpec> inputTanks,
            List<ProcessingMachineSpec.TankSpec> outputTanks,
            List<ItemStack> indexedOutputs,
            List<ItemStack> extraOffered) {
        return prepareInternal(
                recipe,
                items,
                inputSlots,
                outputSlots,
                fluids,
                inputTanks,
                outputTanks,
                indexedOutputs,
                extraOffered,
                true);
    }

    public static Optional<MachineTransaction> prepare(
            GTRecipe recipe,
            List<ItemStack> items,
            List<Integer> inputSlots,
            List<Integer> outputSlots,
            List<FluidStack> fluids,
            List<ProcessingMachineSpec.TankSpec> inputTanks,
            List<ProcessingMachineSpec.TankSpec> outputTanks,
            List<ItemStack> rolledOutputs,
            List<ItemStack> extraOffered) {
        return prepareInternal(
                recipe,
                items,
                inputSlots,
                outputSlots,
                fluids,
                inputTanks,
                outputTanks,
                rolledOutputs,
                extraOffered,
                false);
    }

    private static Optional<MachineTransaction> prepareInternal(
            GTRecipe recipe,
            List<ItemStack> items,
            List<Integer> inputSlots,
            List<Integer> outputSlots,
            List<FluidStack> fluids,
            List<ProcessingMachineSpec.TankSpec> inputTanks,
            List<ProcessingMachineSpec.TankSpec> outputTanks,
            List<ItemStack> rolledOutputs,
            List<ItemStack> extraOffered,
            boolean indexedOutputs) {
        List<ItemStack> afterItems = copyItems(items);
        List<FluidStack> afterFluids = copyFluids(fluids);

        List<ItemStack> offeredItems = new ArrayList<>(
                inputSlots.stream().map(afterItems::get).toList());
        if (extraOffered != null) {
            for (ItemStack extra : extraOffered) {
                offeredItems.add(
                        extra == null || extra.isEmpty()
                                ? ItemStack.EMPTY
                                : extra.copy());
            }
        }
        Optional<long[][]> itemAllocation = recipe.itemAllocation(offeredItems);
        if (itemAllocation.isEmpty()) {
            return Optional.empty();
        }
        long[][] allocatedItems = itemAllocation.get();
        for (int offered = 0; offered < inputSlots.size(); offered++) {
            long consumed = 0L;
            long wear = 0L;
            for (int requirement = 0;
                    requirement < allocatedItems.length;
                    requirement++) {
                long allocation = allocatedItems[requirement][offered];
                ItemInputAction action =
                        recipe.itemInputActions().get(requirement);
                if (action.kind() == ItemInputAction.Kind.CONSUME) {
                    consumed = Math.addExact(consumed, allocation);
                } else if (action.kind() == ItemInputAction.Kind.WEAR
                        && allocation > 0L) {
                    wear = Math.addExact(
                            wear,
                            Math.multiplyExact(
                                    allocation, (long) action.damage()));
                }
            }
            if (consumed > Integer.MAX_VALUE || wear > Integer.MAX_VALUE) {
                return Optional.empty();
            }
            ItemStack current = afterItems.get(inputSlots.get(offered));
            if (consumed > current.getCount()) {
                return Optional.empty();
            }
            ItemStack remaining = current.copy();
            remaining.shrink((int) consumed);
            if (wear > 0L) {
                if (!canApplyWear(remaining)) {
                    return Optional.empty();
                }
                int maxDamage = remaining.getMaxDamage();
                if (maxDamage <= 0) {
                    return Optional.empty();
                }
                long nextDamage = Math.addExact(
                        (long) remaining.getDamageValue(), wear);
                if (nextDamage >= maxDamage) {
                    return Optional.empty();
                }
                remaining.setDamageValue((int) nextDamage);
            }
            afterItems.set(inputSlots.get(offered), remaining);
        }

        Optional<long[][]> fluidAllocation = solveFluids(
                recipe.fluidInputs(),
                inputTanks.stream().map(tank -> afterFluids.get(tank.index())).toList());
        if (fluidAllocation.isEmpty()) {
            return Optional.empty();
        }
        long[][] allocatedFluids = fluidAllocation.get();
        for (int offered = 0; offered < inputTanks.size(); offered++) {
            long consumed = 0L;
            for (long[] requirement : allocatedFluids) {
                consumed = Math.addExact(consumed, requirement[offered]);
            }
            if (consumed > Integer.MAX_VALUE) {
                return Optional.empty();
            }
            int tank = inputTanks.get(offered).index();
            FluidStack remaining = afterFluids.get(tank).copy();
            remaining.shrink((int) consumed);
            afterFluids.set(tank, remaining);
        }

        if (indexedOutputs) {
            if (!insertIndexedItems(
                    afterItems, outputSlots, recipe, rolledOutputs)) {
                return Optional.empty();
            }
        } else {
            for (ItemStack output : rolledOutputs) {
                if (!insertItem(afterItems, outputSlots, output)) {
                    return Optional.empty();
                }
            }
        }
        if (indexedOutputs) {
            if (!insertIndexedFluids(
                    afterFluids, outputTanks, recipe.fluidOutputs())) {
                return Optional.empty();
            }
        } else {
            for (FluidStack output : recipe.fluidOutputs()) {
                if (!insertFluid(afterFluids, outputTanks, output)) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(new MachineTransaction(items, afterItems, fluids, afterFluids));
    }

    public boolean stillValid(ResourceAccess resources) {
        if (resources.itemCount() != beforeItems.size()
                || resources.fluidCount() != beforeFluids.size()) {
            return false;
        }
        for (int slot = 0; slot < beforeItems.size(); slot++) {
            if (!sameItem(beforeItems.get(slot), resources.item(slot))) {
                return false;
            }
        }
        for (int tank = 0; tank < beforeFluids.size(); tank++) {
            if (!sameFluid(beforeFluids.get(tank), resources.fluid(tank))) {
                return false;
            }
        }
        return true;
    }

    public boolean commit(ResourceAccess resources) {
        if (!stillValid(resources)) {
            return false;
        }
        for (int slot = 0; slot < afterItems.size(); slot++) {
            resources.setItem(slot, afterItems.get(slot).copy());
        }
        for (int tank = 0; tank < afterFluids.size(); tank++) {
            resources.setFluid(tank, afterFluids.get(tank).copy());
        }
        return true;
    }

    @Override public List<ItemStack> beforeItems() { return copyItems(beforeItems); }
    @Override public List<ItemStack> afterItems() { return copyItems(afterItems); }
    @Override public List<FluidStack> beforeFluids() { return copyFluids(beforeFluids); }
    @Override public List<FluidStack> afterFluids() { return copyFluids(afterFluids); }

    public interface ResourceAccess {
        int itemCount();
        ItemStack item(int slot);
        void setItem(int slot, ItemStack stack);
        int fluidCount();
        FluidStack fluid(int tank);
        void setFluid(int tank, FluidStack stack);
    }

    private static Optional<long[][]> solveFluids(
            List<FluidStack> requirements,
            List<FluidStack> offered) {
        long[] demands = requirements.stream().mapToLong(FluidStack::getAmount).toArray();
        long[] supplies = offered.stream().mapToLong(FluidStack::getAmount).toArray();
        boolean[][] compatible = new boolean[requirements.size()][offered.size()];
        for (int requirement = 0; requirement < requirements.size(); requirement++) {
            for (int supply = 0; supply < offered.size(); supply++) {
                compatible[requirement][supply] = FluidStack.isSameFluidSameComponents(
                        requirements.get(requirement), offered.get(supply));
            }
        }
        return CapacityMatcher.solve(demands, supplies, compatible);
    }

    private static boolean insertItem(
            List<ItemStack> inventory,
            List<Integer> outputSlots,
            ItemStack offered) {
        ItemStack remaining = offered.copy();
        for (int slot : outputSlots) {
            ItemStack current = inventory.get(slot);
            if (current.isEmpty()
                    || !ItemStack.isSameItemSameComponents(current, remaining)) {
                continue;
            }
            int moved = Math.min(
                    remaining.getCount(),
                    current.getMaxStackSize() - current.getCount());
            if (moved > 0) {
                ItemStack merged = current.copy();
                merged.grow(moved);
                inventory.set(slot, merged);
                remaining.shrink(moved);
            }
        }
        for (int slot : outputSlots) {
            if (remaining.isEmpty()) {
                return true;
            }
            if (!inventory.get(slot).isEmpty()) {
                continue;
            }
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            inventory.set(slot, remaining.copyWithCount(moved));
            remaining.shrink(moved);
        }
        return remaining.isEmpty();
    }

    private static boolean insertIndexedItems(
            List<ItemStack> inventory,
            List<Integer> outputSlots,
            GTRecipe recipe,
            List<ItemStack> indexedOutputs) {
        if (indexedOutputs.size() != recipe.itemOutputs().size()
                || indexedOutputs.size() > outputSlots.size()) {
            return false;
        }
        for (int index = 0; index < indexedOutputs.size(); index++) {
            ItemStack offered = indexedOutputs.get(index);
            if (offered == null || offered.isEmpty()) {
                continue;
            }
            int slot = outputSlots.get(index);
            ItemStack current = inventory.get(slot);
            if (offered.getCount() > offered.getMaxStackSize()) {
                return false;
            }
            if (current.isEmpty()) {
                inventory.set(slot, offered.copy());
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(current, offered)
                    || offered.getCount()
                            > current.getMaxStackSize() - current.getCount()) {
                return false;
            }
            ItemStack merged = current.copy();
            merged.grow(offered.getCount());
            inventory.set(slot, merged);
        }
        return true;
    }

    private static boolean insertIndexedFluids(
            List<FluidStack> fluids,
            List<ProcessingMachineSpec.TankSpec> outputs,
            List<FluidStack> offered) {
        if (offered.size() > outputs.size()) {
            return false;
        }
        for (int index = 0; index < offered.size(); index++) {
            FluidStack output = offered.get(index);
            if (output.isEmpty()) {
                continue;
            }
            ProcessingMachineSpec.TankSpec tank = outputs.get(index);
            FluidStack current = fluids.get(tank.index());
            if (current.isEmpty()) {
                if (output.getAmount() > tank.capacity()) {
                    return false;
                }
                fluids.set(tank.index(), output.copy());
                continue;
            }
            if (!FluidStack.isSameFluidSameComponents(current, output)
                    || output.getAmount()
                            > tank.capacity() - current.getAmount()) {
                return false;
            }
            FluidStack merged = current.copy();
            merged.grow(output.getAmount());
            fluids.set(tank.index(), merged);
        }
        return true;
    }

    private static boolean insertFluid(
            List<FluidStack> fluids,
            List<ProcessingMachineSpec.TankSpec> outputs,
            FluidStack offered) {
        FluidStack remaining = offered.copy();
        for (ProcessingMachineSpec.TankSpec output : outputs) {
            FluidStack current = fluids.get(output.index());
            if (current.isEmpty()
                    || !FluidStack.isSameFluidSameComponents(current, remaining)) {
                continue;
            }
            int moved = Math.min(
                    remaining.getAmount(), output.capacity() - current.getAmount());
            if (moved > 0) {
                FluidStack merged = current.copy();
                merged.grow(moved);
                fluids.set(output.index(), merged);
                remaining.shrink(moved);
            }
        }
        for (ProcessingMachineSpec.TankSpec output : outputs) {
            if (remaining.isEmpty()) {
                return true;
            }
            if (!fluids.get(output.index()).isEmpty()) {
                continue;
            }
            int moved = Math.min(remaining.getAmount(), output.capacity());
            fluids.set(output.index(), remaining.copyWithAmount(moved));
            remaining.shrink(moved);
        }
        return remaining.isEmpty();
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        return first.getCount() == second.getCount()
                && (first.isEmpty() && second.isEmpty()
                || ItemStack.isSameItemSameComponents(first, second));
    }

    private static boolean sameFluid(FluidStack first, FluidStack second) {
        return first.getAmount() == second.getAmount()
                && (first.isEmpty() && second.isEmpty()
                || FluidStack.isSameFluidSameComponents(first, second));
    }

    private static boolean canApplyWear(ItemStack stack) {
        if (stack.isEmpty()
                || !stack.isDamageableItem()
                || stack.getCount() != 1) {
            return false;
        }
        if (stack.getItem() instanceof MaterialComponentPolicy policy) {
            String materialId = stack.get(ModComponents.TOOL_MATERIAL);
            return materialId != null
                    && policy.isPersistedMaterialAllowed(materialId);
        }
        return true;
    }

    private static List<ItemStack> copyItems(List<ItemStack> values) {
        List<ItemStack> copies = new ArrayList<>(values.size());
        values.forEach(value -> copies.add(value.copy()));
        return copies;
    }

    private static List<FluidStack> copyFluids(List<FluidStack> values) {
        List<FluidStack> copies = new ArrayList<>(values.size());
        values.forEach(value -> copies.add(value.copy()));
        return copies;
    }
}
