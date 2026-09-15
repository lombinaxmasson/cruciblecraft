package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.masson.cruciblecraft.gametest.GameTestHeatSources.energyCapacity;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.gametest.GameTestHeatSources;
import com.masson.cruciblecraft.content.item.SemanticObjectCatalog;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader;
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
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated Smelter ordinary-closure runtime gate. Run with
 * {@code -PwaveRecipes=smelter/ordinary-closure}.
 */
@GameTestHolder(SmelterOrdinaryClosureGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SmelterOrdinaryClosureGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_smelter_ordinary_closure";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final String RECIPE_PREFIX = "smelter/ordinary_closure/";
    private static final List<ResourceLocation> GROUPS = List.of(
            CompactPublicationGroups.SMELTER_ORDINARY_SINGLETON,
            CompactPublicationGroups.SMELTER_ORDINARY_MULTIITEM,
            CompactPublicationGroups.SMELTER_ORDINARY_TOOL_HEAD,
            CompactPublicationGroups.SMELTER_ORDINARY_MATERIAL_FORM,
            CompactPublicationGroups.SMELTER_ORDINARY_GT_PREFIX,
            CompactPublicationGroups.SMELTER_ORDINARY_UNMAPPED,
            CompactPublicationGroups.SMELTER_ORDINARY_ACQUISITION);

    private SmelterOrdinaryClosureGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smelterReleasePerformanceGates(GameTestHelper helper) {
        var metrics = GTRecipeMapLoader.lastPublicationMetrics();
        var lookup = GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        var capacity = GTRecipeMapLoader.lastCapacityReport();
        helper.assertTrue(
                metrics.eagerPublishedRecipes()
                        >= ModProcessingMachines.VERIFIED_OPENING_EAGER_PUBLISHED,
                "Smelter ordinary-closure eager drifted below opening 16980: "
                        + metrics.eagerPublishedRecipes());
        helper.assertTrue(
                capacity.concreteEager() + metrics.compactLoadExtruderEagerRecipes()
                        >= ModProcessingMachines.VERIFIED_OPENING_CONCRETE_EAGER,
                "Opening concrete+extruder eager drifted below 16966: "
                        + capacity.concreteEager()
                        + "+"
                        + metrics.compactLoadExtruderEagerRecipes());
        CrucibleCraft.LOGGER.info(
                "smelter/ordinary-closure measured capacity={} metrics={} lookup={}",
                capacity,
                metrics,
                lookup);
        GTRecipeMapLoader.verifyReleasePerformance(
                metrics, lookup, null, null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smelterOrdinaryFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = publishedIds();
        helper.assertTrue(
                !published.isEmpty(),
                "Smelter ordinary-closure compact ids missing");
        int groupsWithRecipes = 0;
        for (ResourceLocation group : GROUPS) {
            RecipeMap.RecipeFamily family = ModRecipeMaps.SMELTER
                    .family(CompactRecipeFamilyProvider.familyId(
                            ModRecipeMaps.SMELTER.id(), group))
                    .orElse(null);
            if (family != null && family.logicalRecipeCount() > 0) {
                groupsWithRecipes++;
            }
        }
        helper.assertTrue(
                groupsWithRecipes > 0,
                "No Smelter ordinary-closure publication group published");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void smelterOrdinaryGroupsExecute(GameTestHelper helper) {
        java.util.ArrayList<ConfiguredProcessingMachineBlockEntity> machines =
                new java.util.ArrayList<>();
        java.util.ArrayList<ResourceLocation> executed = new java.util.ArrayList<>();
        int column = 0;
        for (ResourceLocation group : GROUPS) {
            RecipeMap.RecipeFamily family = ModRecipeMaps.SMELTER
                    .family(CompactRecipeFamilyProvider.familyId(
                            ModRecipeMaps.SMELTER.id(), group))
                    .orElse(null);
            if (family == null || family.logicalRecipeCount() <= 0) {
                continue;
            }
            RecipeMap.Entry representative = firstFitting(family);
            ConfiguredProcessingMachineBlockEntity smelter = placePowered(
                    helper,
                    new BlockPos(2 + column * 2, 2, 3),
                    ModBlocks.SMELTER.get(),
                    ModProcessingMachines.SMELTER);
            loadRecipeInputs(smelter, representative.recipe());
            machines.add(smelter);
            executed.add(group);
            column++;
        }
        helper.assertTrue(!machines.isEmpty(), "No Smelter ordinary-closure group to execute");
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    for (int index = 0; index < machines.size(); index++) {
                        ConfiguredProcessingMachineBlockEntity smelter =
                                machines.get(index);
                        helper.assertTrue(
                                smelter.workProgressLong() > 0L
                                        || smelter.duration() > 0
                                        || hasAnyOutput(smelter),
                                "Smelter group " + executed.get(index)
                                        + " was not selected: "
                                        + smelter.pausedReason());
                        forceLastTick(helper, smelter);
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    for (int index = 0; index < machines.size(); index++) {
                        helper.assertTrue(
                                hasAnyOutput(machines.get(index)),
                                "Smelter group " + executed.get(index)
                                        + " produced no output: "
                                        + machines.get(index).pausedReason());
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void smelterHeatAdjacencyAndBuffering(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity smelter = placePowered(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.SMELTER.get(),
                ModProcessingMachines.SMELTER);
        helper.assertTrue(
                smelter.spec().energy().type() == EnergyType.HEAT
                        && smelter.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.ADJACENT,
                "Smelter is not adjacent HU");
        helper.assertTrue(
                smelter.spec().buffering() == ProcessingMachineSpec.BufferPolicy.PAUSE,
                "Smelter buffering drifted from PAUSE");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void smelterDurationEutAndConservation(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity smelter = placePowered(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.SMELTER.get(),
                ModProcessingMachines.SMELTER);
        RecipeMap.Entry representative = firstOrdinaryFitting();
        GTRecipe recipe = representative.recipe();
        helper.assertTrue(recipe.duration() > 0, "Ordinary Smelter duration is 0");
        helper.assertTrue(recipe.eut() > 0L, "Ordinary Smelter EUt is 0");
        loadRecipeInputs(smelter, recipe);
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            smelter.duration() > 0
                                    || smelter.workProgressLong() > 0L,
                            "Ordinary Smelter recipe was not selected: "
                                    + smelter.pausedReason());
                    forceLastTick(helper, smelter);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            hasAnyOutput(smelter),
                            "Conservation run produced no output");
                    if (!recipe.itemOutputs().isEmpty()) {
                        ItemStack expected = recipe.itemOutputs().getFirst();
                        boolean matched = smelter.spec().items().outputs().stream()
                                .anyMatch(slot -> {
                                    ItemStack produced = smelter.inventory()
                                            .getStackInSlot(slot);
                                    return produced.is(expected.getItem())
                                            && produced.getCount() == expected.getCount();
                                });
                        helper.assertTrue(
                                matched,
                                "Smelter output count drifted from authored recipe");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smelterStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = publishedIds();
        helper.assertTrue(!first.isEmpty(), "Ordinary ids missing before re-enumeration");
        helper.assertTrue(
                publishedIds().equals(first),
                "Ordinary Smelter stable ids drifted on a second entries() pass");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void semanticCatalogIdentitiesAreLoaded(GameTestHelper helper) {
        helper.assertTrue(
                SemanticObjectCatalog.identities() != null,
                "Semantic object catalog failed to load");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smelterEmiPlanIncludesFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = publishedIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> emi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.SMELTER.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                emi.containsAll(published),
                "EMI categories are missing ordinary Smelter recipe ids");
        helper.succeed();
    }

    private static Set<ResourceLocation> publishedIds() {
        Set<ResourceLocation> ids = new TreeSet<>();
        ModRecipeMaps.SMELTER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith(RECIPE_PREFIX))
                .forEach(ids::add);
        return ids;
    }

    private static RecipeMap.Entry firstOrdinaryFitting() {
        for (ResourceLocation group : GROUPS) {
            RecipeMap.RecipeFamily family = ModRecipeMaps.SMELTER
                    .family(CompactRecipeFamilyProvider.familyId(
                            ModRecipeMaps.SMELTER.id(), group))
                    .orElse(null);
            if (family != null && family.logicalRecipeCount() > 0) {
                RecipeMap.Entry fitting = firstFittingOrNull(family);
                if (fitting != null) {
                    return fitting;
                }
            }
        }
        throw new IllegalStateException("Missing Smelter ordinary-closure recipe");
    }

    private static RecipeMap.Entry firstFitting(RecipeMap.RecipeFamily family) {
        RecipeMap.Entry fitting = firstFittingOrNull(family);
        return fitting != null ? fitting : family.enumerationEntry(0);
    }

    private static RecipeMap.Entry firstFittingOrNull(RecipeMap.RecipeFamily family) {
        for (int index = 0; index < family.logicalRecipeCount(); index++) {
            RecipeMap.Entry entry = family.enumerationEntry(index);
            GTRecipe recipe = entry.recipe();
            if (recipe.itemInputs().size() <= 1
                    && recipe.itemOutputs().size() <= 4
                    && recipe.fluidInputs().isEmpty()
                    && recipe.fluidOutputs().size() <= 1) {
                return entry;
            }
        }
        return null;
    }

    private static ConfiguredProcessingMachineBlockEntity placePowered(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
            ProcessingMachineSpec spec) {
        GameTestHeatSources.placeHuSource(helper, pos.below());
        ConfiguredProcessingMachineBlockEntity machine = place(helper, pos, block, spec);
        FuelGeneratorBlockEntity firebox = helper.getBlockEntity(pos.below());
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel the adjacent HU host");
        return machine;
    }

    private static ConfiguredProcessingMachineBlockEntity place(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
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
