package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * EMI-independent projection of a processing recipe.
 *
 * <p>Keeping the semantic conversion outside the EMI adapter lets unit tests
 * exercise it without loading the compile-only EMI runtime.
 */
public record ProcessingEmiRecipeData(
        List<ItemInput> consumedInputs,
        List<ItemInput> catalysts,
        List<ItemOutput> itemOutputs,
        List<FluidResource> fluidInputs,
        List<FluidResource> fluidOutputs,
        int durationTicks,
        long eut,
        long specialValue,
        EnergyType energyType) {

    public ProcessingEmiRecipeData {
        consumedInputs = List.copyOf(consumedInputs);
        catalysts = List.copyOf(catalysts);
        itemOutputs = List.copyOf(itemOutputs);
        fluidInputs = List.copyOf(fluidInputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        Objects.requireNonNull(energyType, "energyType");
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
    }

    public static ProcessingEmiRecipeData from(
            ProcessingMachineSpec spec,
            GTRecipe recipe) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(recipe, "recipe");
        List<ItemInput> consumed = new ArrayList<>();
        List<ItemInput> catalysts = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemInputAction action = recipe.itemInputActions().get(index);
            ItemInput input = new ItemInput(
                    index,
                    recipe.itemInputs().get(index),
                    action.kind() == ItemInputAction.Kind.CONSUME
                            ? recipe.itemInputCounts().get(index)
                            : 1,
                    action);
            (action.kind() == ItemInputAction.Kind.CONSUME
                    ? consumed
                    : catalysts).add(input);
        }
        List<ItemStack> outputStacks = recipe.itemOutputsView();
        List<ItemOutput> itemOutputs = new ArrayList<>(outputStacks.size());
        for (int index = 0; index < outputStacks.size(); index++) {
            itemOutputs.add(new ItemOutput(
                    index,
                    outputStacks.get(index),
                    recipe.outputChances().get(index)));
        }
        List<FluidStack> inputFluids = recipe.fluidInputsView();
        List<FluidResource> fluidInputs = new ArrayList<>(inputFluids.size());
        for (int index = 0; index < inputFluids.size(); index++) {
            fluidInputs.add(new FluidResource(index, inputFluids.get(index)));
        }
        List<FluidStack> outputFluids = recipe.fluidOutputsView();
        List<FluidResource> fluidOutputs = new ArrayList<>(outputFluids.size());
        for (int index = 0; index < outputFluids.size(); index++) {
            fluidOutputs.add(new FluidResource(index, outputFluids.get(index)));
        }
        return new ProcessingEmiRecipeData(
                consumed,
                catalysts,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                recipe.duration(),
                recipe.eut(),
                recipe.specialValue(),
                spec.energy().type());
    }

    public String durationText() {
        if (durationTicks < 1_200) {
            return "Time: " + durationTicks + " ticks";
        }
        if (durationTicks < 36_000) {
            return "Time: " + (durationTicks / 20) + " secs";
        }
        return "Time: " + (durationTicks / 1_200) + " mins";
    }

    public String powerText() {
        long rate = Math.abs(eut);
        return (eut < 0L ? "Output: " : "Usage: ") + rate + " " + energyUnit();
    }

    public String costsText() {
        long total = Math.abs(eut) * (long) durationTicks;
        return (eut < 0L ? "Gain: " : "Costs: ") + total + " " + energyName();
    }

    public String energyName() {
        String unit = energyUnit();
        return unit.endsWith("/t") ? unit.substring(0, unit.length() - 2) : unit;
    }

    public String energyUnit() {
        return switch (energyType) {
            case KINETIC, KINETIC_PUSH -> "KU/t";
            case KINETIC_ROTATION -> "RU/t";
            case ELECTRIC -> "EU/t";
            case LU -> "LU/t";
            case QUANTUM -> "QU/t";
            case CU -> "CU/t";
            case MU -> "MU/t";
            case HEAT -> "HEAT/t";
            case TIME -> "TU/t";
            case AIR -> "Air/t";
        };
    }

    public record ItemInput(
            int recipeIndex,
            Ingredient ingredient,
            long displayAmount,
            ItemInputAction action) {
        public ItemInput {
            if (recipeIndex < 0 || displayAmount <= 0L) {
                throw new IllegalArgumentException("Invalid item input display data");
            }
            Objects.requireNonNull(ingredient, "ingredient");
            Objects.requireNonNull(action, "action");
        }

        public boolean catalyst() {
            return action.kind() != ItemInputAction.Kind.CONSUME;
        }

        public String actionText() {
            return switch (action.kind()) {
                case CONSUME -> "Consumed";
                case PRESERVE -> "Preserved";
                case WEAR -> "Tool wear: " + action.damage() + " durability";
            };
        }
    }

    public record ItemOutput(int recipeIndex, ItemStack stack, int chance) {
        public ItemOutput {
            if (recipeIndex < 0
                    || stack == null
                    || stack.isEmpty()
                    || chance < 0
                    || chance > GTRecipe.GUARANTEED_CHANCE) {
                throw new IllegalArgumentException("Invalid item output display data");
            }
            stack = stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }

        public float chanceFraction() {
            return chance / (float) GTRecipe.GUARANTEED_CHANCE;
        }

        public String chanceText() {
            return String.format(Locale.ROOT, "Chance: %.2f%%", chance / 100.0D);
        }
    }

    public record FluidResource(int recipeIndex, FluidStack stack) {
        public FluidResource {
            if (recipeIndex < 0 || stack == null || stack.isEmpty()) {
                throw new IllegalArgumentException("Invalid fluid display data");
            }
            stack = stack.copy();
        }

        @Override
        public FluidStack stack() {
            return stack.copy();
        }
    }
}
