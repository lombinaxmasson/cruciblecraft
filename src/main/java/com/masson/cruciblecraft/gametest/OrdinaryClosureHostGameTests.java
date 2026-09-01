package com.masson.cruciblecraft.gametest;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FireboxBlockEntity;
import com.masson.cruciblecraft.heat.FuelDefinition;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineEnergyPlacement;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import com.masson.cruciblecraft.api.energy.EnergyType;

final class OrdinaryClosureHostGameTests {
    private static final Direction FRONT = Direction.EAST;

    private OrdinaryClosureHostGameTests() {}

    static Set<ResourceLocation> publishedIds(RecipeMap map, String recipePrefix) {
        Set<ResourceLocation> ids = new TreeSet<>();
        map.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith(recipePrefix))
                .forEach(ids::add);
        return ids;
    }

    static RecipeMap.Entry firstOrdinary(RecipeMap map, String recipePrefix) {
        RecipeMap.Entry best = null;
        for (RecipeMap.Entry entry : map.entries()) {
            if (!entry.id().getPath().startsWith(recipePrefix)) {
                continue;
            }
            GTRecipe recipe = entry.recipe();
            if (!inputsResolvable(recipe) || !hasDeclaredOutput(recipe)) {
                continue;
            }
            if (best == null
                    || recipe.duration() < best.recipe().duration()) {
                best = entry;
            }
            if (recipe.itemInputs().isEmpty() && recipe.duration() <= 16) {
                return entry;
            }
        }
        if (best == null) {
            throw new IllegalStateException("Missing ordinary-closure recipe for " + recipePrefix);
        }
        return best;
    }

    static boolean inputsResolvable(GTRecipe recipe) {
        for (var ingredient : recipe.itemInputs()) {
            if (ingredient.getItems().length == 0) {
                return false;
            }
        }
        return true;
    }

    static boolean hasDeclaredOutput(GTRecipe recipe) {
        return !recipe.itemOutputs().isEmpty() || !recipe.fluidOutputs().isEmpty();
    }

    static void assertEmiContains(
            GameTestHelper helper,
            RecipeMap map,
            Set<ResourceLocation> published) {
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> emi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id().equals(map.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(emi.containsAll(published), "EMI is missing ordinary-closure ids");
    }

    static ConfiguredProcessingMachineBlockEntity place(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
            ProcessingMachineSpec spec) {
        if (spec.energy().type() == EnergyType.HEAT
                && spec.energy().mode() == ProcessingMachineSpec.EnergyMode.ADJACENT) {
            helper.setBlock(pos.below(), ModBlocks.FIREBOX.get());
            FireboxBlockEntity firebox = helper.getBlockEntity(pos.below());
            helper.assertTrue(
                    firebox.addFuel(FuelDefinition.COAL_COKE),
                    "Could not fuel the adjacent HU host");
        }
        helper.setBlock(
                pos,
                block.defaultBlockState().setValue(ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(
                machine.spec() == spec || machine.variant().kind().behavior() == spec,
                "Placed block resolved wrong machine kind");
        return machine;
    }

    static void fillEnergy(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine) {
        EnergyType type = machine.spec().energy().type();
        if (type == EnergyType.TIME) {
            return;
        }
        if (type == EnergyType.HEAT
                && machine.spec().energy().mode()
                        == ProcessingMachineSpec.EnergyMode.ADJACENT) {
            if (!(machine.getLevel().getBlockEntity(machine.getBlockPos().below())
                    instanceof FireboxBlockEntity firebox)) {
                helper.assertTrue(false, "adjacent HU host missing");
                return;
            }
            if (firebox.stored(EnergyType.HEAT) <= 0L) {
                helper.assertTrue(
                        firebox.addFuel(FuelDefinition.COAL_COKE),
                        "Could not refuel the adjacent HU host");
            }
            helper.assertTrue(
                    firebox.stored(EnergyType.HEAT) > 0L
                            || firebox.addFuel(FuelDefinition.COAL_COKE),
                    "machine stored no " + type);
            return;
        }
        Direction front = machine.getBlockState().getValue(ProcessingMachineBlock.FACING);
        Direction back = ProcessingMachineEnergyPlacement
                .connection(machine.spec(), front)
                .providerOffset();
        long packet = 32L;
        Direction side = back;
        if (machine.insert(type, packet, 4L, back, false) <= 0L) {
            for (Direction candidate : Direction.values()) {
                if (machine.insert(type, packet, 4L, candidate, false) > 0L) {
                    side = candidate;
                    break;
                }
            }
        }
        for (int i = 0; i < 8 && machine.stored(type) < machine.capacity(type); i++) {
            if (machine.insert(type, packet, 4L, side, false) <= 0L) {
                break;
            }
        }
        helper.assertTrue(machine.stored(type) > 0L, "machine stored no " + type);
    }

    static void loadRecipeInputs(
            ConfiguredProcessingMachineBlockEntity machine,
            GTRecipe recipe) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack[] options = recipe.itemInputs().get(i).getItems();
            if (options.length == 0) {
                continue;
            }
            ItemStack sample = options[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(
                    machine.spec().items().inputs().get(i), sample);
        }
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            machine.tanks().get(machine.spec().fluids().inputs().get(i).index())
                    .setFluid(recipe.fluidInputs().get(i).copy());
        }
    }

    static void assertRecipeMatches(GameTestHelper helper, RecipeMap map, GTRecipe recipe) {
        java.util.ArrayList<ItemStack> items = new java.util.ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemStack[] options = recipe.itemInputs().get(index).getItems();
            helper.assertTrue(options.length > 0, "representative item input is empty");
            ItemStack sample = options[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(index)));
            items.add(sample);
        }
        helper.assertTrue(
                map.findMatch(new GTRecipeQuery(items, recipe.fluidInputs())).isPresent(),
                "Loaded representative is not findable");
    }

    static boolean hasAnyOutput(ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream().anyMatch(slot ->
                !machine.inventory().getStackInSlot(slot).isEmpty())
                || machine.spec().fluids().outputs().stream().anyMatch(tank ->
                !machine.tanks().get(tank.index()).getFluid().isEmpty());
    }

    static void forceLastTick(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine) {
        helper.assertTrue(
                machine.duration() > 0
                        || machine.workProgressLong() > 0L
                        || hasAnyOutput(machine),
                machine.spec().id() + " has no selected recipe: "
                        + machine.pausedReason()
                        + " stored=" + machine.stored(machine.spec().energy().type())
                        + " work=" + machine.workProgressLong());
        if (machine.duration() > 0) {
            machine.runtime().processor().setProgress(machine.duration() - 1);
        }
    }

    static void executeRepresentative(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine,
            String label) {
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    fillEnergy(helper, machine);
                    forceLastTick(helper, machine);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(machine),
                        label + " produced no output: " + machine.pausedReason()
                                + " stored=" + machine.stored(machine.spec().energy().type())
                                + " work=" + machine.workProgressLong()))
                .thenSucceed();
    }
}
