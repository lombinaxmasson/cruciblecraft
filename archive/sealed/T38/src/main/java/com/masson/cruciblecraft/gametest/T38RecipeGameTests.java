package com.masson.cruciblecraft.gametest;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FireboxBlockEntity;
import com.masson.cruciblecraft.heat.FuelDefinition;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeProvenance;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated T38 compact-family runtime gate. Run with {@code -Pt38Recipes}.
 */
@GameTestHolder(T38RecipeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class T38RecipeGameTests {
    public static final String NAMESPACE = "cruciblecraft_t38";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final ResourceLocation T36_COAL_DUST_BOOTSTRAP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "t36/roaster/coal_dust_bootstrap");

    private T38RecipeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t38CompactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = t38StableIds();
        helper.assertTrue(
                published.size() == 73,
                "Roaster is missing T38 compact ids: " + published.size());
        RecipeMap.RecipeFamily family = ModRecipeMaps.ROASTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ROASTER.id()))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 73,
                "Roaster compact family is not the 73 T38 relations: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                !published.contains(T36_COAL_DUST_BOOTSTRAP),
                "T36 coal_dust_bootstrap leaked into the T38 compact id set");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t38RoasterExecutesRepresentative(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity roaster = placePowered(
                helper, new BlockPos(3, 2, 3));
        RecipeMap.Entry representative = ModRecipeMaps.ROASTER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t38/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.roaster#0000"::equals)
                        .isPresent())
                .filter(entry -> !entry.recipe().itemInputs().isEmpty()
                        && !entry.recipe().fluidInputs().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing T38 #0000-family roaster recipe with item+fluid inputs"));
        loadRecipeInputs(roaster, representative.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            roaster.workProgressLong() > 0L
                                    || roaster.duration() > 0
                                    || hasAnyOutput(roaster),
                            "T38 representative roaster recipe was not selected: "
                                    + roaster.pausedReason());
                    forceLastTick(helper, roaster);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(roaster),
                        "Roaster did not produce any output: "
                                + roaster.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t38SingletonFamilyExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity roaster = placePowered(
                helper, new BlockPos(3, 2, 3));
        RecipeMap.Entry singleton = ModRecipeMaps.ROASTER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t38/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.roaster#0004"::equals)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing T38 singleton roaster family gt.recipe.roaster#0004"));
        loadRecipeInputs(roaster, singleton.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            roaster.workProgressLong() > 0L
                                    || roaster.duration() > 0
                                    || hasAnyOutput(roaster),
                            "T38 singleton roaster recipe was not selected: "
                                    + roaster.pausedReason());
                    forceLastTick(helper, roaster);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(roaster),
                        "Singleton roaster family did not produce output: "
                                + roaster.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t38StableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = t38StableIds();
        helper.assertTrue(first.size() == 73, "T38 ids missing before re-enumeration");
        RecipeMap.RecipeFamily family = ModRecipeMaps.ROASTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ROASTER.id()))
                .orElseThrow();
        helper.assertTrue(
                family.epoch() == ModRecipeMaps.ROASTER.runtimeEpoch()
                        && family.logicalRecipeCount() == 73
                        && new TreeSet<>(family.recipeIds()).equals(first),
                "T38 compact family epoch/ids drifted on the live map");
        helper.assertTrue(
                t38StableIds().equals(first),
                "T38 stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t38EmiPlanIncludesRoasterFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = t38StableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> roasterEmi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.ROASTER.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                roasterEmi.containsAll(published),
                "EMI roaster category is missing T38 recipe ids: "
                        + published.stream()
                                .filter(id -> !roasterEmi.contains(id))
                                .toList());
        helper.succeed();
    }

    private static Set<ResourceLocation> t38StableIds() {
        return ModRecipeMaps.ROASTER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("t38/"))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static ConfiguredProcessingMachineBlockEntity placePowered(
            GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity roaster = place(
                helper,
                pos,
                ModBlocks.STEEL_ROASTER.get(),
                ModProcessingMachines.ROASTER);
        FireboxBlockEntity firebox = helper.getBlockEntity(pos.below());
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.COAL_COKE),
                "Could not fuel the adjacent HU roaster");
        helper.assertTrue(
                roaster.spec().energy().type() == EnergyType.HEAT
                        && roaster.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.ADJACENT,
                "Steel roaster is not an adjacent HU host");
        return roaster;
    }

    private static ConfiguredProcessingMachineBlockEntity place(
            GameTestHelper helper,
            BlockPos pos,
            net.minecraft.world.level.block.Block block,
            com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec spec) {
        helper.setBlock(
                pos,
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(
                machine.spec() == spec
                        || machine.variant().kind().behavior() == spec,
                "Placed block resolved wrong machine kind");
        return machine;
    }

    private static void loadRecipeInputs(
            ConfiguredProcessingMachineBlockEntity machine, GTRecipe recipe) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample = recipe.itemInputs().get(i).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(
                    machine.spec().items().inputs().get(i), sample);
        }
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            machine.tanks().get(machine.spec().fluids().inputs().get(i).index())
                    .setFluid(recipe.fluidInputs().get(i).copy());
        }
    }

    private static void forceLastTick(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine) {
        helper.assertTrue(
                machine.duration() > 0,
                machine.spec().id() + " has no selected recipe: "
                        + machine.pausedReason());
        machine.runtime().processor().setProgress(machine.duration() - 1);
    }

    private static boolean hasAnyOutput(
            ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream().anyMatch(slot ->
                !machine.inventory().getStackInSlot(slot).isEmpty())
                || machine.spec().fluids().outputs().stream().anyMatch(tank ->
                !machine.tanks().get(tank.index()).getFluid().isEmpty());
    }
}
