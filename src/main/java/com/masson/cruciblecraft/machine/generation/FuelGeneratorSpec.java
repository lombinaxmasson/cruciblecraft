package com.masson.cruciblecraft.machine.generation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.material.ChemicalFluidRegistrationGate;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Immutable source-map, exhaust shape and energy output for one fuel generator. */
public record FuelGeneratorSpec(
        ResourceLocation id,
        Supplier<RecipeMap> recipeMap,
        int inputCapacityMb,
        int outputCapacityMb,
        int outputTanks,
        EnergyType outputEnergyType,
        long outputPacketSize,
        long maximumOutputPacketsPerTick,
        long energyCapacity,
        int efficiencyBps,
        EnergyOutputFace energyOutputFace,
        List<Direction> exhaustOutputSides,
        int identitySchemaVersion,
        InputPhase inputPhase) {
    public enum EnergyOutputFace {
        FRONT,
        UP;

        public Direction resolve(Direction front) {
            return this == UP ? Direction.UP : front;
        }
    }

    /** GT6 {@code FL.gas} vs liquid split on FM.Burn hosts. */
    public enum InputPhase {
        ANY,
        GAS,
        LIQUID
    }

    public FuelGeneratorSpec(
            ResourceLocation id,
            Supplier<RecipeMap> recipeMap,
            int inputCapacityMb,
            int outputCapacityMb,
            int outputTanks,
            EnergyType outputEnergyType,
            long outputPacketSize,
            long maximumOutputPacketsPerTick,
            long energyCapacity,
            int efficiencyBps,
            EnergyOutputFace energyOutputFace,
            List<Direction> exhaustOutputSides,
            int identitySchemaVersion) {
        this(
                id,
                recipeMap,
                inputCapacityMb,
                outputCapacityMb,
                outputTanks,
                outputEnergyType,
                outputPacketSize,
                maximumOutputPacketsPerTick,
                energyCapacity,
                efficiencyBps,
                energyOutputFace,
                exhaustOutputSides,
                identitySchemaVersion,
                InputPhase.ANY);
    }

    public FuelGeneratorSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(recipeMap, "recipeMap");
        Objects.requireNonNull(outputEnergyType, "outputEnergyType");
        Objects.requireNonNull(energyOutputFace, "energyOutputFace");
        Objects.requireNonNull(inputPhase, "inputPhase");
        exhaustOutputSides = List.copyOf(
                Objects.requireNonNull(
                        exhaustOutputSides, "exhaustOutputSides"));
        if (inputCapacityMb <= 0
                || outputCapacityMb <= 0
                || outputTanks <= 0
                || outputTanks > 2
                || outputPacketSize <= 0L
                || maximumOutputPacketsPerTick <= 0L
                || energyCapacity < outputPacketSize
                || efficiencyBps <= 0
                || efficiencyBps > 10_000
                || exhaustOutputSides.size() != outputTanks
                || exhaustOutputSides.stream().distinct().count()
                        != outputTanks
                || identitySchemaVersion <= 0) {
            throw new IllegalArgumentException(
                    "Fuel-generator tank and energy limits are invalid");
        }
        Direction fixedEnergySide = energyOutputFace.resolve(Direction.NORTH);
        if (exhaustOutputSides.contains(fixedEnergySide)
                && energyOutputFace == EnergyOutputFace.UP) {
            throw new IllegalArgumentException(
                    "Fuel-generator energy and exhaust faces overlap");
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
                || recipePowerInvalid(recipe)
                || recipe.fluidInputs().getFirst().getAmount()
                        > inputCapacityMb
                || recipe.fluidOutputs().stream().anyMatch(
                        stack -> stack.getAmount() > outputCapacityMb)) {
            return Optional.of("fuel_generator_recipe_shape");
        }
        return Optional.empty();
    }

    public long generatedEnergy(GTRecipe recipe) {
        try {
            long sourceEnergy = Math.multiplyExact(
                    Math.abs(recipe.eut()), (long) recipe.duration());
            return Math.multiplyExact(sourceEnergy, efficiencyBps)
                    / 10_000L;
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException(
                    "Fuel recipe total energy overflows", overflow);
        }
    }

    public long generatedEnergyAtTick(GTRecipe recipe, int tick) {
        if (tick < 0 || tick >= recipe.duration()) {
            throw new IllegalArgumentException(
                    "Fuel recipe tick is outside its duration");
        }
        long total = generatedEnergy(recipe);
        long base = total / recipe.duration();
        long remainder = total % recipe.duration();
        return base + (tick < remainder ? 1L : 0L);
    }

    public Direction energyOutputSide(Direction front) {
        return energyOutputFace.resolve(front);
    }

    /** GT6 gas/liquid burning boxes need a front-face igniter. */
    public boolean requiresIgnition() {
        return inputPhase == InputPhase.GAS
                || inputPhase == InputPhase.LIQUID;
    }

    private boolean recipePowerInvalid(GTRecipe recipe) {
        try {
            return generatedEnergy(recipe) <= 0L;
        } catch (IllegalArgumentException overflow) {
            return true;
        }
    }

    public boolean acceptsInput(FluidStack stack) {
        if (stack.isEmpty()
                || !requireRecipeMap().hasFluidCandidate(stack.getFluid())) {
            return false;
        }
        return switch (inputPhase) {
            case ANY -> true;
            case GAS -> gaseous(stack);
            case LIQUID -> !gaseous(stack);
        };
    }

    private static boolean gaseous(FluidStack stack) {
        return ModFluids.chemicalState(stack.getFluid())
                .orElse(ChemicalFluidRegistrationGate.State.LIQUID)
                == ChemicalFluidRegistrationGate.State.GAS;
    }
}
