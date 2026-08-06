package com.masson.cruciblecraft.machine.generation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Immutable source-map and tank shape for one direct-electric fuel generator. */
public record FuelGeneratorSpec(
        ResourceLocation id,
        Supplier<RecipeMap> recipeMap,
        int inputCapacityMb,
        int outputCapacityMb,
        int outputTanks) {
    public FuelGeneratorSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(recipeMap, "recipeMap");
        if (inputCapacityMb <= 0
                || outputCapacityMb <= 0
                || outputTanks <= 0
                || outputTanks > 2) {
            throw new IllegalArgumentException(
                    "Fuel generators support one top and one bottom exhaust tank");
        }
    }

    public RecipeMap requireRecipeMap() {
        return Objects.requireNonNull(
                recipeMap.get(), "fuel generator recipe map");
    }

    public Optional<String> validate(GTRecipe recipe) {
        if (recipe.fluidOutputs().size() > outputTanks) {
            return Optional.of("host_output_shape_unsupported");
        }
        if (!recipe.itemInputs().isEmpty()
                || !recipe.itemOutputs().isEmpty()
                || recipe.fluidInputs().size() != 1
                || recipe.fluidOutputs().isEmpty()
                || recipe.eut() >= 0L
                || recipe.eut() == Long.MIN_VALUE
                || Math.abs(recipe.eut()) > 1_024L
                || recipe.duration() <= 0
                || recipe.fluidInputs().getFirst().getAmount()
                        > inputCapacityMb
                || recipe.fluidOutputs().stream().anyMatch(
                        stack -> stack.getAmount() > outputCapacityMb)) {
            return Optional.of("fuel_generator_recipe_shape");
        }
        return Optional.empty();
    }

    public boolean acceptsInput(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return requireRecipeMap().hasFluidCandidate(stack.getFluid());
    }
}
