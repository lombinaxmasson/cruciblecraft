package com.masson.cruciblecraft.recipe.gt;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Immutable snapshot of the concrete resources offered to a recipe map. */
public record GTRecipeQuery(
        List<ItemStack> itemInputs,
        List<FluidStack> fluidInputs) {

    public GTRecipeQuery {
        itemInputs = copyItems(itemInputs);
        fluidInputs = copyFluids(fluidInputs);
    }

    public static GTRecipeQuery items(ItemStack... inputs) {
        return new GTRecipeQuery(List.of(inputs), List.of());
    }

    @Override
    public List<ItemStack> itemInputs() {
        return copyItems(itemInputs);
    }

    @Override
    public List<FluidStack> fluidInputs() {
        return copyFluids(fluidInputs);
    }

    /** Package-private zero-copy view for trusted matching/index code. */
    List<ItemStack> itemInputsView() {
        return itemInputs;
    }

    /** Package-private zero-copy view for trusted matching/index code. */
    List<FluidStack> fluidInputsView() {
        return fluidInputs;
    }

    private static List<ItemStack> copyItems(List<ItemStack> stacks) {
        if (stacks == null) {
            throw new NullPointerException("itemInputs");
        }
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private static List<FluidStack> copyFluids(List<FluidStack> stacks) {
        if (stacks == null) {
            throw new NullPointerException("fluidInputs");
        }
        return stacks.stream().map(FluidStack::copy).toList();
    }
}
