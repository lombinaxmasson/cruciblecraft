package com.masson.cruciblecraft.gametest;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.masson.cruciblecraft.gametest.GameTestHeatSources.energyCapacity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.gametest.GameTestHeatSources;
import com.masson.cruciblecraft.gametest.support.PublicationPolicyCounts;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
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
 * Isolated roaster/compact compact-family runtime gate. Run with {@code -PgameTestGrid=machines}.
 */
@GameTestHolder(RoasterCompactGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RoasterCompactGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final ResourceLocation COAL_DUST_BOOTSTRAP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "machine/bootstrap/roaster/coal_dust_bootstrap");

    private RoasterCompactGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = roasterCompactStableIds();
        helper.assertTrue(
                published.size() == roasterRows(helper),
                "Roaster is missing roaster/compact compact ids: " + published.size());
        RecipeMap.RecipeFamily family = ModRecipeMaps.ROASTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ROASTER.id()))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == roasterRows(helper),
                "Roaster compact family is not the 73 roaster/compact relations: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                !published.contains(COAL_DUST_BOOTSTRAP),
                "coal_dust_bootstrap leaked into the roaster/compact compact id set");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void roasterExecutesRepresentative(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity roaster = placePowered(
                helper, new BlockPos(3, 2, 3));
        RecipeMap.Entry representative = ModRecipeMaps.ROASTER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("roaster/compact/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.roaster#0000"::equals)
                        .isPresent())
                .filter(entry -> !entry.recipe().itemInputs().isEmpty()
                        && !entry.recipe().fluidInputs().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing roaster/compact #0000-family roaster recipe with item+fluid inputs"));
        loadRecipeInputs(roaster, representative.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            roaster.workProgressLong() > 0L
                                    || roaster.duration() > 0
                                    || hasAnyOutput(roaster),
                            "roaster/compact representative roaster recipe was not selected: "
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
    public static void singletonFamilyExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity roaster = placePowered(
                helper, new BlockPos(3, 2, 3));
        RecipeMap.Entry singleton = ModRecipeMaps.ROASTER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("roaster/compact/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.roaster#0004"::equals)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing roaster/compact singleton roaster family gt.recipe.roaster#0004"));
        loadRecipeInputs(roaster, singleton.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            roaster.workProgressLong() > 0L
                                    || roaster.duration() > 0
                                    || hasAnyOutput(roaster),
                            "roaster/compact singleton roaster recipe was not selected: "
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
    public static void roasterCompactStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = roasterCompactStableIds();
        helper.assertTrue(first.size() == roasterRows(helper), "roaster/compact ids missing before re-enumeration");
        RecipeMap.RecipeFamily family = ModRecipeMaps.ROASTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ROASTER.id()))
                .orElseThrow();
        helper.assertTrue(
                family.epoch() == ModRecipeMaps.ROASTER.runtimeEpoch()
                        && family.logicalRecipeCount() == roasterRows(helper)
                        && new TreeSet<>(family.recipeIds()).equals(first),
                "roaster/compact compact family epoch/ids drifted on the live map");
        helper.assertTrue(
                roasterCompactStableIds().equals(first),
                "roaster/compact stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emiPlanIncludesRoasterFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = roasterCompactStableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> roasterEmi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.ROASTER.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                roasterEmi.containsAll(published),
                "EMI roaster category is missing roaster/compact recipe ids: "
                        + published.stream()
                                .filter(id -> !roasterEmi.contains(id))
                                .toList());
        helper.succeed();
    }

    private static int roasterRows(GameTestHelper helper) {
        return PublicationPolicyCounts.relationCount(
                helper, CompactPublicationGroups.ROASTER_COMPACT);
    }

    private static Set<ResourceLocation> roasterCompactStableIds() {
        return ModRecipeMaps.ROASTER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("roaster/compact/"))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static ConfiguredProcessingMachineBlockEntity placePowered(
            GameTestHelper helper, BlockPos pos) {
        GameTestHeatSources.placeHuSource(helper, pos.below());
        ConfiguredProcessingMachineBlockEntity roaster = place(
                helper,
                pos,
                ModBlocks.STEEL_ROASTER.get(),
                ModProcessingMachines.ROASTER);
        FuelGeneratorBlockEntity firebox = helper.getBlockEntity(pos.below());
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
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
