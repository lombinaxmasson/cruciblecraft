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
import com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilyDefinition;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated smelter/stone compact-family runtime gate. Run with {@code -PgameTestGrid=machines}.
 */
@GameTestHolder(SmelterStoneGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SmelterStoneGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;

    private SmelterStoneGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = smelterStoneStableIds();
        int expected = PublicationPolicyCounts.relationCount(
                helper, CompactPublicationGroups.SMELTER_STONE);
        helper.assertTrue(
                published.size() == expected,
                "Smelter is missing smelter/stone compact ids: " + published.size());
        RecipeMap.RecipeFamily family = ModRecipeMaps.SMELTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SMELTER.id(),
                        CompactPublicationGroups.SMELTER_STONE))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == expected,
                "Smelter stone compact family logical rows != policy relation_count: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lockedSupportPublished(GameTestHelper helper) {
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("player_path_support/smelter_stone/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("player_path_support/smelter_stone/"))
                .count();
        helper.assertTrue(
                gtSupport + craftingSupport == 0,
                "Locked smelter/stone support must not be vanilla/GT recipes: "
                        + gtSupport
                        + " gt + "
                        + craftingSupport
                        + " crafting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void smelterExecutesRepresentative(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity smelter = placePowered(
                helper, new BlockPos(3, 2, 3));
        RecipeMap.Entry representative = ModRecipeMaps.SMELTER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("smelter/stone/"))
                .filter(entry -> !entry.recipe().itemInputs().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing smelter/stone smelter recipe with item inputs"));
        loadRecipeInputs(smelter, representative.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            smelter.workProgressLong() > 0L
                                    || smelter.duration() > 0
                                    || hasAnyOutput(smelter),
                            "smelter/stone representative smelter recipe was not selected: "
                                    + smelter.pausedReason());
                    forceLastTick(helper, smelter);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(smelter),
                        "Smelter did not produce any output: "
                                + smelter.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void noteblockFamilyExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity smelter = placePowered(
                helper, new BlockPos(3, 2, 3));
        RecipeMap.Entry noteblock = ModRecipeMaps.SMELTER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("smelter/stone/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.smelter#2932"::equals)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing locked smelter/stone cohort-B family gt.recipe.smelter#2932"));
        loadRecipeInputs(smelter, noteblock.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            smelter.workProgressLong() > 0L
                                    || smelter.duration() > 0
                                    || hasAnyOutput(smelter),
                            "smelter/stone noteblock smelter recipe was not selected: "
                                    + smelter.pausedReason());
                    forceLastTick(helper, smelter);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(smelter),
                        "Noteblock smelter family did not produce output: "
                                + smelter.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smelterStoneStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = smelterStoneStableIds();
        int expected = PublicationPolicyCounts.relationCount(
                helper, CompactPublicationGroups.SMELTER_STONE);
        helper.assertTrue(
                first.size() == expected,
                "smelter/stone locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily family = ModRecipeMaps.SMELTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SMELTER.id(),
                        CompactPublicationGroups.SMELTER_STONE))
                .orElseThrow();
        helper.assertTrue(
                family.epoch() == ModRecipeMaps.SMELTER.runtimeEpoch()
                        && family.logicalRecipeCount() == expected,
                "smelter/stone compact family epoch/count drifted on the live map");
        helper.assertTrue(
                new TreeSet<>(family.recipeIds()).equals(first),
                "smelter/stone compact family recipe ids do not cover the stable id set");
        helper.assertTrue(
                smelterStoneStableIds().equals(first),
                "smelter/stone stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emiPlanIncludesSmelterFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = smelterStoneStableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> smelterEmi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.SMELTER.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                smelterEmi.containsAll(published),
                "EMI smelter category is missing smelter/stone recipe ids: "
                        + published.stream()
                                .filter(id -> !smelterEmi.contains(id))
                                .toList());
        helper.succeed();
    }

    private static Set<ResourceLocation> smelterStoneStableIds() {
        return ModRecipeMaps.SMELTER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("smelter/stone/"))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static ConfiguredProcessingMachineBlockEntity placePowered(
            GameTestHelper helper, BlockPos pos) {
        GameTestHeatSources.placeHuSource(helper, pos.below());
        ConfiguredProcessingMachineBlockEntity smelter = place(
                helper,
                pos,
                ModBlocks.SMELTER.get(),
                ModProcessingMachines.SMELTER);
        FuelGeneratorBlockEntity firebox = helper.getBlockEntity(pos.below());
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel the adjacent HU smelter");
        helper.assertTrue(
                smelter.spec().energy().type() == EnergyType.HEAT
                        && smelter.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.ADJACENT,
                "Smelter is not an adjacent HU host");
        return smelter;
    }

    private static ConfiguredProcessingMachineBlockEntity place(
            GameTestHelper helper,
            BlockPos pos,
            net.minecraft.world.level.block.Block block,
            ProcessingMachineSpec spec) {
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
