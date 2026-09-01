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
import com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilyDefinition;
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
 * Isolated T43 compact-family runtime gate. Run with {@code -Pt43Recipes}.
 */
@GameTestHolder(T43RecipeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class T43RecipeGameTests {
    public static final String NAMESPACE = "cruciblecraft_t43";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;

    private T43RecipeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t43CompactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = t43StableIds();
        helper.assertTrue(
                published.size() == 407,
                "Smelter is missing T43 compact ids: " + published.size());
        RecipeMap.RecipeFamily family = ModRecipeMaps.SMELTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SMELTER.id(),
                        CompactGTRecipeFamilyDefinition
                                .T43_SMELTER_STONE_PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 407,
                "Smelter stone compact family is not the 407 locked relations: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t43LockedSupportPublished(GameTestHelper helper) {
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("t43_player_path_support/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("t43_player_path_support/"))
                .count();
        helper.assertTrue(
                gtSupport + craftingSupport == 0,
                "Locked T43 support must not be vanilla/GT recipes: "
                        + gtSupport
                        + " gt + "
                        + craftingSupport
                        + " crafting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t43SmelterExecutesRepresentative(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity smelter = placePowered(
                helper, new BlockPos(3, 2, 3));
        RecipeMap.Entry representative = ModRecipeMaps.SMELTER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t43/"))
                .filter(entry -> !entry.recipe().itemInputs().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing T43 smelter recipe with item inputs"));
        loadRecipeInputs(smelter, representative.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            smelter.workProgressLong() > 0L
                                    || smelter.duration() > 0
                                    || hasAnyOutput(smelter),
                            "T43 representative smelter recipe was not selected: "
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
    public static void t43NoteblockFamilyExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity smelter = placePowered(
                helper, new BlockPos(3, 2, 3));
        RecipeMap.Entry noteblock = ModRecipeMaps.SMELTER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t43/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.smelter#2932"::equals)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing locked T43 cohort-B family gt.recipe.smelter#2932"));
        loadRecipeInputs(smelter, noteblock.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            smelter.workProgressLong() > 0L
                                    || smelter.duration() > 0
                                    || hasAnyOutput(smelter),
                            "T43 noteblock smelter recipe was not selected: "
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
    public static void t43StableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = t43StableIds();
        helper.assertTrue(first.size() == 407, "T43 locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily family = ModRecipeMaps.SMELTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SMELTER.id(),
                        CompactGTRecipeFamilyDefinition
                                .T43_SMELTER_STONE_PUBLICATION_GROUP))
                .orElseThrow();
        helper.assertTrue(
                family.epoch() == ModRecipeMaps.SMELTER.runtimeEpoch()
                        && family.logicalRecipeCount() == 407,
                "T43 compact family epoch/count drifted on the live map");
        helper.assertTrue(
                new TreeSet<>(family.recipeIds()).equals(first),
                "T43 compact family recipe ids do not cover the stable id set");
        helper.assertTrue(
                t43StableIds().equals(first),
                "T43 stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t43EmiPlanIncludesSmelterFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = t43StableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> smelterEmi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.SMELTER.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                smelterEmi.containsAll(published),
                "EMI smelter category is missing T43 recipe ids: "
                        + published.stream()
                                .filter(id -> !smelterEmi.contains(id))
                                .toList());
        helper.succeed();
    }

    private static Set<ResourceLocation> t43StableIds() {
        return ModRecipeMaps.SMELTER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("t43/"))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static ConfiguredProcessingMachineBlockEntity placePowered(
            GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity smelter = place(
                helper,
                pos,
                ModBlocks.SMELTER.get(),
                ModProcessingMachines.SMELTER);
        FireboxBlockEntity firebox = helper.getBlockEntity(pos.below());
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.COAL_COKE),
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
