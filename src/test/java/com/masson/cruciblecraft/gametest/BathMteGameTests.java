package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.gametest.support.PublicationPolicyCounts;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated bath/mte compact-family runtime gate. Run with {@code -PgameTestGrid=machines}.
 * Relation-level equivalence lives in JUnit; these tests are representative.
 */
@GameTestHolder(BathMteGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class BathMteGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final int FLUID_SUPPORT_RECIPES = 37;
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "bath/mte");

    private BathMteGameTests() {}

    private static int lockedRelations(GameTestHelper helper) {
        return PublicationPolicyCounts.relationCount(helper, PUBLICATION_GROUP);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = bathMteStableIds();
        helper.assertTrue(
                published.size() == lockedRelations(helper),
                "bath/mte compact ids missing: " + published.size());
        RecipeMap.RecipeFamily family = ModRecipeMaps.BATH
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.BATH.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == lockedRelations(helper),
                "Bath MTE compact family is not the 1517 locked relations: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                family instanceof CompactRecipeFamilyProvider.Snapshot snapshot
                        && snapshot.shardCount() == lockedRelations(helper)
                        && snapshot.overflowRelationCount() == 0,
                "bath/mte live shard count drifted from the 1517 pair manifest");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lockedSupportPublished(GameTestHelper helper) {
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("player_path_support/bath_mte/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("player_path_support/bath_mte/"))
                .count();
        helper.assertTrue(
                craftingSupport == 0,
                "bath/mte B1 must not publish crafting recipes: "
                        + craftingSupport);
        helper.assertTrue(
                gtSupport == FLUID_SUPPORT_RECIPES,
                "bath/mte fluid support recipes drifted: " + gtSupport);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void oilSupportExecutes(GameTestHelper helper) {
        executeSupportFluid(
                helper,
                "player_path_support/bath_mte/cruciblecraft_sunflower_oil");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void dyeSupportExecutes(GameTestHelper helper) {
        executeSupportFluid(
                helper,
                "player_path_support/bath_mte/cruciblecraft_dye_flower_red");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void exactRelationExecutes(GameTestHelper helper) {
        executeAcquiredRepresentative(helper, true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void exactMultiRelationExecutes(GameTestHelper helper) {
        executeAcquiredRepresentative(helper, false);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wrongIdentityDoesNotStart(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity bath = placeBath(helper);
        helper.assertTrue(
                bath.spec().energy().type() == EnergyType.TIME
                        && bath.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED,
                "Bath is not a buffered TIME host");
        GTRecipe recipe = firstBathMteRecipe(candidate -> !candidate.fluidInputs().isEmpty());
        ItemStack sample = recipe.itemInputs().getFirst().getItems()[0].copy();
        sample.setCount(Math.max(1, recipe.itemInputCounts().getFirst()));
        bath.inventory().setStackInSlot(bath.spec().items().inputs().getFirst(), sample);
        bath.tanks().get(bath.spec().fluids().inputs().getFirst().index())
                .setFluid(new FluidStack(Fluids.WATER, recipe.fluidInputs().getFirst().getAmount()));
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(
                        bath.duration() == 0 && bath.workProgressLong() == 0L,
                        "Wrong fluid started a bath/mte Bath recipe: "
                                + bath.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wrongItemDoesNotStart(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity bath = placeBath(helper);
        GTRecipe recipe = firstBathMteRecipe(candidate -> !candidate.fluidInputs().isEmpty());
        bath.inventory().setStackInSlot(
                bath.spec().items().inputs().getFirst(),
                new ItemStack(Items.DIRT));
        bath.tanks().get(bath.spec().fluids().inputs().getFirst().index())
                .setFluid(recipe.fluidInputs().getFirst().copy());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(
                        bath.duration() == 0 && bath.workProgressLong() == 0L,
                        "Wrong MTE started a bath/mte Bath recipe: "
                                + bath.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bathMteStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = bathMteStableIds();
        helper.assertTrue(
                first.size() == lockedRelations(helper),
                "bath/mte locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily family = ModRecipeMaps.BATH
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.BATH.id(), PUBLICATION_GROUP))
                .orElseThrow();
        helper.assertTrue(
                family.epoch() == ModRecipeMaps.BATH.runtimeEpoch()
                        && family.logicalRecipeCount() == lockedRelations(helper),
                "bath/mte Bath compact family epoch/count drifted on the live map");
        helper.assertTrue(
                family instanceof CompactRecipeFamilyProvider.Snapshot snapshot
                        && snapshot.shardCount() == lockedRelations(helper)
                        && snapshot.overflowRelationCount() == 0,
                "bath/mte shard count drifted after re-enumeration");
        helper.assertTrue(
                bathMteStableIds().equals(first),
                "bath/mte stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emiPlanIncludesFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = bathMteStableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> emi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.BATH.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                emi.containsAll(published),
                "EMI categories are missing bath/mte recipe ids: "
                        + published.stream()
                                .filter(id -> !emi.contains(id))
                                .toList());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void authoredAndChemicalBathRecipesRemain(GameTestHelper helper) {
        helper.assertTrue(
                ModRecipeMaps.BATH.entries().stream().anyMatch(entry ->
                        entry.id().getPath().startsWith("bath/crushed_to_washed")),
                "authored bath/crushed_to_washed is missing after bath/mte publication");
        helper.assertTrue(
                ModRecipeMaps.BATH.entries().stream().anyMatch(entry ->
                        entry.id().getPath().startsWith("chemical/bath/")),
                "chemical bath recipes are missing after bath/mte publication");
        helper.succeed();
    }

    private static void executeSupportFluid(GameTestHelper helper, String path) {
        GTRecipe recipe = ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> entry.id().getPath().equals(path))
                .map(RecipeMap.Entry::recipe)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing bath/mte support recipe " + path));
        helper.assertTrue(
                !recipe.fluidOutputs().isEmpty(),
                "Support recipe has no fluid output: " + path);
        executeRepresentative(helper, recipe);
    }

    private static void executeAcquiredRepresentative(
            GameTestHelper helper, boolean firstExact) {
        List<GTRecipe> matches = ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("bath/mte/"))
                .map(RecipeMap.Entry::recipe)
                .filter(candidate -> !candidate.itemInputs().isEmpty()
                        && !candidate.fluidInputs().isEmpty())
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalStateException(
                    "Missing bath/mte Bath recipe with item and fluid inputs");
        }
        GTRecipe recipe = matches.getFirst();
        if (!firstExact && matches.size() > 1) {
            recipe = matches.get(1);
        }
        executeRepresentative(helper, recipe);
    }

    private static void executeRepresentative(GameTestHelper helper, GTRecipe recipe) {
        ConfiguredProcessingMachineBlockEntity bath = placeBath(helper);
        helper.assertTrue(
                bath.spec().energy().type() == EnergyType.TIME
                        && bath.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED,
                "Bath is not a buffered TIME host");
        loadRecipeInputs(bath, recipe);
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            bath.workProgressLong() > 0L
                                    || bath.duration() > 0
                                    || hasAnyOutput(bath),
                            "bath/mte representative Bath recipe was not selected: "
                                    + bath.pausedReason());
                    forceLastTick(helper, bath);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(bath),
                        "Bath did not produce any output: "
                                + bath.pausedReason()))
                .thenSucceed();
    }

    private static GTRecipe firstBathMteRecipe(
            java.util.function.Predicate<GTRecipe> filter) {
        return ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("bath/mte/"))
                .map(RecipeMap.Entry::recipe)
                .filter(filter)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing matching bath/mte Bath recipe"));
    }

    private static Set<ResourceLocation> bathMteStableIds() {
        Set<ResourceLocation> ids = new TreeSet<>();
        ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("bath/mte/"))
                .forEach(ids::add);
        return ids;
    }

    private static ConfiguredProcessingMachineBlockEntity placeBath(GameTestHelper helper) {
        return place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.BATH.get(),
                ModProcessingMachines.BATH);
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
            ConfiguredProcessingMachineBlockEntity machine,
            GTRecipe recipe) {
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
